package com.kc.autodetectandfix.analysis;

import com.kc.autodetectandfix.event.ErrorDetectedEvent;
import com.kc.autodetectandfix.event.LogEntryEvent;
import com.kc.autodetectandfix.model.DetectedError;
import com.kc.autodetectandfix.model.StackTraceElement;
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

    // Pattern to detect exception in log: "2024-01-01 12:00:00.000 ... ERROR ... ExceptionType: message"
    // This pattern requires a timestamp at the beginning to avoid matching duplicate exception lines
    private static final Pattern EXCEPTION_PATTERN =
        Pattern.compile("^\\d{4}-\\d{2}-\\d{2}\\s+\\d{2}:\\d{2}:\\d{2}\\.\\d+.*ERROR.*?([a-zA-Z0-9.]+(?:Exception|Error)):\\s*(.*)");

    // Pattern for stack trace line: "at package.Class.method(File.java:123)"
    private static final Pattern STACK_TRACE_PATTERN =
        Pattern.compile("\\s*at\\s+([a-zA-Z0-9.$_]+)\\.([a-zA-Z0-9_<>]+)\\(([a-zA-Z0-9._]+):([0-9]+)\\)");

    // Pattern for bare exception line (Logback repeats exception type before stack trace)
    // Matches lines like: "java.lang.NullPointerException: Cannot invoke method"
    private static final Pattern BARE_EXCEPTION_PATTERN =
        Pattern.compile("^([a-zA-Z0-9.]+(?:Exception|Error)):\\s*(.*)");

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
                processBufferedException();
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
                logLine.trim().startsWith("Suppressed:") ||
                BARE_EXCEPTION_PATTERN.matcher(logLine).matches()) {

                currentExceptionBuffer.append(logLine).append("\n");
            } else {
                // End of stack trace
                processBufferedException();
                parsingException = false;
            }
        }
    }

    /**
     * Processes the buffered exception and publishes an event.
     */
    private void processBufferedException() {
        String rawException = currentExceptionBuffer.toString();

        DetectedError error = parseException(rawException);
        if (error != null) {
            // Filter out errors from external libraries (no application code in stack trace)
            boolean hasStackTrace = error.getStackTrace() != null && !error.getStackTrace().isEmpty();
            boolean hasApplicationCode = hasStackTrace &&
                error.getStackTrace().stream().anyMatch(StackTraceElement::isApplicationCode);

            // Skip errors with no stack trace or only library code
            if (!hasApplicationCode) {
                logger.debug("Ignoring library/framework error (no application code): {} - {}",
                    error.getExceptionType(), error.getMessage());
                return;
            }

            logger.info("Detected error: {} - {}", error.getExceptionType(), error.getMessage());

            // Print the full stack trace for debugging
            logger.info("Full Stack Trace:");
            if (error.getStackTrace() != null && !error.getStackTrace().isEmpty()) {
                error.getStackTrace().forEach(frame -> {
                    String marker = frame.isApplicationCode() ? " [APP CODE]" : "";
                    logger.info("  at {}.{}({}:{}){}",
                        frame.getClassName(),
                        frame.getMethodName(),
                        frame.getFileName(),
                        frame.getLineNumber(),
                        marker);
                });
            } else {
                logger.warn("No stack trace elements parsed for this error");
            }

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
        int lineIndex = 0;
        for (String line : lines) {
            lineIndex++;
            Matcher stackMatcher = STACK_TRACE_PATTERN.matcher(line);
            if (stackMatcher.find()) {
                StackTraceElement element = new StackTraceElement();
                element.setClassName(stackMatcher.group(1));
                element.setMethodName(stackMatcher.group(2));
                element.setFileName(stackMatcher.group(3));
                element.setLineNumber(Integer.parseInt(stackMatcher.group(4)));

                // Mark as application code if it matches our package
                boolean isAppCode = element.getClassName().startsWith("com.kc.autodetectandfix");
                element.setApplicationCode(isAppCode);

                // Debug log to see what's being parsed
                logger.debug("Parsed stack frame {}: {} [isAppCode={}]",
                    lineIndex, element.getClassName(), isAppCode);

                stackTrace.add(element);
            }
        }

        error.setStackTrace(stackTrace);
        logger.debug("Total stack frames parsed: {} (app frames: {})",
            stackTrace.size(),
            stackTrace.stream().filter(StackTraceElement::isApplicationCode).count());

        return error;
    }
}
