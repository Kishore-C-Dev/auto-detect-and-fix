package com.example.autodetectandfix.logging;

import com.example.autodetectandfix.event.LogEntryEvent;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;

/**
 * Scheduled service that monitors the log file for new entries.
 * Publishes LogEntryEvent for each new log line detected.
 */
@Service
@ConditionalOnProperty(name = "app.log.monitor.enabled", havingValue = "true", matchIfMissing = true)
public class LogFileMonitor {

    private static final Logger logger = LoggerFactory.getLogger(LogFileMonitor.class);

    private final LogFileWatcher logFileWatcher;
    private final ApplicationEventPublisher eventPublisher;

    @Value("${app.log.file-path}")
    private String logFilePath;

    private volatile boolean monitoring = false;

    public LogFileMonitor(LogFileWatcher logFileWatcher,
                         ApplicationEventPublisher eventPublisher) {
        this.logFileWatcher = logFileWatcher;
        this.eventPublisher = eventPublisher;
    }

    @PostConstruct
    public void startMonitoring() {
        monitoring = true;
        logger.info("Started monitoring log file: {}", logFilePath);
    }

    @PreDestroy
    public void stopMonitoring() {
        monitoring = false;
        logger.info("Stopped monitoring log file");
    }

    /**
     * Polls the log file for new entries at configured intervals.
     */
    @Scheduled(fixedDelayString = "${app.log.monitor.poll-interval-ms}")
    public void pollLogFile() {
        if (!monitoring) {
            return;
        }

        try {
            String[] newLines = logFileWatcher.readNewLines();

            for (String line : newLines) {
                if (line != null && !line.trim().isEmpty()) {
                    // Publish event for each new log line
                    eventPublisher.publishEvent(new LogEntryEvent(this, line));
                }
            }

            if (newLines.length > 0) {
                logger.debug("Processed {} new log lines", newLines.length);
            }

        } catch (IOException e) {
            logger.error("Error reading log file", e);
        }
    }
}
