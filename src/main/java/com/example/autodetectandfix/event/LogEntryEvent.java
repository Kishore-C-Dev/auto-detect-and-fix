package com.example.autodetectandfix.event;

import org.springframework.context.ApplicationEvent;

/**
 * Event published when a new log entry is detected in the log file.
 */
public class LogEntryEvent extends ApplicationEvent {

    private final String logLine;

    public LogEntryEvent(Object source, String logLine) {
        super(source);
        this.logLine = logLine;
    }

    public String getLogLine() {
        return logLine;
    }
}
