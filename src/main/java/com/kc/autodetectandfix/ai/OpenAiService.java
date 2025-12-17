package com.kc.autodetectandfix.ai;

import com.kc.autodetectandfix.ai.dto.OpenAiRequest;
import com.kc.autodetectandfix.ai.dto.OpenAiResponse;
import com.kc.autodetectandfix.config.OpenAiConfig;
import com.kc.autodetectandfix.model.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.retry.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
public class OpenAiService {

    private static final Logger logger = LoggerFactory.getLogger(OpenAiService.class);

    private final WebClient webClient;
    private final OpenAiConfig config;
    private final Retry retry;
    private final ObjectMapper objectMapper;

    public OpenAiService(WebClient openAiWebClient, OpenAiConfig config, Retry openAiRetry) {
        this.webClient = openAiWebClient;
        this.config = config;
        this.retry = openAiRetry;
        this.objectMapper = new ObjectMapper();
    }

    public ErrorCategory classifyError(DetectedError error) {
        if (config.isMockMode()) {
            return mockClassify(error);
        }

        String systemPrompt = "You are an expert error classifier for Java Spring Boot applications. " +
            "Analyze the complete error context including exception, stack trace, and source code to determine the root cause.\n\n" +
            "Categories:\n" +
            "- CONFIG: Configuration/routing errors (missing properties, wrong config, missing API endpoints, NoResourceFoundException)\n" +
            "- DATA: Data validation or parsing errors (invalid input, format issues, validation failures)\n" +
            "- INFRA: Infrastructure errors (network failures, database connection issues, I/O failures, timeouts)\n" +
            "- CODE: Programming logic errors (NPE, null checks, array bounds, arithmetic, type casting)\n" +
            "- UNKNOWN: Cannot determine category\n\n" +
            "Analyze the actual source code provided to understand the context. " +
            "Respond with exactly ONE word: CONFIG, DATA, INFRA, CODE, or UNKNOWN.";

        StringBuilder userPrompt = new StringBuilder();
        userPrompt.append("=== EXCEPTION DETAILS ===\n");
        userPrompt.append("Type: ").append(error.getExceptionType()).append("\n");
        userPrompt.append("Message: ").append(error.getMessage()).append("\n\n");

        // Include the FULL raw exception text which contains "Caused by" chain
        if (error.getRawLogEntry() != null && !error.getRawLogEntry().isEmpty()) {
            userPrompt.append("=== FULL EXCEPTION (includes Caused by chain) ===\n");
            userPrompt.append(error.getRawLogEntry()).append("\n\n");
        }

        userPrompt.append("=== STACK TRACE ===\n");
        userPrompt.append(getDetailedStackTrace(error, 10));

        userPrompt.append("\n=== ANALYSIS ===\n");
        userPrompt.append("IMPORTANT: If the FULL EXCEPTION contains 'Caused by:', classify based on the ROOT CAUSE exception, not the wrapper.\n");
        userPrompt.append("For example, if you see 'RuntimeException... Caused by: IllegalArgumentException: Invalid balance format', classify based on the IllegalArgumentException (DATA error), not the RuntimeException.\n\n");
        userPrompt.append("Based on the exception type, message, and stack trace, classify this error.\n");

        try {
            // Log the classification prompt being sent to OpenAI
            logger.info("=== SENDING CLASSIFICATION PROMPT TO OPENAI ===");
            logger.info("System Prompt: {}", systemPrompt);
            logger.info("User Prompt:\n{}", userPrompt.toString());
            logger.info("=== END CLASSIFICATION PROMPT ===");

            OpenAiResponse response = callOpenAi(systemPrompt, userPrompt.toString());
            String category = extractContent(response).trim().toUpperCase();

            // Log the classification response from OpenAI
            logger.info("=== OPENAI CLASSIFICATION RESPONSE ===");
            logger.info("Category: {}", category);
            logger.info("=== END CLASSIFICATION RESPONSE ===");

            try {
                return ErrorCategory.valueOf(category);
            } catch (IllegalArgumentException e) {
                logger.warn("Invalid category from OpenAI: {}", category);
                return ErrorCategory.UNKNOWN;
            }
        } catch (Exception e) {
            logger.error("OpenAI classification failed", e);
            throw new RuntimeException("OpenAI classification failed", e);
        }
    }

    public FixSuggestion generateFixSuggestion(DetectedError error, SourceCodeContext context) {
        if (config.isMockMode()) {
            return mockFixSuggestion(error);
        }

        String systemPrompt = "You are a senior Java developer expert at debugging and fixing code issues. " +
                "Analyze the actual source code, error, and stack trace to provide SPECIFIC code changes.\n\n" +
                "Respond ONLY with raw JSON (no markdown code blocks, no ```json wrapper).\n" +
                "Use this EXACT format:\n" +
                "{\"summary\": \"brief description of what needs to be fixed\", " +
                "\"steps\": [" +
                "\"Step 1: Specific code change with actual code snippet\", " +
                "\"Step 2: Another specific change\", " +
                "\"Step 3: Additional changes or verification steps\"" +
                "], " +
                "\"confidence\": \"HIGH or MEDIUM or LOW\", " +
                "\"originalCode\": \"The exact problematic code snippet that causes the error (multi-line if needed)\", " +
                "\"fixedCode\": \"The corrected code snippet that fixes the issue (multi-line if needed)\"}\n\n" +
                "CRITICAL FIX GUIDELINES - Follow these defensive programming practices:\n" +
                "- For NullPointerException: Add null checks BEFORE accessing methods/fields (if (obj != null) { ... })\n" +
                "- For ArrayIndexOutOfBoundsException: Add bounds checking BEFORE array access (if (index >= 0 && index < arr.length) { ... })\n" +
                "- For ArithmeticException (division by zero): Add validation BEFORE division (if (divisor != 0) { ... })\n" +
                "- DO NOT just change values to avoid the error - add proper defensive validation\n" +
                "- DO NOT just pick a valid index - add bounds checking that works for ANY index\n\n" +
                "FORMATTING REQUIREMENTS:\n" +
                "- Return ONLY raw JSON, do NOT wrap in markdown code blocks\n" +
                "- Provide ACTUAL code changes, not generic advice\n" +
                "- Reference specific line numbers and file names in steps\n" +
                "- originalCode should contain the EXACT problematic code from the source WITHOUT line numbers or markers\n" +
                "- fixedCode should contain the EXACT corrected code with the fix applied WITHOUT line numbers or markers\n" +
                "- Include only the relevant code lines (not the entire file)\n" +
                "- Include imports if needed in fixedCode\n" +
                "- Be specific about WHERE in the code to make changes\n" +
                "- DO NOT include line numbers (like '48:') or markers (like '>>>') in originalCode or fixedCode";

        StringBuilder userPrompt = new StringBuilder();
        userPrompt.append("=== EXCEPTION DETAILS ===\n");
        userPrompt.append("Type: ").append(error.getExceptionType()).append("\n");
        userPrompt.append("Category: ").append(error.getCategory()).append("\n");
        userPrompt.append("Message: ").append(error.getMessage()).append("\n\n");

        // Include the FULL raw exception text which contains "Caused by" chain
        if (error.getRawLogEntry() != null && !error.getRawLogEntry().isEmpty()) {
            userPrompt.append("=== FULL EXCEPTION (includes Caused by chain) ===\n");
            userPrompt.append(error.getRawLogEntry()).append("\n\n");
        }

        userPrompt.append("=== STACK TRACE (Application Code Highlighted) ===\n");
        userPrompt.append(getDetailedStackTrace(error, 15));

        // Add source code context if available
        if (context != null && context.getSurroundingLines() != null && !context.getSurroundingLines().isEmpty()) {
            userPrompt.append("\n\n=== SOURCE CODE ===\n");
            userPrompt.append("File: ").append(context.getFilePath()).append("\n");
            userPrompt.append("Error Location: Line ").append(context.getErrorLineNumber()).append("\n");

            if (context.getLastModifiedBy() != null) {
                userPrompt.append("Last Modified By: ").append(context.getLastModifiedBy()).append("\n");
            }
            if (context.getCommitHash() != null) {
                userPrompt.append("Commit: ").append(context.getCommitHash()).append("\n");
            }

            userPrompt.append("\nCode Context:\n");
            // Note: surroundingLines already contain line numbers and >>> markers from GitRepositoryService
            for (String line : context.getSurroundingLines()) {
                userPrompt.append(line).append("\n");
            }
        } else {
            userPrompt.append("\n\n=== SOURCE CODE ===\n");
            userPrompt.append("Source code not available. Provide fix based on exception type and stack trace.\n");
        }

        userPrompt.append("\n\n=== INSTRUCTIONS ===\n");
        userPrompt.append("CRITICAL: Look at the 'FULL EXCEPTION' section above - if it contains 'Caused by:' exceptions, focus on the ROOT CAUSE (the deepest 'Caused by'), NOT the top-level wrapper exception.\n");
        userPrompt.append("The real problem is in the 'Caused by' chain. For example, if you see:\n");
        userPrompt.append("  RuntimeException: Failed to fetch accounts\n");
        userPrompt.append("    Caused by: IllegalArgumentException: Invalid balance format for account ACC-2025-001: expected numeric value but got text '5000.50'\n");
        userPrompt.append("Then focus on the IllegalArgumentException about balance format, NOT the RuntimeException wrapper.\n\n");
        userPrompt.append("Analyze the above error and source code. Provide:\n");
        userPrompt.append("1. A brief summary of the ROOT CAUSE issue (from the deepest 'Caused by' exception)\n");
        userPrompt.append("2. Specific code changes to fix the ROOT CAUSE (show actual code)\n");
        userPrompt.append("3. Any additional steps or verification needed\n");
        userPrompt.append("\nBe as specific as possible with line numbers, code snippets, and exact changes.");
        userPrompt.append("\nIf you see 'Invalid balance format' or similar data format errors, suggest how to handle the data type mismatch (e.g., parse String to BigDecimal).");

        try {
            // Log the prompt being sent to OpenAI
            logger.info("=== SENDING PROMPT TO OPENAI ===");
            logger.info("System Prompt: {}", systemPrompt);
            logger.info("User Prompt:\n{}", userPrompt.toString());
            logger.info("=== END PROMPT ===");

            OpenAiResponse response = callOpenAi(systemPrompt, userPrompt.toString());
            String jsonContent = extractContent(response);

            // Log the response from OpenAI
            logger.info("=== OPENAI RESPONSE ===");
            logger.info("Response: {}", jsonContent);
            logger.info("=== END RESPONSE ===");

            return parseFixSuggestion(jsonContent, context);
        } catch (Exception e) {
            logger.error("OpenAI fix generation failed", e);
            throw new RuntimeException("OpenAI fix generation failed", e);
        }
    }

    private OpenAiResponse callOpenAi(String systemPrompt, String userPrompt) {
        OpenAiRequest request = new OpenAiRequest(
            config.getModel(),
            Arrays.asList(
                OpenAiRequest.Message.system(systemPrompt),
                OpenAiRequest.Message.user(userPrompt)
            )
        );

        return Retry.decorateSupplier(retry, () ->
            webClient.post()
                .bodyValue(request)
                .retrieve()
                .bodyToMono(OpenAiResponse.class)
                .timeout(Duration.ofSeconds(config.getTimeoutSeconds()))
                .block()
        ).get();
    }

    private String extractContent(OpenAiResponse response) {
        if (response.getChoices() != null && !response.getChoices().isEmpty()) {
            return response.getChoices().get(0).getMessage().getContent();
        }
        throw new RuntimeException("No content in OpenAI response");
    }

    private FixSuggestion parseFixSuggestion(String jsonContent, SourceCodeContext context) {
        try {
            // Clean markdown code blocks if present
            String cleanedJson = cleanJsonResponse(jsonContent);

            FixSuggestionDto dto = objectMapper.readValue(cleanedJson, FixSuggestionDto.class);

            FixSuggestion suggestion = new FixSuggestion();
            suggestion.setSummary(dto.summary);
            suggestion.setSteps(dto.steps != null ? dto.steps : new ArrayList<>());
            suggestion.setConfidence(dto.confidence != null ? dto.confidence : "MEDIUM");
            suggestion.setSourceContext(context);
            // Strip line numbers and markers from code snippets
            suggestion.setOriginalCode(stripLineNumbers(dto.originalCode));
            suggestion.setFixedCode(stripLineNumbers(dto.fixedCode));

            return suggestion;
        } catch (Exception e) {
            logger.error("Failed to parse fix suggestion JSON: {}", jsonContent, e);

            FixSuggestion fallback = new FixSuggestion();
            fallback.setSummary("OpenAI provided suggestion (parse failed)");
            fallback.setSteps(Arrays.asList(jsonContent));
            fallback.setConfidence("LOW");
            fallback.setSourceContext(context);
            return fallback;
        }
    }

    /**
     * Strips line numbers and markers from code snippets.
     * Removes patterns like "  48: code", ">>>  48: code", etc.
     */
    private String stripLineNumbers(String code) {
        if (code == null || code.isEmpty()) {
            return code;
        }

        StringBuilder result = new StringBuilder();
        String[] lines = code.split("\n");

        for (String line : lines) {
            // Remove patterns: ">>>   48: ", "    48: ", "  48: ", etc.
            // Pattern: optional >>> + optional spaces + digits + colon + space
            String cleaned = line.replaceFirst("^(>>>)?\\s*\\d+:\\s*", "");
            result.append(cleaned).append("\n");
        }

        // Remove trailing newline
        String stripped = result.toString();
        if (stripped.endsWith("\n")) {
            stripped = stripped.substring(0, stripped.length() - 1);
        }

        return stripped;
    }

    /**
     * Cleans JSON response by removing markdown code blocks and extra whitespace.
     */
    private String cleanJsonResponse(String response) {
        if (response == null) {
            return "";
        }

        String cleaned = response.trim();

        // Remove markdown code blocks: ```json ... ``` or ``` ... ```
        if (cleaned.startsWith("```")) {
            // Find the first newline after the opening ```
            int firstNewline = cleaned.indexOf('\n');
            if (firstNewline > 0) {
                cleaned = cleaned.substring(firstNewline + 1);
            }

            // Remove the closing ```
            if (cleaned.endsWith("```")) {
                cleaned = cleaned.substring(0, cleaned.lastIndexOf("```"));
            }
        }

        return cleaned.trim();
    }

    private String getTopStackFrames(DetectedError error, int count) {
        if (error.getStackTrace() == null || error.getStackTrace().isEmpty()) {
            return "No stack trace available";
        }

        StringBuilder sb = new StringBuilder();
        int limit = Math.min(count, error.getStackTrace().size());
        for (int i = 0; i < limit; i++) {
            var frame = error.getStackTrace().get(i);
            sb.append(String.format("  at %s.%s(%s:%d)\n",
                frame.getClassName(), frame.getMethodName(),
                frame.getFileName(), frame.getLineNumber()));
        }
        return sb.toString();
    }

    /**
     * Get detailed stack trace with application code highlighted
     */
    private String getDetailedStackTrace(DetectedError error, int count) {
        if (error.getStackTrace() == null || error.getStackTrace().isEmpty()) {
            return "No stack trace available";
        }

        StringBuilder sb = new StringBuilder();
        int limit = Math.min(count, error.getStackTrace().size());

        for (int i = 0; i < limit; i++) {
            var frame = error.getStackTrace().get(i);
            String prefix = frame.isApplicationCode() ? ">>> " : "    ";
            String suffix = frame.isApplicationCode() ? " [APP CODE]" : "";

            sb.append(String.format("%sat %s.%s(%s:%d)%s\n",
                prefix,
                frame.getClassName(),
                frame.getMethodName(),
                frame.getFileName(),
                frame.getLineNumber(),
                suffix));
        }
        return sb.toString();
    }

    private ErrorCategory mockClassify(DetectedError error) {
        logger.info("MOCK MODE: Classifying error {}", error.getExceptionType());

        String type = error.getExceptionType().toLowerCase();
        String message = error.getMessage() != null ? error.getMessage().toLowerCase() : "";
        String rawLog = error.getRawLogEntry() != null ? error.getRawLogEntry().toLowerCase() : "";

        // Check for "Caused by" exceptions in the raw log - classify based on root cause
        if (rawLog.contains("caused by:")) {
            logger.debug("MOCK MODE: Found 'Caused by' chain, analyzing root cause");

            // Check for data/parsing errors in the caused by chain
            if (rawLog.contains("illegalargumentexception") &&
                (rawLog.contains("invalid") || rawLog.contains("format") || rawLog.contains("expected"))) {
                logger.debug("MOCK MODE: Root cause is data format/validation error");
                return ErrorCategory.DATA;
            }

            // Check for other common root causes
            if (rawLog.contains("caused by:") && rawLog.contains("numberformatexception")) {
                return ErrorCategory.DATA;
            }
            if (rawLog.contains("caused by:") && rawLog.contains("jsonprocessingexception")) {
                return ErrorCategory.DATA;
            }
        }

        // Check message for clues about data format errors
        if (message.contains("invalid") && (message.contains("format") || message.contains("balance") || message.contains("expected"))) {
            logger.debug("MOCK MODE: Message indicates data format error");
            return ErrorCategory.DATA;
        }

        // Handle Spring routing exceptions
        if (type.contains("noresourcefound") || type.contains("nohandlerfound") || type.contains("methodnotallowed")) {
            logger.debug("MOCK MODE: Detected missing endpoint/route configuration");
            return ErrorCategory.CONFIG;
        }

        if (type.contains("nullpointer") || type.contains("indexoutofbounds") || type.contains("arithmetic")) {
            return ErrorCategory.CODE;
        } else if (type.contains("io") || type.contains("timeout") || type.contains("connection")) {
            return ErrorCategory.INFRA;
        } else if (type.contains("validation") || type.contains("parse")) {
            return ErrorCategory.DATA;
        } else if (type.contains("config") || type.contains("property")) {
            return ErrorCategory.CONFIG;
        }
        return ErrorCategory.UNKNOWN;
    }

    private FixSuggestion mockFixSuggestion(DetectedError error) {
        logger.info("MOCK MODE: Generating fix suggestion for {}", error.getExceptionType());

        FixSuggestion suggestion = new FixSuggestion();
        String exceptionType = error.getExceptionType().toLowerCase();
        String message = error.getMessage() != null ? error.getMessage() : "";
        List<String> steps = new ArrayList<>();

        // Generate contextual fix suggestions based on exception type
        if (exceptionType.contains("noresourcefound") || exceptionType.contains("nohandlerfound")) {
            suggestion.setSummary("Add missing API endpoint mapping in controller");

            // Try to extract the requested path from the message
            String requestedPath = message.contains("/") ?
                message.substring(message.lastIndexOf("resource") + 9).trim() : "the requested path";

            steps.add("Create or verify @RestController class exists for this API endpoint");
            steps.add("Add @GetMapping or @PostMapping annotation for path: " + requestedPath);
            steps.add("Example: @GetMapping(\"" + requestedPath + "\") public ResponseEntity<?> handleRequest() { ... }");
            steps.add("Verify the path matches exactly (check for typos, case sensitivity, missing /api prefix)");
            steps.add("Restart the application after adding the endpoint");
            suggestion.setConfidence("HIGH");

        } else if (exceptionType.contains("methodnotallowed")) {
            suggestion.setSummary("HTTP method not allowed for this endpoint");
            steps.add("Verify the correct HTTP method annotation (@GetMapping, @PostMapping, @PutMapping, @DeleteMapping)");
            steps.add("Check if the endpoint exists but uses a different HTTP method");
            steps.add("Ensure client is using the correct HTTP verb (GET, POST, PUT, DELETE)");
            steps.add("Add the missing method mapping if needed");
            suggestion.setConfidence("HIGH");

        } else if (exceptionType.contains("nullpointer")) {
            suggestion.setSummary("Add null check before accessing the object to prevent NullPointerException");
            steps.add("Add a null check: if (object != null) { ... } before line " +
                (error.getStackTrace() != null && !error.getStackTrace().isEmpty() ?
                    error.getStackTrace().get(0).getLineNumber() : "N/A"));
            steps.add("Consider using Optional<T> for nullable values in method signatures");
            steps.add("Use Objects.requireNonNull() for parameters that must not be null");
            steps.add("Review object initialization in the calling code path");
            suggestion.setConfidence("HIGH");

            // Add example code comparison
            suggestion.setOriginalCode("String value = someObject.toString();\nint length = value.length();");
            suggestion.setFixedCode("if (someObject != null) {\n    String value = someObject.toString();\n    int length = value.length();\n} else {\n    // Handle null case\n    logger.warn(\"Object is null\");\n}");

        } else if (exceptionType.contains("indexoutofbounds") || exceptionType.contains("arrayindex")) {
            suggestion.setSummary("Add bounds checking before array/list access");
            steps.add("Add validation: if (index >= 0 && index < arr.length) before accessing the element");
            steps.add("Review loop conditions for off-by-one errors");
            steps.add("Consider using enhanced for-loop or streams to avoid manual index management");
            steps.add("Add defensive logging to track actual vs expected array/collection sizes");
            suggestion.setConfidence("HIGH");

            // Add example code comparison
            suggestion.setOriginalCode("int[] arr = {1, 2, 3};\nreturn ResponseEntity.ok(arr[10]);  // Index out of bounds");
            suggestion.setFixedCode("int[] arr = {1, 2, 3};\nint index = 10;\nif (index >= 0 && index < arr.length) {\n    return ResponseEntity.ok(arr[index]);\n} else {\n    return ResponseEntity.badRequest().body(\"Index out of bounds: \" + index);\n}");

        } else if (exceptionType.contains("arithmetic")) {
            suggestion.setSummary("Add validation to prevent division by zero");
            steps.add("Check denominator before division: if (divisor != 0) { result = numerator / divisor; }");
            steps.add("Consider using BigDecimal for precise decimal arithmetic operations");
            steps.add("Add error handling to return a default value or throw a meaningful exception");
            suggestion.setConfidence("HIGH");

            // Add example code comparison
            suggestion.setOriginalCode("int result = 10 / 0;  // Division by zero\nreturn ResponseEntity.ok(result);");
            suggestion.setFixedCode("int divisor = 0;\nif (divisor != 0) {\n    int result = 10 / divisor;\n    return ResponseEntity.ok(result);\n} else {\n    return ResponseEntity.badRequest().body(\"Cannot divide by zero\");\n}");

        } else if (exceptionType.contains("classcast")) {
            suggestion.setSummary("Add type check before casting to prevent ClassCastException");
            steps.add("Use instanceof check: if (obj instanceof TargetType) { TargetType target = (TargetType) obj; }");
            steps.add("Review the type hierarchy and ensure proper casting strategy");
            steps.add("Consider using generics to eliminate runtime casts");
            suggestion.setConfidence("MEDIUM");

        } else if (exceptionType.contains("io")) {
            suggestion.setSummary("Add proper error handling and resource management for I/O operations");
            steps.add("Use try-with-resources for automatic resource cleanup");
            steps.add("Add specific exception handling for FileNotFoundException and IOException");
            steps.add("Verify file path exists and has correct permissions");
            steps.add("Consider using java.nio.file.Files for better error messages");
            suggestion.setConfidence("MEDIUM");

        } else if (exceptionType.contains("validation") || exceptionType.contains("illegal")) {
            suggestion.setSummary("Add input validation before processing data");
            steps.add("Add @Valid and @NotNull annotations to method parameters");
            steps.add("Implement custom validators for complex business rules");
            steps.add("Provide clear error messages explaining what validation failed");
            suggestion.setConfidence("MEDIUM");

        } else if (message.contains("Invalid balance format") || message.contains("expected numeric value but got text")) {
            suggestion.setSummary("Backend API returns balance as String instead of number - fix parsing logic to handle String balance values");
            steps.add("Step 1: The external banking API is returning balance as a String (\"5000.50\") instead of a numeric value");
            steps.add("Step 2: Fix Option A: Update backend API to return balance as a number, not a string");
            steps.add("Step 3: Fix Option B (Recommended): Update AccountsService to handle String balance by converting it to BigDecimal");
            steps.add("Step 4: Add validation to ensure the balance string is a valid decimal number before parsing");
            steps.add("Step 5: Add error handling for malformed balance values");
            suggestion.setConfidence("HIGH");

            // Add example code comparison
            suggestion.setOriginalCode("// Current code - expects numeric balance\nif (!accountNode.path(\"balance\").isNumber()) {\n    throw new IllegalArgumentException(\"Invalid balance format...\");\n}\nBigDecimal balance = accountNode.path(\"balance\").decimalValue();");
            suggestion.setFixedCode("// Fixed code - handles both String and numeric balance\nJsonNode balanceNode = accountNode.path(\"balance\");\nBigDecimal balance;\n\nif (balanceNode.isNumber()) {\n    balance = balanceNode.decimalValue();\n} else if (balanceNode.isTextual()) {\n    // Backend API returns balance as String - convert it\n    try {\n        balance = new BigDecimal(balanceNode.asText());\n    } catch (NumberFormatException e) {\n        throw new IllegalArgumentException(\n            \"Invalid balance format for account \" + accountNode.path(\"accountNumber\").asText() + \n            \": '\" + balanceNode.asText() + \"' is not a valid number\", e\n        );\n    }\n} else {\n    throw new IllegalArgumentException(\n        \"Invalid balance format for account \" + accountNode.path(\"accountNumber\").asText() +\n        \": expected number or text, got \" + balanceNode.getNodeType()\n    );\n}\naccount.setBalance(balance);");

        } else {
            suggestion.setSummary("Investigate and fix " + error.getExceptionType());
            steps.add("Review the error message: " + error.getMessage());
            steps.add("Examine the stack trace to identify the exact line causing the issue");
            steps.add("Add logging to understand the failure path and variable states");
            steps.add("Write a unit test to reproduce the issue consistently");
            suggestion.setConfidence("LOW");
        }

        suggestion.setSteps(steps);
        return suggestion;
    }

    private static class FixSuggestionDto {
        public String summary;
        public List<String> steps;
        public String confidence;
        public String originalCode;
        public String fixedCode;
    }
}
