package com.kc.autodetectandfix.model;

import java.util.List;

/**
 * Represents a suggested fix for a detected error.
 */
public class FixSuggestion {
    private String summary;
    private List<String> steps;
    private SourceCodeContext sourceContext;
    private String confidence;  // HIGH, MEDIUM, LOW
    private String originalCode;  // The problematic code snippet
    private String fixedCode;     // The corrected code snippet

    public FixSuggestion() {
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public List<String> getSteps() {
        return steps;
    }

    public void setSteps(List<String> steps) {
        this.steps = steps;
    }

    public SourceCodeContext getSourceContext() {
        return sourceContext;
    }

    public void setSourceContext(SourceCodeContext sourceContext) {
        this.sourceContext = sourceContext;
    }

    public String getConfidence() {
        return confidence;
    }

    public void setConfidence(String confidence) {
        this.confidence = confidence;
    }

    public String getOriginalCode() {
        return originalCode;
    }

    public void setOriginalCode(String originalCode) {
        this.originalCode = originalCode;
    }

    public String getFixedCode() {
        return fixedCode;
    }

    public void setFixedCode(String fixedCode) {
        this.fixedCode = fixedCode;
    }

    @Override
    public String toString() {
        return "FixSuggestion{" +
                "summary='" + summary + '\'' +
                ", confidence='" + confidence + '\'' +
                ", steps=" + steps +
                '}';
    }
}
