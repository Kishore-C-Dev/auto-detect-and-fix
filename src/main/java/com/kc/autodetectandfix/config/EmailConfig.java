package com.kc.autodetectandfix.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Configuration
@ConfigurationProperties(prefix = "app.notification.email")
public class EmailConfig {

    private boolean enabled = true;
    private String from;
    private String to;
    private String subjectPrefix = "[Error Alert]";
    private boolean includeSourceCode = true;
    private boolean mockMode = false;

    public List<String> getToRecipients() {
        if (to == null || to.trim().isEmpty()) {
            return List.of();
        }
        return Arrays.stream(to.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    // Getters and setters
    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public String getSubjectPrefix() {
        return subjectPrefix;
    }

    public void setSubjectPrefix(String subjectPrefix) {
        this.subjectPrefix = subjectPrefix;
    }

    public boolean isIncludeSourceCode() {
        return includeSourceCode;
    }

    public void setIncludeSourceCode(boolean includeSourceCode) {
        this.includeSourceCode = includeSourceCode;
    }

    public boolean isMockMode() {
        return mockMode;
    }

    public void setMockMode(boolean mockMode) {
        this.mockMode = mockMode;
    }
}
