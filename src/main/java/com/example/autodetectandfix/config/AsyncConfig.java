package com.example.autodetectandfix.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Configuration for asynchronous task execution.
 * Customizes the thread pool for @Async methods (primarily CodeAnalyzer).
 */
@Configuration
@ConditionalOnProperty(name = "app.analysis.async-analysis", havingValue = "true", matchIfMissing = true)
public class AsyncConfig implements AsyncConfigurer {

    private static final Logger logger = LoggerFactory.getLogger(AsyncConfig.class);

    @Override
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // Core pool size: number of threads to keep alive
        executor.setCorePoolSize(2);

        // Maximum pool size: maximum number of threads
        executor.setMaxPoolSize(5);

        // Queue capacity: number of tasks to queue before creating new threads
        executor.setQueueCapacity(100);

        // Thread name prefix for easier debugging
        executor.setThreadNamePrefix("error-analyzer-");

        // Allow core threads to timeout when idle
        executor.setAllowCoreThreadTimeOut(true);

        // Keep alive time for idle threads (60 seconds)
        executor.setKeepAliveSeconds(60);

        // Wait for tasks to complete on shutdown
        executor.setWaitForTasksToCompleteOnShutdown(true);

        // Maximum time to wait for tasks to complete (30 seconds)
        executor.setAwaitTerminationSeconds(30);

        executor.initialize();

        logger.info("Configured async executor: core={}, max={}, queue={}",
                executor.getCorePoolSize(),
                executor.getMaxPoolSize(),
                executor.getQueueCapacity());

        return executor;
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (throwable, method, params) -> {
            logger.error("Async method {} threw exception: {}",
                    method.getName(),
                    throwable.getMessage(),
                    throwable);
        };
    }
}
