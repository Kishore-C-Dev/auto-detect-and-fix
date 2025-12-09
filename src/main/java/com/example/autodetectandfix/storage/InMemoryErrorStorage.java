package com.example.autodetectandfix.storage;

import com.example.autodetectandfix.model.DetectedError;
import com.example.autodetectandfix.model.ErrorCategory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * In-memory implementation of ErrorStorage using ConcurrentHashMap.
 * Provides thread-safe storage with deduplication and automatic cleanup.
 */
@Repository
public class InMemoryErrorStorage implements ErrorStorage {

    private static final Logger logger = LoggerFactory.getLogger(InMemoryErrorStorage.class);

    @Value("${app.analysis.max-stored-errors}")
    private int maxStoredErrors;

    @Value("${app.analysis.error-retention-hours}")
    private int retentionHours;

    // Thread-safe storage
    private final Map<String, DetectedError> errors = new ConcurrentHashMap<>();

    @Override
    public void storeError(DetectedError error) {
        // Check if similar error already exists (deduplication)
        Optional<DetectedError> existing = findSimilarError(error);

        if (existing.isPresent()) {
            // Increment occurrence count instead of storing duplicate
            DetectedError existingError = existing.get();
            existingError.setOccurrenceCount(existingError.getOccurrenceCount() + 1);
            logger.debug("Incremented occurrence count for error: {} (now {})",
                existingError.getId(), existingError.getOccurrenceCount());
        } else {
            // Store new error
            errors.put(error.getId(), error);
            logger.info("Stored new error: {} - {} (total: {})",
                error.getId(), error.getExceptionType(), errors.size());

            // Check size limit
            if (errors.size() > maxStoredErrors) {
                removeOldestError();
            }
        }
    }

    @Override
    public Optional<DetectedError> getErrorById(String id) {
        return Optional.ofNullable(errors.get(id));
    }

    @Override
    public List<DetectedError> getAllErrors(int limit) {
        return errors.values().stream()
            .sorted(Comparator.comparing(DetectedError::getDetectedAt).reversed())
            .limit(limit)
            .collect(Collectors.toList());
    }

    @Override
    public List<DetectedError> getErrorsByCategory(String category) {
        try {
            ErrorCategory cat = ErrorCategory.valueOf(category.toUpperCase());

            return errors.values().stream()
                .filter(e -> e.getCategory() == cat)
                .sorted(Comparator.comparing(DetectedError::getDetectedAt).reversed())
                .collect(Collectors.toList());
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid category: {}", category);
            return Collections.emptyList();
        }
    }

    @Override
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();

        stats.put("totalErrors", errors.size());
        stats.put("byCategory", getErrorCountByCategory());
        stats.put("recentErrors1Hour", getRecentErrorCount(1));
        stats.put("recentErrors24Hours", getRecentErrorCount(24));
        stats.put("maxCapacity", maxStoredErrors);
        stats.put("retentionHours", retentionHours);

        return stats;
    }

    @Override
    public void deleteError(String id) {
        errors.remove(id);
        logger.info("Deleted error: {}", id);
    }

    @Override
    public void clearAll() {
        int count = errors.size();
        errors.clear();
        logger.info("Cleared all {} stored errors", count);
    }

    /**
     * Scheduled cleanup of old errors beyond retention period.
     * Runs every hour.
     */
    @Scheduled(fixedRate = 3600000) // Every hour
    public void cleanupOldErrors() {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(retentionHours);

        List<String> toRemove = errors.values().stream()
            .filter(e -> e.getDetectedAt().isBefore(cutoff))
            .map(DetectedError::getId)
            .collect(Collectors.toList());

        toRemove.forEach(errors::remove);

        if (!toRemove.isEmpty()) {
            logger.info("Cleaned up {} old errors (retention: {} hours)",
                toRemove.size(), retentionHours);
        }
    }

    /**
     * Finds a similar error for deduplication.
     * Errors are considered similar if they have the same exception type
     * and similar stack trace (first 3 frames).
     */
    private Optional<DetectedError> findSimilarError(DetectedError error) {
        return errors.values().stream()
            .filter(e -> e.getExceptionType().equals(error.getExceptionType()))
            .filter(e -> isSimilarStackTrace(e, error))
            .findFirst();
    }

    /**
     * Compares stack traces for similarity.
     * Returns true if first 3 frames match.
     */
    private boolean isSimilarStackTrace(DetectedError e1, DetectedError e2) {
        if (e1.getStackTrace() == null || e2.getStackTrace() == null) {
            return false;
        }

        if (e1.getStackTrace().size() < 3 || e2.getStackTrace().size() < 3) {
            return false;
        }

        // Compare first 3 stack frames
        for (int i = 0; i < Math.min(3, Math.min(e1.getStackTrace().size(), e2.getStackTrace().size())); i++) {
            var st1 = e1.getStackTrace().get(i);
            var st2 = e2.getStackTrace().get(i);

            if (!st1.getClassName().equals(st2.getClassName()) ||
                !st1.getMethodName().equals(st2.getMethodName())) {
                return false;
            }
        }

        return true;
    }

    /**
     * Removes the oldest error to maintain size limit.
     */
    private void removeOldestError() {
        errors.values().stream()
            .min(Comparator.comparing(DetectedError::getDetectedAt))
            .ifPresent(oldest -> {
                errors.remove(oldest.getId());
                logger.debug("Removed oldest error {} to maintain size limit", oldest.getId());
            });
    }

    /**
     * Gets count of errors by category.
     */
    private Map<String, Integer> getErrorCountByCategory() {
        Map<String, Integer> counts = new HashMap<>();

        for (ErrorCategory category : ErrorCategory.values()) {
            long count = errors.values().stream()
                .filter(e -> e.getCategory() == category)
                .count();
            counts.put(category.name(), (int) count);
        }

        return counts;
    }

    /**
     * Gets count of errors detected in the last N hours.
     */
    private int getRecentErrorCount(int hours) {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(hours);

        return (int) errors.values().stream()
            .filter(e -> e.getDetectedAt().isAfter(cutoff))
            .count();
    }
}
