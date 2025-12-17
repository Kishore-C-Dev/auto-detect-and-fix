package com.kc.autodetectandfix.model;

/**
 * Enum representing the category of detected errors.
 *
 * Categories are used to classify errors for better analysis and fix suggestions.
 */
public enum ErrorCategory {
    /**
     * Configuration errors - missing properties, invalid config, profile issues
     */
    CONFIG,

    /**
     * Data-related errors - validation failures, parsing errors, DB constraints
     */
    DATA,

    /**
     * Infrastructure errors - network issues, timeouts, external service failures
     */
    INFRA,

    /**
     * Code logic errors - NPE, IndexOutOfBounds, arithmetic errors
     */
    CODE,

    /**
     * Unable to categorize the error
     */
    UNKNOWN
}
