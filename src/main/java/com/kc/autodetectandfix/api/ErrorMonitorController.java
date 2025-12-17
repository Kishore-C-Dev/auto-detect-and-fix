package com.kc.autodetectandfix.api;

import com.kc.autodetectandfix.model.DetectedError;
import com.kc.autodetectandfix.storage.ErrorStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST API controller for querying and managing detected errors.
 * Provides endpoints to retrieve, filter, and analyze detected errors.
 */
@RestController
@RequestMapping("/api/errors")
public class ErrorMonitorController {

    private static final Logger logger = LoggerFactory.getLogger(ErrorMonitorController.class);

    private final ErrorStorage errorStorage;

    public ErrorMonitorController(ErrorStorage errorStorage) {
        this.errorStorage = errorStorage;
    }

    /**
     * Get all detected errors with optional limit and category filter.
     *
     * @param limit    Maximum number of errors to return (default: 100)
     * @param category Optional category filter (CONFIG, DATA, INFRA, CODE)
     * @return List of detected errors
     */
    @GetMapping
    public ResponseEntity<List<DetectedError>> getAllErrors(
            @RequestParam(required = false, defaultValue = "100") Integer limit,
            @RequestParam(required = false) String category) {

        logger.debug("GET /api/errors - limit: {}, category: {}", limit, category);

        List<DetectedError> errors;

        if (category != null && !category.trim().isEmpty()) {
            errors = errorStorage.getErrorsByCategory(category);
            // Apply limit if category filter is used
            if (errors.size() > limit) {
                errors = errors.subList(0, limit);
            }
        } else {
            errors = errorStorage.getAllErrors(limit);
        }

        logger.info("Returning {} errors", errors.size());
        return ResponseEntity.ok(errors);
    }

    /**
     * Get a specific error by ID.
     *
     * @param id The error ID
     * @return The detected error with full details
     */
    @GetMapping("/{id}")
    public ResponseEntity<DetectedError> getErrorById(@PathVariable String id) {
        logger.debug("GET /api/errors/{}", id);

        return errorStorage.getErrorById(id)
                .map(error -> {
                    logger.info("Found error: {}", id);
                    return ResponseEntity.ok(error);
                })
                .orElseGet(() -> {
                    logger.warn("Error not found: {}", id);
                    return ResponseEntity.notFound().build();
                });
    }

    /**
     * Get error statistics and metrics.
     *
     * @return Statistics including total count, count by category, recent errors
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStatistics() {
        logger.debug("GET /api/errors/stats");

        Map<String, Object> stats = errorStorage.getStatistics();

        logger.info("Returning statistics: total={}", stats.get("totalErrors"));
        return ResponseEntity.ok(stats);
    }

    /**
     * Delete a specific error by ID.
     *
     * @param id The error ID to delete
     * @return 204 No Content if successful
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteError(@PathVariable String id) {
        logger.debug("DELETE /api/errors/{}", id);

        if (errorStorage.getErrorById(id).isEmpty()) {
            logger.warn("Cannot delete - error not found: {}", id);
            return ResponseEntity.notFound().build();
        }

        errorStorage.deleteError(id);
        logger.info("Deleted error: {}", id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Clear all stored errors.
     *
     * @return 204 No Content
     */
    @DeleteMapping
    public ResponseEntity<Void> clearAllErrors() {
        logger.debug("DELETE /api/errors (clear all)");

        errorStorage.clearAll();
        logger.info("Cleared all errors");
        return ResponseEntity.noContent().build();
    }

    /**
     * Health check endpoint for the error monitoring system.
     *
     * @return Simple status response
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "error-monitoring"
        ));
    }
}
