package com.example.autodetectandfix.analysis;

import com.example.autodetectandfix.event.ErrorDetectedEvent;
import com.example.autodetectandfix.model.DetectedError;
import com.example.autodetectandfix.model.ErrorCategory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/**
 * Classifies detected errors into categories using rule-based matching.
 * Runs before CodeAnalyzer to set the category for downstream processing.
 */
@Component
public class ErrorClassifier {

    private static final Logger logger = LoggerFactory.getLogger(ErrorClassifier.class);

    // CONFIG error patterns
    private static final Pattern CONFIG_PATTERN =
        Pattern.compile(".*(PropertySource|Configuration|@Value|application\\.yml|application\\.properties|" +
                        "profile|missing.*property|config).*", Pattern.CASE_INSENSITIVE);

    // DATA error patterns
    private static final Pattern DATA_PATTERN =
        Pattern.compile(".*(ValidationException|DataIntegrityViolation|ConstraintViolation|" +
                        "ParseException|invalid.*data|validation.*failed|format.*invalid).*",
                        Pattern.CASE_INSENSITIVE);

    // INFRA error patterns
    private static final Pattern INFRA_PATTERN =
        Pattern.compile(".*(IOException|ConnectException|TimeoutException|SocketException|" +
                        "UnknownHostException|connection.*refused|host.*unreachable|" +
                        "database.*unavailable|network).*", Pattern.CASE_INSENSITIVE);

    // CODE error patterns
    private static final Pattern CODE_PATTERN =
        Pattern.compile(".*(NullPointerException|IndexOutOfBoundsException|" +
                        "ArrayIndexOutOfBoundsException|ArithmeticException|" +
                        "ClassCastException|IllegalStateException).*", Pattern.CASE_INSENSITIVE);

    /**
     * Listens for error detected events and classifies them.
     * Runs with @Order(1) to execute before CodeAnalyzer.
     */
    @EventListener
    @Order(1)
    public void onErrorDetected(ErrorDetectedEvent event) {
        DetectedError error = event.getError();

        ErrorCategory category = classifyError(error);
        error.setCategory(category);

        logger.debug("Classified error {} as: {}", error.getId(), category);
    }

    /**
     * Classifies an error into a category based on exception type and message.
     *
     * @param error The detected error to classify
     * @return The determined ErrorCategory
     */
    public ErrorCategory classifyError(DetectedError error) {
        String exceptionType = error.getExceptionType();
        String message = error.getMessage() != null ? error.getMessage() : "";
        String rawLog = error.getRawLogEntry() != null ? error.getRawLogEntry() : "";

        String combinedText = exceptionType + " " + message + " " + rawLog;

        // Check patterns in order of specificity
        if (CONFIG_PATTERN.matcher(combinedText).find()) {
            return ErrorCategory.CONFIG;
        }

        if (DATA_PATTERN.matcher(combinedText).find()) {
            return ErrorCategory.DATA;
        }

        if (INFRA_PATTERN.matcher(combinedText).find()) {
            return ErrorCategory.INFRA;
        }

        if (CODE_PATTERN.matcher(combinedText).find()) {
            return ErrorCategory.CODE;
        }

        // Additional logic-based classification
        if (exceptionType.contains("Config") || exceptionType.contains("Property")) {
            return ErrorCategory.CONFIG;
        }

        if (exceptionType.contains("Validation") || exceptionType.contains("Parse")) {
            return ErrorCategory.DATA;
        }

        if (exceptionType.contains("IO") || exceptionType.contains("Network") ||
            exceptionType.contains("Connection") || exceptionType.contains("Timeout")) {
            return ErrorCategory.INFRA;
        }

        if (exceptionType.contains("NullPointer") || exceptionType.contains("Index") ||
            exceptionType.contains("Arithmetic") || exceptionType.contains("Cast")) {
            return ErrorCategory.CODE;
        }

        logger.debug("Could not classify error type: {}", exceptionType);
        return ErrorCategory.UNKNOWN;
    }
}
