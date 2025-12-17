package com.kc.autodetectandfix;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Main Spring Boot application class for Auto Detect and Fix system.
 *
 * This application monitors log files in real-time, detects exceptions,
 * classifies them into categories (CONFIG, DATA, INFRA, CODE), and provides
 * intelligent fix suggestions using Git repository analysis.
 *
 * @EnableAsync enables asynchronous method execution for code analysis
 * @EnableScheduling enables scheduled tasks for log file monitoring
 */
@SpringBootApplication
@EnableAsync
@EnableScheduling
public class AutoDetectAndFixApplication {

    public static void main(String[] args) {
        SpringApplication.run(AutoDetectAndFixApplication.class, args);
    }
}
