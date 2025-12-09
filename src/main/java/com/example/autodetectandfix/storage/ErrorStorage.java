package com.example.autodetectandfix.storage;

import com.example.autodetectandfix.model.DetectedError;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Interface for error storage operations.
 * Allows for different storage implementations (in-memory, database, etc.)
 */
public interface ErrorStorage {

    /**
     * Stores a detected error.
     * Implements deduplication if similar error already exists.
     *
     * @param error The error to store
     */
    void storeError(DetectedError error);

    /**
     * Retrieves an error by its ID.
     *
     * @param id The error ID
     * @return Optional containing the error if found
     */
    Optional<DetectedError> getErrorById(String id);

    /**
     * Gets all errors up to a specified limit.
     *
     * @param limit Maximum number of errors to return
     * @return List of errors sorted by detection time (newest first)
     */
    List<DetectedError> getAllErrors(int limit);

    /**
     * Gets errors filtered by category.
     *
     * @param category The error category (CONFIG, DATA, INFRA, CODE)
     * @return List of errors in the specified category
     */
    List<DetectedError> getErrorsByCategory(String category);

    /**
     * Gets error statistics.
     *
     * @return Map containing statistics (total count, count by category, etc.)
     */
    Map<String, Object> getStatistics();

    /**
     * Deletes a specific error.
     *
     * @param id The error ID to delete
     */
    void deleteError(String id);

    /**
     * Clears all stored errors.
     */
    void clearAll();
}
