package com.kc.autodetectandfix.logging;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Watches a log file for new content using file pointer tracking.
 * Only reads new lines since the last check.
 */
@Component
public class LogFileWatcher {

    private static final Logger logger = LoggerFactory.getLogger(LogFileWatcher.class);

    @Value("${app.log.file-path}")
    private String logFilePath;

    private final AtomicLong filePointer = new AtomicLong(0);

    /**
     * Initialize file pointer to current end of file on startup.
     * This ensures we only read NEW exceptions, not old ones from previous runs.
     */
    @PostConstruct
    public void initialize() {
        try {
            Path path = Paths.get(logFilePath);
            if (Files.exists(path)) {
                long currentSize = Files.size(path);
                filePointer.set(currentSize);
                logger.info("Initialized log file watcher at position {} (end of existing file). Will only monitor new exceptions.", currentSize);
            } else {
                logger.info("Log file does not exist yet. Will start monitoring from beginning when created.");
            }
        } catch (IOException e) {
            logger.warn("Could not initialize file pointer to end of file: {}. Starting from beginning.", e.getMessage());
        }
    }

    /**
     * Reads only new lines from the log file since the last read.
     *
     * @return Array of new log lines
     * @throws IOException if file reading fails
     */
    public String[] readNewLines() throws IOException {
        Path path = Paths.get(logFilePath);

        if (!Files.exists(path)) {
            logger.debug("Log file does not exist yet: {}", logFilePath);
            return new String[0];
        }

        try (RandomAccessFile raf = new RandomAccessFile(path.toFile(), "r")) {
            long currentLength = raf.length();
            long lastPosition = filePointer.get();

            // Check if file was rotated/truncated
            if (currentLength < lastPosition) {
                logger.info("Log file appears to have been rotated. Resetting pointer.");
                lastPosition = 0;
            }

            if (currentLength == lastPosition) {
                return new String[0];  // No new content
            }

            raf.seek(lastPosition);

            List<String> newLines = new ArrayList<>();
            String line;
            while ((line = raf.readLine()) != null) {
                newLines.add(line);
            }

            filePointer.set(raf.getFilePointer());

            return newLines.toArray(new String[0]);
        }
    }

    /**
     * Resets the file pointer to the beginning.
     */
    public void reset() {
        filePointer.set(0);
        logger.info("File pointer reset to beginning");
    }
}
