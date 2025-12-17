package com.kc.autodetectandfix.model;

import java.util.List;

/**
 * Represents the source code context around an error location.
 */
public class SourceCodeContext {
    private String filePath;
    private int errorLineNumber;
    private List<String> surroundingLines;  // Lines around the error
    private String commitHash;
    private String lastModifiedBy;

    public SourceCodeContext() {
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public int getErrorLineNumber() {
        return errorLineNumber;
    }

    public void setErrorLineNumber(int errorLineNumber) {
        this.errorLineNumber = errorLineNumber;
    }

    public List<String> getSurroundingLines() {
        return surroundingLines;
    }

    public void setSurroundingLines(List<String> surroundingLines) {
        this.surroundingLines = surroundingLines;
    }

    public String getCommitHash() {
        return commitHash;
    }

    public void setCommitHash(String commitHash) {
        this.commitHash = commitHash;
    }

    public String getLastModifiedBy() {
        return lastModifiedBy;
    }

    public void setLastModifiedBy(String lastModifiedBy) {
        this.lastModifiedBy = lastModifiedBy;
    }

    @Override
    public String toString() {
        return "SourceCodeContext{" +
                "filePath='" + filePath + '\'' +
                ", errorLineNumber=" + errorLineNumber +
                ", commitHash='" + commitHash + '\'' +
                ", lastModifiedBy='" + lastModifiedBy + '\'' +
                '}';
    }
}
