package com.kc.autodetectandfix.event;

import com.kc.autodetectandfix.model.DetectedError;
import org.springframework.context.ApplicationEvent;

/**
 * Event published when an error has been fully analyzed and stored.
 * This triggers downstream actions like email notifications.
 */
public class ErrorStoredEvent extends ApplicationEvent {

    private final DetectedError error;

    public ErrorStoredEvent(Object source, DetectedError error) {
        super(source);
        this.error = error;
    }

    public DetectedError getError() {
        return error;
    }
}
