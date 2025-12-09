package com.example.autodetectandfix.analysis;

import com.example.autodetectandfix.event.ErrorDetectedEvent;
import com.example.autodetectandfix.git.GitRepositoryService;
import com.example.autodetectandfix.model.*;
import com.example.autodetectandfix.storage.ErrorStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    public CodeAnalyzer(GitRepositoryService gitService,
                       FixSuggestionGenerator suggestionGenerator,
                       ErrorStorage errorStorage) {
        this.gitService = gitService;
        this.suggestionGenerator = suggestionGenerator;
        this.errorStorage = errorStorage;
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

        logger.info("Analyzing error: {} - {}", error.getId(), error.getExceptionType());

        try {
            // Find the first application stack trace element
            Optional<StackTraceElement> appElement = error.getStackTrace().stream()
                .filter(StackTraceElement::isApplicationCode)
                .findFirst();

            SourceCodeContext context = null;

            if (appElement.isPresent()) {
                StackTraceElement element = appElement.get();

                logger.debug("Found application code at: {}:{}",
                    element.getClassName(), element.getLineNumber());

                // Get source code context from Git
                Optional<SourceCodeContext> contextOpt = gitService.getSourceContext(
                    element.getClassName(),
                    element.getLineNumber()
                );

                context = contextOpt.orElse(null);

                if (context == null) {
                    logger.warn("Could not retrieve source context for {}",
                        element.getClassName());
                }
            } else {
                logger.debug("No application code found in stack trace for error: {}",
                    error.getId());
            }

            // Generate fix suggestion (even if no context available)
            FixSuggestion suggestion = suggestionGenerator.generateSuggestion(error, context);
            error.setFixSuggestion(suggestion);

            // Store the error with analysis
            errorStorage.storeError(error);

            logger.info("Completed analysis for error: {} (confidence: {})",
                error.getId(), suggestion.getConfidence());

        } catch (Exception e) {
            logger.error("Error during code analysis for " + error.getId(), e);

            // Store the error even if analysis failed
            try {
                errorStorage.storeError(error);
            } catch (Exception storageEx) {
                logger.error("Failed to store error after analysis failure", storageEx);
            }
        }
    }
}
