package com.kc.autodetectandfix.notification;

import com.kc.autodetectandfix.event.ErrorStoredEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Listens for ErrorStoredEvent and triggers email notifications.
 * Only active when EmailNotificationService is available.
 */
@Component
@ConditionalOnBean(EmailNotificationService.class)
public class ErrorNotificationListener {

    private static final Logger logger = LoggerFactory.getLogger(ErrorNotificationListener.class);

    private final EmailNotificationService emailService;

    public ErrorNotificationListener(EmailNotificationService emailService) {
        this.emailService = emailService;
    }

    /**
     * Handles ErrorStoredEvent by sending email notification.
     */
    @EventListener
    public void onErrorStored(ErrorStoredEvent event) {
        logger.debug("Received ErrorStoredEvent for error: {}", event.getError().getId());
        emailService.sendErrorNotification(event.getError());
    }
}
