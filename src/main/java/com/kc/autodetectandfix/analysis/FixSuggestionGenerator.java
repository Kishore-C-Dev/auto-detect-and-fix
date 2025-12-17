package com.kc.autodetectandfix.analysis;

import com.kc.autodetectandfix.ai.OpenAiService;
import com.kc.autodetectandfix.config.OpenAiConfig;
import com.kc.autodetectandfix.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Generates fix suggestions for detected errors using OpenAI or rule-based heuristics.
 * Tries OpenAI first for intelligent suggestions, falls back to rules if unavailable.
 */
@Component
public class FixSuggestionGenerator {

    private static final Logger logger = LoggerFactory.getLogger(FixSuggestionGenerator.class);

    private final OpenAiService openAiService;
    private final OpenAiConfig openAiConfig;

    public FixSuggestionGenerator(OpenAiService openAiService, OpenAiConfig openAiConfig) {
        this.openAiService = openAiService;
        this.openAiConfig = openAiConfig;
    }

    /**
     * Generates a fix suggestion for a detected error using OpenAI or rule-based logic.
     *
     * @param error   The detected error
     * @param context The source code context (may be null)
     * @return FixSuggestion with actionable steps
     */
    public FixSuggestion generateSuggestion(DetectedError error, SourceCodeContext context) {
        // Try OpenAI suggestion first
        if (openAiConfig.isEnabled()) {
            try {
                FixSuggestion aiSuggestion = openAiService.generateFixSuggestion(error, context);
                logger.info("Generated fix suggestion via OpenAI (confidence: {})", aiSuggestion.getConfidence());
                return aiSuggestion;
            } catch (Exception e) {
                logger.warn("OpenAI fix generation failed, falling back to rule-based: {}", e.getMessage());
                // Fall through to rule-based generation
            }
        }

        // Fallback to rule-based suggestions
        return generateRuleBasedSuggestion(error, context);
    }

    /**
     * Fallback rule-based suggestion generation.
     */
    private FixSuggestion generateRuleBasedSuggestion(DetectedError error, SourceCodeContext context) {
        FixSuggestion suggestion = new FixSuggestion();

        String exceptionType = error.getExceptionType();
        ErrorCategory category = error.getCategory();

        suggestion.setSummary(generateSummary(exceptionType, category));
        suggestion.setSteps(generateSteps(exceptionType, category, error));
        suggestion.setSourceContext(context);
        suggestion.setConfidence(determineConfidence(error, context));

        return suggestion;
    }

    /**
     * Generates a human-readable summary of the suggested fix.
     */
    private String generateSummary(String exceptionType, ErrorCategory category) {
        return switch (category) {
            case CONFIG -> "Configuration issue detected. Review application properties and environment variables.";
            case DATA -> "Data validation or parsing error. Check input data format and validation rules.";
            case INFRA -> "Infrastructure connectivity issue. Verify external dependencies and network configuration.";
            case CODE -> "Code logic error in " + exceptionType + ". Review implementation and add defensive checks.";
            default -> "Error detected. Manual investigation required to determine root cause.";
        };
    }

    /**
     * Generates specific actionable steps based on error type.
     */
    private List<String> generateSteps(String exceptionType, ErrorCategory category, DetectedError error) {
        List<String> steps = new ArrayList<>();

        // Specific handling for common exception types
        if (exceptionType.contains("NullPointerException")) {
            steps.add("Add null check before accessing the object: if (obj != null) { ... }");
            steps.add("Consider using Optional<T> for nullable values");
            steps.add("Review object initialization in this code path");
            steps.add("Use Objects.requireNonNull() for method parameters that must not be null");

        } else if (exceptionType.contains("ArrayIndexOutOfBounds") || exceptionType.contains("IndexOutOfBounds")) {
            steps.add("Add bounds checking before array/list access: if (index >= 0 && index < array.length)");
            steps.add("Verify collection size before accessing elements");
            steps.add("Consider using enhanced for-loop or streams to avoid index management");
            steps.add("Review loop conditions and off-by-one errors");

        } else if (exceptionType.contains("ArithmeticException")) {
            steps.add("Add validation to prevent division by zero");
            steps.add("Check denominator value before arithmetic operation: if (divisor != 0)");
            steps.add("Consider using BigDecimal for precise decimal arithmetic");

        } else if (exceptionType.contains("ClassCastException")) {
            steps.add("Use instanceof check before casting: if (obj instanceof TargetType)");
            steps.add("Review type hierarchy and ensure correct casting");
            steps.add("Consider using generics to avoid runtime casts");

        } else if (category == ErrorCategory.CONFIG) {
            steps.add("Check application.yml/application.properties for missing or incorrect properties");
            steps.add("Verify environment variables are set correctly");
            steps.add("Review @Value annotations for correct property names and default values");
            steps.add("Ensure active Spring profile matches your environment");

        } else if (category == ErrorCategory.DATA) {
            steps.add("Add input validation using @Valid and @NotNull annotations");
            steps.add("Implement custom validators for complex business rules");
            steps.add("Review data format expectations and provide clear error messages");
            steps.add("Check database constraints and entity mappings");

        } else if (category == ErrorCategory.INFRA) {
            steps.add("Check network connectivity to external service");
            steps.add("Verify service credentials, endpoints, and ports");
            steps.add("Add retry logic with exponential backoff for transient failures");
            steps.add("Implement circuit breaker pattern using Resilience4j");
            steps.add("Check firewall rules and DNS resolution");

        } else {
            // Generic steps for unknown error types
            steps.add("Review the stack trace to identify the exact line causing the error");
            steps.add("Add logging to understand the failure path and variable states");
            steps.add("Write a unit test to reproduce the issue consistently");
            steps.add("Check recent code changes that might have introduced this error");
        }

        return steps;
    }

    /**
     * Determines confidence level of the suggestion.
     */
    private String determineConfidence(DetectedError error, SourceCodeContext context) {
        // High confidence if we have source context and a known category
        if (context != null && error.getCategory() != ErrorCategory.UNKNOWN) {
            return "HIGH";
        }

        // Medium confidence if we have a category but no source context
        if (error.getCategory() != ErrorCategory.UNKNOWN) {
            return "MEDIUM";
        }

        // Low confidence for unknown categories
        return "LOW";
    }
}
