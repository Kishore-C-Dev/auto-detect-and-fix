# Auto-Detect-and-Fix

A Spring Boot 3.x application that monitors log files in real-time, automatically detects exceptions, classifies them into categories, and provides intelligent fix suggestions using Git repository analysis.

## Features

- **Real-time Log Monitoring**: Continuously monitors application log files for new exceptions
- **Automatic Classification**: Categorizes errors into CONFIG, DATA, INFRA, or CODE categories
- **Intelligent Fix Suggestions**: Provides actionable fix recommendations based on error type
- **Source Code Context**: Retrieves relevant source code snippets from Git repository
- **Deduplication**: Groups similar errors and tracks occurrence counts
- **REST API**: Full REST API for querying detected errors and statistics
- **Async Processing**: Non-blocking error analysis using Spring's async capabilities
- **Configurable**: All behaviors configurable via application.yml

## Technology Stack

- **Java**: 17 (LTS)
- **Framework**: Spring Boot 3.2.0
- **Build Tool**: Maven
- **Git Integration**: JGit 6.7.0
- **Storage**: In-memory (ConcurrentHashMap)
- **Logging**: Logback

## Prerequisites

- Java 17 or higher
- Maven 3.6 or higher
- Git repository (for source code analysis features)

## Quick Start

### 1. Clone and Build

```bash
cd auto-detect-and-fix
mvn clean install
```

### 2. Initialize Git Repository

The application requires a Git repository for source code analysis:

```bash
git init
git add .
git commit -m "Initial commit"
```

### 3. Run the Application

```bash
mvn spring-boot:run
```

The application will start on `http://localhost:8080`

### 4. Trigger Test Errors

Use the test endpoints to generate sample errors:

```bash
# Trigger a NullPointerException
curl http://localhost:8080/api/test/null-pointer

# Trigger an ArithmeticException
curl http://localhost:8080/api/test/arithmetic

# Trigger an ArrayIndexOutOfBoundsException
curl http://localhost:8080/api/test/array-index

# Trigger a validation error
curl http://localhost:8080/api/test/validation

# Trigger an IO error
curl http://localhost:8080/api/test/io-error
```

### 5. View Detected Errors

Query the detected errors via the REST API:

```bash
# Get all errors
curl http://localhost:8080/api/errors

# Get specific error by ID
curl http://localhost:8080/api/errors/{error-id}

# Filter errors by category
curl "http://localhost:8080/api/errors?category=CODE"

# Get error statistics
curl http://localhost:8080/api/errors/stats

# Limit results
curl "http://localhost:8080/api/errors?limit=10"
```

## How It Works

### Error Detection Pipeline

```
1. Exception occurs → GlobalExceptionHandler logs to logs/app.log
2. LogFileMonitor polls log file every 1 second
3. ExceptionParser extracts exception details and stack trace
4. ErrorClassifier categorizes the error (CONFIG/DATA/INFRA/CODE)
5. CodeAnalyzer (async):
   - Retrieves source code context from Git
   - Generates fix suggestions
   - Stores in InMemoryErrorStorage
6. REST API provides access to detected errors
```

### Error Categories

- **CONFIG**: Configuration-related errors (missing properties, invalid config)
- **DATA**: Data validation or parsing errors
- **INFRA**: Infrastructure errors (IO, network, timeouts)
- **CODE**: Programming errors (NPE, array index, arithmetic)

### Fix Suggestions

The system provides intelligent fix suggestions based on error type:

- **NullPointerException**: Add null checks, use Optional<T>
- **ArrayIndexOutOfBoundsException**: Add bounds validation
- **ArithmeticException**: Validate denominators before division
- **Configuration errors**: Check application.yml, environment variables
- And more...

## REST API Endpoints

### Error Monitoring Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/errors` | Get all errors (with optional `limit` and `category` params) |
| GET | `/api/errors/{id}` | Get specific error by ID |
| GET | `/api/errors/stats` | Get error statistics |
| DELETE | `/api/errors/{id}` | Delete specific error |
| DELETE | `/api/errors` | Clear all errors |
| GET | `/api/errors/health` | Health check for error monitoring |

### Test Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/test/null-pointer` | Trigger NullPointerException |
| GET | `/api/test/arithmetic` | Trigger ArithmeticException |
| GET | `/api/test/array-index` | Trigger ArrayIndexOutOfBoundsException |
| GET | `/api/test/validation` | Trigger validation error |
| GET | `/api/test/io-error` | Trigger IO error |

## Configuration

Edit `src/main/resources/application.yml` to customize behavior:

```yaml
app:
  log:
    file-path: logs/app.log          # Log file to monitor
    monitor:
      enabled: true                   # Enable/disable monitoring
      poll-interval-ms: 1000          # Polling interval (1 second)
      batch-size: 100                 # Max lines to process per poll

  git:
    repository-path: .                # Git repository path
    enabled: true                     # Enable/disable Git integration
    max-search-depth: 100             # Max depth for file search

  analysis:
    max-stored-errors: 500            # Maximum errors to store
    error-retention-hours: 24         # How long to keep errors
    async-analysis: true              # Enable async analysis
```

## Architecture

### Package Structure

```
com.example.autodetectandfix/
├── api/                    # REST controllers
│   ├── ErrorMonitorController.java
│   ├── TestController.java
│   └── GlobalExceptionHandler.java
├── logging/                # Log file monitoring
│   ├── LogFileMonitor.java
│   └── LogFileWatcher.java
├── analysis/               # Exception parsing and classification
│   ├── ExceptionParser.java
│   ├── ErrorClassifier.java
│   ├── CodeAnalyzer.java
│   └── FixSuggestionGenerator.java
├── git/                    # Git integration
│   └── GitRepositoryService.java
├── model/                  # Domain objects
│   ├── DetectedError.java
│   ├── ErrorCategory.java
│   ├── FixSuggestion.java
│   ├── StackTraceElement.java
│   └── SourceCodeContext.java
├── storage/                # Error persistence
│   ├── ErrorStorage.java
│   └── InMemoryErrorStorage.java
├── event/                  # Spring events
│   ├── LogEntryEvent.java
│   └── ErrorDetectedEvent.java
└── config/                 # Configuration
    ├── AsyncConfig.java
    └── GitConfig.java
```

### Key Components

- **LogFileMonitor**: Scheduled service that polls log file for new entries
- **ExceptionParser**: Parses log entries to extract exception details
- **ErrorClassifier**: Categorizes errors using pattern matching
- **CodeAnalyzer**: Retrieves source context and generates fix suggestions (async)
- **GitRepositoryService**: Integrates with Git to retrieve source code
- **InMemoryErrorStorage**: Thread-safe storage with deduplication

## Monitoring

The application exposes Spring Boot Actuator endpoints:

```bash
# Health check
curl http://localhost:8080/actuator/health

# Metrics
curl http://localhost:8080/actuator/metrics

# View log file via actuator
curl http://localhost:8080/actuator/logfile
```

## Development

### Running Tests

```bash
mvn test
```

### Building for Production

```bash
mvn clean package
java -jar target/auto-detect-and-fix-0.0.1-SNAPSHOT.jar
```

### Enabling Debug Logging

Add to `application.yml`:

```yaml
logging:
  level:
    com.example.autodetectandfix: DEBUG
```

## Limitations

- **In-Memory Storage**: Errors are lost on application restart
- **Single Log File**: Only monitors one log file at a time
- **Heuristic Suggestions**: Fix suggestions are rule-based, not AI-powered
- **Local Repository**: Only works with local Git repositories

## Future Enhancements

- AI/LLM integration for smarter fix suggestions
- Database persistence (PostgreSQL, MongoDB)
- Multiple log source support
- Distributed log aggregation
- Slack/email notifications
- Web UI dashboard
- Real-time WebSocket updates
- Custom classification rules via YAML
- Error export (CSV, JSON)

## License

This project is for demonstration and educational purposes.

## Contributing

This is a proof-of-concept application. Contributions and suggestions are welcome!

## Contact

For questions or issues, please create an issue in the repository.
