package com.kc.autodetectandfix.analysis;

import com.kc.autodetectandfix.ai.OpenAiService;
import com.kc.autodetectandfix.config.OpenAiConfig;
import com.kc.autodetectandfix.event.ErrorDetectedEvent;
import com.kc.autodetectandfix.model.DetectedError;
import com.kc.autodetectandfix.model.ErrorCategory;
import com.kc.autodetectandfix.model.StackTraceElement;
import com.kc.autodetectandfix.storage.ErrorStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Classifies detected errors into categories using AI-driven analysis.
 * Analyzes error context, stack trace, and source code to determine the root cause category.
 * Runs before CodeAnalyzer to set the category for downstream processing.
 *
 * Only uses minimal fallback classification when AI is completely unavailable.
 * Checks for duplicates before calling AI to avoid unnecessary API calls.
 */
@Component
public class ErrorClassifier {

    private static final Logger logger = LoggerFactory.getLogger(ErrorClassifier.class);

    private final OpenAiService openAiService;
    private final OpenAiConfig openAiConfig;
    private final ErrorStorage errorStorage;

    public ErrorClassifier(OpenAiService openAiService, OpenAiConfig openAiConfig, ErrorStorage errorStorage) {
        this.openAiService = openAiService;
        this.openAiConfig = openAiConfig;
        this.errorStorage = errorStorage;
    }

    // Note: Regex patterns removed - system now relies on AI analysis for accurate classification
    // Minimal fallback logic is implemented in basicFallbackClassification() method

    /**
     * Listens for error detected events and classifies them.
     * Runs with @Order(1) to execute before CodeAnalyzer.
     */
    @EventListener
    @Order(1)
    public void onErrorDetected(ErrorDetectedEvent event) {
        DetectedError error = event.getError();

        // Check if this is a duplicate error first (avoid unnecessary AI calls)
        if (isDuplicate(error)) {
            logger.info("Duplicate error detected BEFORE AI call: {}. Skipping AI classification and analysis entirely.", error.getExceptionType());
            // Don't process further - CodeAnalyzer will also skip this
            return;
        }

        ErrorCategory category = classifyError(error);
        error.setCategory(category);

        logger.debug("Classified error {} as: {}", error.getId(), category);
    }

    /**
     * Quick check if this error is a duplicate based on exception type and first few stack frames.
     * Checks existing errors in storage to avoid calling AI for duplicates.
     */
    private boolean isDuplicate(DetectedError error) {
        if (error.getStackTrace() == null || error.getStackTrace().size() < 3) {
            return false;
        }

        // Check all recent errors in storage
        List<DetectedError> recentErrors = errorStorage.getAllErrors(100);

        for (DetectedError existingError : recentErrors) {
            if (isSimilarError(error, existingError)) {
                logger.debug("Found similar error in storage: {} (occurrence: {})",
                    existingError.getId(), existingError.getOccurrenceCount());
                return true;
            }
        }

        return false;
    }

    /**
     * Checks if two errors are similar based on exception type and first 3 stack frames.
     */
    private boolean isSimilarError(DetectedError e1, DetectedError e2) {
        // Must have same exception type
        if (!e1.getExceptionType().equals(e2.getExceptionType())) {
            return false;
        }

        // Must have stack traces
        if (e1.getStackTrace() == null || e2.getStackTrace() == null) {
            return false;
        }

        if (e1.getStackTrace().size() < 3 || e2.getStackTrace().size() < 3) {
            return false;
        }

        // Compare first 3 stack frames
        for (int i = 0; i < 3; i++) {
            StackTraceElement st1 = e1.getStackTrace().get(i);
            StackTraceElement st2 = e2.getStackTrace().get(i);

            if (!st1.getClassName().equals(st2.getClassName()) ||
                !st1.getMethodName().equals(st2.getMethodName())) {
                return false;
            }
        }

        return true;
    }

    /**
     * Classifies an error into a category using OpenAI.
     * Only falls back to basic classification if OpenAI is completely unavailable.
     *
     * @param error The detected error to classify
     * @return The determined ErrorCategory
     */
    public ErrorCategory classifyError(DetectedError error) {
        // Always try OpenAI classification first if enabled
        if (openAiConfig.isEnabled()) {
            try {
                ErrorCategory aiCategory = openAiService.classifyError(error);
                logger.info("Classified error {} as {} (via AI analysis)", error.getId(), aiCategory);
                return aiCategory;
            } catch (Exception e) {
                logger.warn("OpenAI classification failed: {}. Using minimal fallback.", e.getMessage());
                // Fall through to minimal fallback
            }
        } else {
            logger.debug("OpenAI disabled, using minimal classification");
        }

        // Minimal fallback - only used when AI is unavailable
        return basicFallbackClassification(error);
    }

    /**
     * Minimal fallback classification - only used when AI is completely unavailable.
     * This is intentionally basic to encourage relying on AI analysis.
     */
    private ErrorCategory basicFallbackClassification(DetectedError error) {
        String exceptionType = error.getExceptionType();
        String message = error.getMessage() != null ? error.getMessage() : "";

        logger.debug("Using basic fallback classification for error {}", error.getId());

        // Very basic classification based only on exception type
        // Most errors will be UNKNOWN, which is intentional to highlight the need for AI

        // Common coding errors
        if (exceptionType.contains("NullPointer")) {
            return ErrorCategory.CODE;
        }

        // Configuration/routing
        if (exceptionType.contains("NoResourceFoundException") ||
            exceptionType.contains("NoHandlerFoundException") ||
            exceptionType.contains("Property")) {
            return ErrorCategory.CONFIG;
        }

        // Infrastructure
        if (exceptionType.contains("IOException") ||
            exceptionType.contains("ConnectException") ||
            exceptionType.contains("TimeoutException")) {
            return ErrorCategory.INFRA;
        }

        logger.warn("Could not classify error type: {}. Consider enabling OpenAI for accurate classification.",
            exceptionType);
        return ErrorCategory.UNKNOWN;
    }
}
