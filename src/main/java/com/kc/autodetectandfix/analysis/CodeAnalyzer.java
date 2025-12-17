package com.kc.autodetectandfix.analysis;

import com.kc.autodetectandfix.event.ErrorDetectedEvent;
import com.kc.autodetectandfix.event.ErrorStoredEvent;
import com.kc.autodetectandfix.git.GitRepositoryService;
import com.kc.autodetectandfix.model.*;
import com.kc.autodetectandfix.storage.ErrorStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Orchestrates code analysis for detected errors.
 * Runs asynchronously to avoid blocking log processing.
 * Executes after ErrorClassifier (Order 2).
 */
@Component
public class CodeAnalyzer {

    private static final Logger logger = LoggerFactory.getLogger(CodeAnalyzer.class);

    private final GitRepositoryService gitService;
    private final FixSuggestionGenerator suggestionGenerator;
    private final ErrorStorage errorStorage;
    private final ApplicationEventPublisher eventPublisher;

    public CodeAnalyzer(GitRepositoryService gitService,
                       FixSuggestionGenerator suggestionGenerator,
                       ErrorStorage errorStorage,
                       ApplicationEventPublisher eventPublisher) {
        this.gitService = gitService;
        this.suggestionGenerator = suggestionGenerator;
        this.errorStorage = errorStorage;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Analyzes detected errors asynchronously.
     * Retrieves source code context and generates fix suggestions.
     */
    @Async
    @EventListener
    @Order(2)
    public void onErrorDetected(ErrorDetectedEvent event) {
        DetectedError error = event.getError();

        // Double-check for duplicates (ErrorClassifier also checks, but this is a safety net)
        if (isDuplicateError(error)) {
            logger.info("Skipping analysis - duplicate error detected: {}", error.getExceptionType());
            return;
        }

        logger.info("Analyzing NEW error: {} - {}", error.getId(), error.getExceptionType());

        try {
            // Log all stack frames for debugging
            logger.debug("Analyzing error {} with {} stack frames",
                error.getId(), error.getStackTrace() != null ? error.getStackTrace().size() : 0);

            if (error.getStackTrace() != null) {
                logger.debug("Stack frames:");
                error.getStackTrace().forEach(frame -> {
                    logger.debug("  - {} [isAppCode={}]",
                        frame.getClassName(), frame.isApplicationCode());
                });
            }

            // Find the first application stack trace element
            Optional<com.kc.autodetectandfix.model.StackTraceElement> appElement = error.getStackTrace().stream()
                .filter(com.kc.autodetectandfix.model.StackTraceElement::isApplicationCode)
                .findFirst();

            SourceCodeContext context = null;

            if (appElement.isPresent()) {
                com.kc.autodetectandfix.model.StackTraceElement element = appElement.get();

                logger.debug("Found application code at: {}.{}({}:{})",
                    element.getClassName(),
                    element.getMethodName(),
                    element.getFileName(),
                    element.getLineNumber());

                // Get source code context from Git
                Optional<SourceCodeContext> contextOpt = gitService.getSourceContext(
                    element.getClassName(),
                    element.getLineNumber()
                );

                if (contextOpt.isPresent()) {
                    context = contextOpt.get();
                    logger.debug("Retrieved source context from: {}", context.getFilePath());
                } else {
                    logger.warn("Failed to retrieve source code for: {}", element.getClassName());
                }
            } else {
                logger.warn("No application code found in stack trace for error: {}", error.getId());
            }

            // Generate fix suggestion (even if no context available)
            FixSuggestion suggestion = suggestionGenerator.generateSuggestion(error, context);
            error.setFixSuggestion(suggestion);

            // Store the error with analysis (returns true if new, false if duplicate)
            boolean isNewError = errorStorage.storeError(error);

            // Only publish event (and send email) for NEW errors, not duplicates
            if (isNewError) {
                eventPublisher.publishEvent(new ErrorStoredEvent(this, error));
                logger.info("Completed analysis for NEW error: {} (confidence: {}). Email will be sent.",
                    error.getId(), suggestion.getConfidence());
            } else {
                logger.info("Completed analysis for DUPLICATE error: {}. Email will NOT be sent.",
                    error.getId());
            }

        } catch (Exception e) {
            logger.error("Error during code analysis for " + error.getId(), e);

            // Store the error even if analysis failed
            try {
                boolean isNewError = errorStorage.storeError(error);

                // Only publish event (and send email) for new errors
                if (isNewError) {
                    eventPublisher.publishEvent(new ErrorStoredEvent(this, error));
                    logger.info("Stored NEW error after analysis failure. Email will be sent.");
                } else {
                    logger.info("DUPLICATE error after analysis failure. Email will NOT be sent.");
                }
            } catch (Exception storageEx) {
                logger.error("Failed to store error after analysis failure", storageEx);
            }
        }
    }

    /**
     * Checks if this error is a duplicate by comparing with existing errors in storage.
     */
    private boolean isDuplicateError(DetectedError error) {
        if (error.getStackTrace() == null || error.getStackTrace().size() < 3) {
            return false;
        }

        // Check recently stored errors
        var recentErrors = errorStorage.getAllErrors(100);

        for (DetectedError existing : recentErrors) {
            if (isSimilarStackTrace(error, existing)) {
                logger.debug("Found duplicate error in storage: {} (occurrence: {})",
                    existing.getId(), existing.getOccurrenceCount());
                return true;
            }
        }

        return false;
    }

    /**
     * Compares two errors to see if they have similar stack traces.
     */
    private boolean isSimilarStackTrace(DetectedError e1, DetectedError e2) {
        if (!e1.getExceptionType().equals(e2.getExceptionType())) {
            return false;
        }

        if (e1.getStackTrace() == null || e2.getStackTrace() == null) {
            return false;
        }

        if (e1.getStackTrace().size() < 3 || e2.getStackTrace().size() < 3) {
            return false;
        }

        // Compare first 3 stack frames
        for (int i = 0; i < 3; i++) {
            var st1 = e1.getStackTrace().get(i);
            var st2 = e2.getStackTrace().get(i);

            if (!st1.getClassName().equals(st2.getClassName()) ||
                !st1.getMethodName().equals(st2.getMethodName())) {
                return false;
            }
        }

        return true;
    }
}
