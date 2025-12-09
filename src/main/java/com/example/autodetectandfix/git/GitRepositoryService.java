package com.example.autodetectandfix.git;

import com.example.autodetectandfix.model.SourceCodeContext;
import jakarta.annotation.PostConstruct;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Service for interacting with Git repository using JGit.
 * Retrieves source code context for error analysis.
 */
@Service
@ConditionalOnProperty(name = "app.git.enabled", havingValue = "true", matchIfMissing = true)
public class GitRepositoryService {

    private static final Logger logger = LoggerFactory.getLogger(GitRepositoryService.class);

    @Value("${app.git.repository-path}")
    private String repositoryPath;

    private Repository repository;
    private Git git;

    @PostConstruct
    public void initialize() throws IOException {
        FileRepositoryBuilder builder = new FileRepositoryBuilder();

        File gitDir = new File(repositoryPath, ".git");

        if (!gitDir.exists()) {
            logger.warn("Git repository not found at: {}. Git features will be limited.", repositoryPath);
            return;
        }

        repository = builder.setGitDir(gitDir)
            .readEnvironment()
            .findGitDir()
            .build();

        git = new Git(repository);

        logger.info("Initialized Git repository at: {}", repositoryPath);
    }

    /**
     * Gets source code context for an error location.
     *
     * @param className  The class name where the error occurred
     * @param lineNumber The line number of the error
     * @return Optional containing SourceCodeContext if found
     */
    public Optional<SourceCodeContext> getSourceContext(String className, int lineNumber) {
        if (git == null) {
            logger.debug("Git not initialized, cannot retrieve source context");
            return Optional.empty();
        }

        try {
            // Convert class name to file path
            String filePath = classNameToFilePath(className);

            Path fullPath = Paths.get(repositoryPath, filePath);

            if (!Files.exists(fullPath)) {
                logger.warn("Source file not found: {}", fullPath);
                return Optional.empty();
            }

            SourceCodeContext context = new SourceCodeContext();
            context.setFilePath(filePath);
            context.setErrorLineNumber(lineNumber);

            // Read surrounding lines (5 before and after)
            List<String> surroundingLines = readSurroundingLines(fullPath, lineNumber, 5);
            context.setSurroundingLines(surroundingLines);

            // Get last commit info
            String commitHash = getLastCommitHash(filePath);
            context.setCommitHash(commitHash);

            String lastModifiedBy = getLastModifiedBy(filePath);
            context.setLastModifiedBy(lastModifiedBy);

            logger.debug("Retrieved source context for {}:{}", className, lineNumber);
            return Optional.of(context);

        } catch (Exception e) {
            logger.error("Error getting source context for " + className, e);
            return Optional.empty();
        }
    }

    /**
     * Converts Java class name to file path.
     */
    private String classNameToFilePath(String className) {
        // Convert com.example.MyClass to src/main/java/com/example/MyClass.java
        // Remove any inner class names (after $)
        String baseName = className.contains("$") ? className.substring(0, className.indexOf("$")) : className;
        String path = baseName.replace('.', '/');
        return "src/main/java/" + path + ".java";
    }

    /**
     * Reads lines surrounding the error line.
     */
    private List<String> readSurroundingLines(Path filePath, int targetLine, int context)
            throws IOException {

        List<String> allLines = Files.readAllLines(filePath);
        List<String> result = new ArrayList<>();

        int start = Math.max(0, targetLine - context - 1);
        int end = Math.min(allLines.size(), targetLine + context);

        for (int i = start; i < end; i++) {
            String marker = (i == targetLine - 1) ? ">>> " : "    ";
            result.add(String.format("%s%4d: %s", marker, i + 1, allLines.get(i)));
        }

        return result;
    }

    /**
     * Gets the last commit hash that modified the file.
     */
    private String getLastCommitHash(String filePath) {
        try {
            Iterable<RevCommit> commits = git.log()
                .addPath(filePath)
                .setMaxCount(1)
                .call();

            for (RevCommit commit : commits) {
                return commit.getName().substring(0, 8);  // Short hash
            }
        } catch (GitAPIException e) {
            logger.error("Error getting commit hash", e);
        }
        return "unknown";
    }

    /**
     * Gets the author of the last commit that modified the file.
     */
    private String getLastModifiedBy(String filePath) {
        try {
            Iterable<RevCommit> commits = git.log()
                .addPath(filePath)
                .setMaxCount(1)
                .call();

            for (RevCommit commit : commits) {
                return commit.getAuthorIdent().getName();
            }
        } catch (GitAPIException e) {
            logger.error("Error getting author", e);
        }
        return "unknown";
    }
}
