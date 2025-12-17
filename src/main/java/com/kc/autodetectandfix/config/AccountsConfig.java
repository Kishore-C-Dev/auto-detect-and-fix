package com.kc.autodetectandfix.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for Accounts API.
 */
@Configuration
@ConfigurationProperties(prefix = "app.accounts")
public class AccountsConfig {

    private String mockApiUrl = "http://localhost:8080/api/mock/accounts";
    private int timeoutSeconds = 10;
    private boolean mockMode = true;

    public String getMockApiUrl() {
        return mockApiUrl;
    }

    public void setMockApiUrl(String mockApiUrl) {
        this.mockApiUrl = mockApiUrl;
    }

    public int getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public void setTimeoutSeconds(int timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
    }

    public boolean isMockMode() {
        return mockMode;
    }

    public void setMockMode(boolean mockMode) {
        this.mockMode = mockMode;
    }
}
