package com.example.autodetectandfix.event;

import com.example.autodetectandfix.model.DetectedError;
import org.springframework.context.ApplicationEvent;

/**
 * Event published when an error is detected and parsed from log entries.
 */
public class ErrorDetectedEvent extends ApplicationEvent {

    private final DetectedError error;

    public ErrorDetectedEvent(Object source, DetectedError error) {
        super(source);
        this.error = error;
    }

    public DetectedError getError() {
        return error;
    }
}
