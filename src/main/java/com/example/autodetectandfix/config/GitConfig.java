package com.example.autodetectandfix.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for Git integration.
 * Maps to app.git.* properties in application.yml.
 */
@Configuration
@ConfigurationProperties(prefix = "app.git")
public class GitConfig {

    /**
     * Path to the Git repository (default: current directory).
     */
    private String repositoryPath = ".";

    /**
     * Whether Git integration is enabled.
     */
    private boolean enabled = true;

    /**
     * Maximum depth to search for Git repository.
     */
    private int maxSearchDepth = 100;

    public String getRepositoryPath() {
        return repositoryPath;
    }

    public void setRepositoryPath(String repositoryPath) {
        this.repositoryPath = repositoryPath;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getMaxSearchDepth() {
        return maxSearchDepth;
    }

    public void setMaxSearchDepth(int maxSearchDepth) {
        this.maxSearchDepth = maxSearchDepth;
    }

    @Override
    public String toString() {
        return "GitConfig{" +
                "repositoryPath='" + repositoryPath + '\'' +
                ", enabled=" + enabled +
                ", maxSearchDepth=" + maxSearchDepth +
                '}';
    }
}
