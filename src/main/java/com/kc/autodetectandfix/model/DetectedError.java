package com.kc.autodetectandfix.model;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Represents a detected error from log files.
 *
 * Contains full details of the exception including stack trace,
 * classification, and fix suggestions.
 */
public class DetectedError {
    private String id;
    private String exceptionType;
    private String message;
    private ErrorCategory category;
    private List<StackTraceElement> stackTrace;
    private String rawLogEntry;
    private LocalDateTime detectedAt;
    private FixSuggestion fixSuggestion;
    private int occurrenceCount;

    public DetectedError() {
        this.id = UUID.randomUUID().toString();
        this.detectedAt = LocalDateTime.now();
        this.occurrenceCount = 1;
        this.category = ErrorCategory.UNKNOWN;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getExceptionType() {
        return exceptionType;
    }

    public void setExceptionType(String exceptionType) {
        this.exceptionType = exceptionType;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public ErrorCategory getCategory() {
        return category;
    }

    public void setCategory(ErrorCategory category) {
        this.category = category;
    }

    public List<StackTraceElement> getStackTrace() {
        return stackTrace;
    }

    public void setStackTrace(List<StackTraceElement> stackTrace) {
        this.stackTrace = stackTrace;
    }

    public String getRawLogEntry() {
        return rawLogEntry;
    }

    public void setRawLogEntry(String rawLogEntry) {
        this.rawLogEntry = rawLogEntry;
    }

    public LocalDateTime getDetectedAt() {
        return detectedAt;
    }

    public void setDetectedAt(LocalDateTime detectedAt) {
        this.detectedAt = detectedAt;
    }

    public FixSuggestion getFixSuggestion() {
        return fixSuggestion;
    }

    public void setFixSuggestion(FixSuggestion fixSuggestion) {
        this.fixSuggestion = fixSuggestion;
    }

    public int getOccurrenceCount() {
        return occurrenceCount;
    }

    public void setOccurrenceCount(int occurrenceCount) {
        this.occurrenceCount = occurrenceCount;
    }

    @Override
    public String toString() {
        return "DetectedError{" +
                "id='" + id + '\'' +
                ", exceptionType='" + exceptionType + '\'' +
                ", message='" + message + '\'' +
                ", category=" + category +
                ", detectedAt=" + detectedAt +
                ", occurrenceCount=" + occurrenceCount +
                '}';
    }
}
