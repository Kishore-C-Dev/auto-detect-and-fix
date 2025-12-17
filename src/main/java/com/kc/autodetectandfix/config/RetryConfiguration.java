package com.kc.autodetectandfix.config;

import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;
import java.util.concurrent.TimeoutException;

@Configuration
public class RetryConfiguration {

    @Bean
    public Retry openAiRetry(OpenAiConfig config) {
        RetryConfig retryConfig = RetryConfig.custom()
                .maxAttempts(config.getMaxRetries())
                .waitDuration(Duration.ofSeconds(2))
                .retryExceptions(WebClientRequestException.class, TimeoutException.class)
                .retryOnException(e -> {
                    if (e instanceof WebClientResponseException) {
                        WebClientResponseException ex = (WebClientResponseException) e;
                        return ex.getStatusCode().is5xxServerError() || ex.getStatusCode().value() == 429;
                    }
                    return false;
                })
                .build();

        return Retry.of("openai", retryConfig);
    }
}
