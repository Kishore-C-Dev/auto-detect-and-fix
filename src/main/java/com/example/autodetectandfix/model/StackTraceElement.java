package com.example.autodetectandfix.model;

/**
 * Represents a single element in a stack trace.
 */
public class StackTraceElement {
    private String className;
    private String methodName;
    private String fileName;
    private int lineNumber;
    private boolean isApplicationCode;  // true if from our package

    public StackTraceElement() {
    }

    public StackTraceElement(String className, String methodName, String fileName, int lineNumber) {
        this.className = className;
        this.methodName = methodName;
        this.fileName = fileName;
        this.lineNumber = lineNumber;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public void setLineNumber(int lineNumber) {
        this.lineNumber = lineNumber;
    }

    public boolean isApplicationCode() {
        return isApplicationCode;
    }

    public void setApplicationCode(boolean applicationCode) {
        isApplicationCode = applicationCode;
    }

    @Override
    public String toString() {
        return "StackTraceElement{" +
                "className='" + className + '\'' +
                ", methodName='" + methodName + '\'' +
                ", fileName='" + fileName + '\'' +
                ", lineNumber=" + lineNumber +
                ", isApplicationCode=" + isApplicationCode +
                '}';
    }
}
