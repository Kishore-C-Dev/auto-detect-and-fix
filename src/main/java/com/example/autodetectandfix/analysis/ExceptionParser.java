package com.example.autodetectandfix.analysis;

import com.example.autodetectandfix.event.ErrorDetectedEvent;
import com.example.autodetectandfix.event.LogEntryEvent;
import com.example.autodetectandfix.model.DetectedError;
import com.example.autodetectandfix.model.StackTraceElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses exception stack traces from log entries.
 * Buffers multi-line stack traces and publishes ErrorDetectedEvent when complete.
 */
@Component
public class ExceptionParser {

    private static final Logger logger = LoggerFactory.getLogger(ExceptionParser.class);

    // Pattern to detect exception in log: "ERROR ... ExceptionType: message"
    private static final Pattern EXCEPTION_PATTERN =
        Pattern.compile("ERROR.*?([a-zA-Z0-9.]+(?:Exception|Error)):\\s*(.*)");

    // Pattern for stack trace line: "at package.Class.method(File.java:123)"
    private static final Pattern STACK_TRACE_PATTERN =
        Pattern.compile("\\s*at\\s+([a-zA-Z0-9.$_]+)\\.([a-zA-Z0-9_<>]+)\\(([a-zA-Z0-9._]+):([0-9]+)\\)");

    private final ApplicationEventPublisher eventPublisher;

    private StringBuilder currentExceptionBuffer = new StringBuilder();
    private boolean parsingException = false;

    public ExceptionParser(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    /**
     * Listens for new log entries and parses exceptions.
     */
    @EventListener
    public void onLogEntry(LogEntryEvent event) {
        String logLine = event.getLogLine();

        Matcher exceptionMatcher = EXCEPTION_PATTERN.matcher(logLine);

        if (exceptionMatcher.find()) {
            // Start of new exception
            if (parsingException) {
                processBufferedExcept ion();
            }

            parsingException = true;
            currentExceptionBuffer = new StringBuilder();
            currentExceptionBuffer.append(logLine).append("\n");

        } else if (parsingException) {
            // Check if this is a stack trace line or continuation
            if (STACK_TRACE_PATTERN.matcher(logLine).find() ||
                logLine.trim().startsWith("at ") ||
                logLine.contains("Caused by:") ||
                logLine.contains("... ") ||
                logLine.trim().startsWith("Suppressed:")) {

                currentExceptionBuffer.append(logLine).append("\n");
            } else {
                // End of stack trace
                processBufferedExcept ion();
                parsingException = false;
            }
        }
    }

    /**
     * Processes the buffered exception and publishes an event.
     */
    private void processBufferedExcept ion() {
        String rawException = currentExceptionBuffer.toString();

        DetectedError error = parseException(rawException);
        if (error != null) {
            logger.info("Detected error: {} - {}", error.getExceptionType(), error.getMessage());
            eventPublisher.publishEvent(new ErrorDetectedEvent(this, error));
        }
    }

    /**
     * Parses a raw exception string into a DetectedError object.
     *
     * @param rawException The raw exception log entry
     * @return DetectedError object or null if parsing fails
     */
    public DetectedError parseException(String rawException) {
        if (rawException == null || rawException.trim().isEmpty()) {
            return null;
        }

        DetectedError error = new DetectedError();
        error.setRawLogEntry(rawException);

        String[] lines = rawException.split("\n");

        // Parse first line for exception type and message
        Matcher exceptionMatcher = EXCEPTION_PATTERN.matcher(lines[0]);
        if (exceptionMatcher.find()) {
            error.setExceptionType(exceptionMatcher.group(1));
            error.setMessage(exceptionMatcher.group(2));
        } else {
            logger.warn("Could not parse exception type from: {}", lines[0]);
            return null;
        }

        // Parse stack trace
        List<StackTraceElement> stackTrace = new ArrayList<>();
        for (String line : lines) {
            Matcher stackMatcher = STACK_TRACE_PATTERN.matcher(line);
            if (stackMatcher.find()) {
                StackTraceElement element = new StackTraceElement();
                element.setClassName(stackMatcher.group(1));
                element.setMethodName(stackMatcher.group(2));
                element.setFileName(stackMatcher.group(3));
                element.setLineNumber(Integer.parseInt(stackMatcher.group(4)));

                // Mark as application code if it matches our package
                element.setApplicationCode(
                    element.getClassName().startsWith("com.example.autodetectandfix")
                );

                stackTrace.add(element);
            }
        }

        error.setStackTrace(stackTrace);

        return error;
    }
}
