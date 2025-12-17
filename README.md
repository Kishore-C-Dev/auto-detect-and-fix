# Auto-Detect-and-Fix

A Spring Boot 3.x application that monitors log files in real-time, automatically detects exceptions, classifies them into categories, and provides AI-powered fix suggestions using OpenAI GPT models and Git repository analysis. The system includes email notifications to keep teams informed about critical errors.

## Features

- **Real-time Log Monitoring**: Continuously monitors application log files for new exceptions
- **Automatic Classification**: Categorizes errors into CONFIG, DATA, INFRA, or CODE categories
- **AI-Powered Fix Suggestions**: Leverages OpenAI GPT models to provide context-aware, intelligent fix recommendations
- **Source Code Context**: Retrieves relevant source code snippets from Git repository
- **Email Notifications**: Sends email alerts to configured recipients when critical errors are detected
- **Deduplication**: Groups similar errors and tracks occurrence counts
- **REST API**: Full REST API for querying detected errors and statistics
- **Async Processing**: Non-blocking error analysis using Spring's async capabilities
- **OpenAI Integration**: Seamlessly integrates with OpenAI API for advanced code analysis and suggestions
- **Configurable**: All behaviors configurable via application.yml

## Technology Stack

- **Java**: 17 (LTS)
- **Framework**: Spring Boot 3.2.0
- **Build Tool**: Maven
- **AI/ML**: OpenAI GPT-4 API for intelligent code analysis
- **Email**: Spring Boot Starter Mail (SMTP)
- **Git Integration**: JGit 6.7.0
- **Storage**: In-memory (ConcurrentHashMap)
- **Logging**: Logback
- **HTTP Client**: Spring WebClient for OpenAI API integration

## Prerequisites

- Java 17 or higher
- Maven 3.6 or higher
- Git repository (for source code analysis features)
- OpenAI API Key (for AI-powered fix suggestions)
- SMTP server credentials (for email notifications)

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

### 3. Configure OpenAI and Email

Create a `.env` file or export environment variables:

```bash
# OpenAI API Key (get from https://platform.openai.com/api-keys)
export OPENAI_API_KEY="sk-your-api-key-here"

# Email Configuration (for Gmail)
export MAIL_USERNAME="your-email@gmail.com"
export MAIL_PASSWORD="your-gmail-app-password"
export EMAIL_TO="dev-team@example.com,oncall@example.com"

# Or use mock mode for testing
export OPENAI_MOCK_MODE=true
export EMAIL_MOCK_MODE=true
```

### 4. Run the Application

```bash
# With real OpenAI API
./run-with-real-api.sh

# Or with mock mode (no API costs)
./run-with-mock.sh

# Or manually
mvn spring-boot:run
```

The application will start on `http://localhost:8080`

**Note**: On first run with mock mode, you'll see AI-powered features using rule-based fallbacks. Set your OpenAI API key to unlock full AI capabilities.

### 5. Trigger Test Errors

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

### 6. View Detected Errors and AI Suggestions

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

The response will include:
- Error details with AI-generated category
- AI-generated fix suggestions with confidence level
- Original vs. fixed code snippets
- Source code context from Git
- Stack trace and occurrence count

**Check your email!** If email notifications are enabled, you'll receive a beautifully formatted HTML email with all error details and AI-powered fix suggestions.

## How It Works

### Error Detection Pipeline

```
1. Exception occurs → GlobalExceptionHandler logs to logs/app.log
2. LogFileMonitor polls log file every 1 second
3. ExceptionParser extracts exception details and stack trace
4. ErrorClassifier (uses OpenAI):
   - Sends error context to OpenAI API
   - AI analyzes exception type, message, stack trace, and source code
   - Categorizes error as CONFIG/DATA/INFRA/CODE
   - Falls back to rule-based classification if OpenAI unavailable
5. CodeAnalyzer (async):
   - Retrieves source code context from Git repository
   - Identifies application code in stack trace
6. FixSuggestionGenerator (uses OpenAI):
   - Sends complete error context and source code to OpenAI
   - AI generates specific fix suggestions with code examples
   - Provides confidence level (HIGH/MEDIUM/LOW)
   - Falls back to rule-based suggestions if OpenAI unavailable
   - Stores analyzed error in InMemoryErrorStorage
7. EmailNotificationService (async):
   - Sends HTML-formatted email alerts with beautiful templates
   - Includes error details, AI fix suggestions, and code comparison
   - Side-by-side view of problematic code vs. recommended fix
   - Only sends for new errors (deduplicates to avoid spam)
8. REST API provides access to detected errors and statistics
```

### Error Categories

- **CONFIG**: Configuration-related errors (missing properties, invalid config)
- **DATA**: Data validation or parsing errors
- **INFRA**: Infrastructure errors (IO, network, timeouts)
- **CODE**: Programming errors (NPE, array index, arithmetic)

### AI-Powered Fix Suggestions

The system leverages OpenAI GPT-4 to provide context-aware, intelligent fix suggestions:

- **Code Analysis**: Analyzes the error context, stack trace, and source code
- **Root Cause Identification**: Identifies the underlying cause of the exception
- **Actionable Recommendations**: Provides specific code changes and best practices
- **Context-Aware**: Considers your codebase structure and coding patterns
- **Multiple Solutions**: Often suggests multiple approaches to fix the issue
- **Prevention Tips**: Includes recommendations to prevent similar errors

Examples:
- **NullPointerException**: AI suggests null checks, Optional<T> usage, or refactoring patterns
- **ArrayIndexOutOfBoundsException**: Recommends bounds validation with code snippets
- **ArithmeticException**: Provides defensive programming patterns
- **Configuration errors**: Suggests configuration fixes with examples
- **Custom Exceptions**: Analyzes your domain-specific errors and provides tailored solutions

## OpenAI Integration Details

### How AI Analysis Works

The application integrates with OpenAI's GPT-4 model to provide intelligent, context-aware error analysis and fix suggestions:

1. **Error Context Collection**:
   - Exception type, message, and full stack trace
   - Source code from the affected file(s)
   - Git commit history for the affected code
   - Related configuration files

2. **AI Prompt Engineering**:
   - Structured prompt with error details
   - Source code context with line numbers
   - Request for root cause analysis
   - Request for specific fix recommendations
   - Ask for prevention strategies

3. **AI Response Processing**:
   - Parse AI response into structured format
   - Extract code snippets and explanations
   - Store suggestions with confidence scores
   - Cache responses to reduce API costs

### Supported OpenAI Models

The system is configured to use:
- **GPT-4o-mini** (Default): Fast, cost-effective, excellent for error analysis
- **GPT-4**: Most accurate, best for complex errors (configurable)
- **GPT-4-Turbo**: Faster, good for high-volume scenarios (configurable)
- **GPT-3.5-Turbo**: Budget option for simple errors (configurable)

You can change the model by setting the `OPENAI_MODEL` environment variable or updating `app.openai.model` in application.yml.

### Cost Optimization

The system includes several features to optimize OpenAI API usage and reduce costs:

- **Deduplication**: Similar errors are grouped and only analyzed once
- **Retry with Resilience4j**: Automatic retry for transient failures (max 3 retries)
- **Timeout Configuration**: Prevents long-running API calls (default 30 seconds)
- **Mock Mode**: Test the system without making actual API calls (`OPENAI_MOCK_MODE=true`)
- **Fallback Logic**: Uses rule-based suggestions if OpenAI is unavailable or disabled

### Example AI Interaction

**Input to OpenAI**:
```
You are a senior Java developer. Analyze this error and provide fix suggestions.

Error: NullPointerException
Location: UserService.java:45
Stack Trace: ...

Source Code:
43: public User getUser(String userId) {
44:     User user = userRepository.findById(userId);
45:     return user.getEmail();
46: }

Provide:
1. Root cause analysis
2. Specific code fix with explanation
3. Prevention strategies
```

**AI Response**:
```
Root Cause: The findById method returns null when user is not found,
causing NPE on line 45.

Fix:
public User getUser(String userId) {
    User user = userRepository.findById(userId);
    if (user == null) {
        throw new UserNotFoundException("User not found: " + userId);
    }
    return user.getEmail();
}

Prevention:
1. Use Optional<User> return type from repository
2. Add @NotNull annotations
3. Implement proper exception handling
```

## Email Notification System

### How Email Notifications Work

The application sends email alerts when critical errors are detected, keeping your team informed in real-time.

### Email Features

The system sends beautifully formatted HTML emails using Thymeleaf templates with the following features:

1. **Professional HTML Design**:
   - Color-coded error categories (CONFIG=Orange, DATA=Blue, INFRA=Purple, CODE=Red)
   - Responsive email layout that works on all devices
   - Syntax-highlighted code blocks
   - Side-by-side code comparison (problematic vs. fixed)

2. **Rich Email Content**:
   - Error ID, timestamp, exception type, and category
   - Full error message with formatting
   - AI-generated fix suggestions with confidence level
   - Complete source file path and line number
   - Side-by-side view: Current Code (with error marker) vs. AI-Recommended Fix
   - Top 10 stack trace frames
   - Professional styling with colors and icons

3. **Smart Sending**:
   - Asynchronous email sending (doesn't block error processing)
   - Deduplication: Only sends emails for NEW errors, not duplicates
   - Configurable subject prefix for easy filtering
   - Optional source code inclusion control

4. **Multiple Recipients**:
   - Comma-separated list of recipients
   - Single configuration for all team members
   - Mock mode for testing without sending actual emails

### Email Template Example

**Subject:** `[Error Alert] CODE - java.lang.NullPointerException`

The email contains a beautifully formatted HTML template with:

**Header Section** (Red background):
- Error Detected in Application
- Color-coded category badge (CODE = Red)

**Error Details Section**:
- Error ID: `abc-123-def-456`
- Exception Type: `java.lang.NullPointerException`
- Category: `CODE`
- Timestamp: `2025-12-16 10:30:00`
- Error Message (in highlighted box)

**Recommended Fix Section** (Green highlighted):
- AI-generated summary with confidence badge (HIGH/MEDIUM/LOW)
- Numbered list of specific fix steps
- Each step is actionable and code-specific

**Source File & Code Analysis Section**:
- File path with highlighted filename
- Error line number prominently displayed
- **Side-by-side code comparison**:
  - Left: Current Code From Your File (with ❌ marker on error line)
  - Right: Recommended Fixed Code (with ✅)

**Stack Trace Section**:
- Top 10 stack frames in monospace font
- Easy to read format

**Footer**:
- Error ID for reference
- "This is an automated notification from Auto-Detect-and-Fix System"

The HTML template (`error-notification.html`) uses professional styling with:
- Responsive grid layouts
- Color-coded badges
- Monospace fonts for code
- Highlighted error lines
- Clean, modern design

### Supported Email Providers

- **Gmail**: Use App Passwords for authentication
- **Outlook/Office 365**: Configure with OAuth or App Password
- **AWS SES**: For production deployments
- **SendGrid**: Enterprise email delivery
- **Custom SMTP**: Any standard SMTP server

### Gmail Configuration Example

1. **Enable 2-Step Verification** in your Google Account
2. **Generate App Password**:
   - Go to Google Account Settings
   - Security > 2-Step Verification > App passwords
   - Generate password for "Mail"
3. **Configure in application.yml**:
```yaml
spring:
  mail:
    host: smtp.gmail.com
    port: 587
    username: your-email@gmail.com
    password: your-16-char-app-password
```

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

  # OpenAI Configuration
  openai:
    enabled: ${OPENAI_ENABLED:true}                      # Enable/disable AI-powered suggestions
    api-key: ${OPENAI_API_KEY:}                         # OpenAI API key (required)
    api-url: ${OPENAI_API_URL:https://api.openai.com/v1/chat/completions}
    model: ${OPENAI_MODEL:gpt-4o-mini}                  # Model: gpt-4o-mini, gpt-4, gpt-3.5-turbo
    timeout-seconds: ${OPENAI_TIMEOUT:30}               # API request timeout
    max-retries: ${OPENAI_MAX_RETRIES:3}                # Max retry attempts for failed requests
    mock-mode: ${OPENAI_MOCK_MODE:false}                # Use mock responses (testing without API)

  # Email Notification Configuration
  notification:
    email:
      enabled: ${EMAIL_NOTIFICATION_ENABLED:true}       # Enable/disable email notifications
      from: ${EMAIL_FROM:auto-detect@example.com}       # Sender email address
      to: ${EMAIL_TO:admin@example.com}                 # Recipients (comma-separated)
      subject-prefix: ${EMAIL_SUBJECT_PREFIX:[Error Alert]}
      include-source-code: ${EMAIL_INCLUDE_SOURCE:true} # Include source code in emails
      mock-mode: ${EMAIL_MOCK_MODE:false}               # Log emails instead of sending

# Spring Mail Configuration
spring:
  mail:
    host: ${MAIL_HOST:smtp.gmail.com}                   # SMTP server host
    port: ${MAIL_PORT:587}                              # SMTP server port
    username: ${MAIL_USERNAME:}                         # Email username
    password: ${MAIL_PASSWORD:}                         # Email password or app password
    properties:
      mail:
        smtp:
          auth: true
          starttls:
            enable: true
            required: true
        debug: ${MAIL_DEBUG:false}                      # Enable SMTP debug logging
```

### Environment Variables

Set these environment variables before running the application:

```bash
# Required for OpenAI integration
export OPENAI_API_KEY="sk-your-openai-api-key-here"

# Required for email notifications
export MAIL_USERNAME="your-email@gmail.com"
export MAIL_PASSWORD="your-app-password"       # Gmail App Password

# Optional - customize recipients
export EMAIL_FROM="auto-detect@yourcompany.com"
export EMAIL_TO="dev-team@yourcompany.com,oncall@yourcompany.com"

# Optional - testing without API costs
export OPENAI_MOCK_MODE="true"   # Use rule-based suggestions instead of OpenAI
export EMAIL_MOCK_MODE="true"    # Log emails instead of sending
```

### Running with Mock Mode (No API Required)

For testing without OpenAI API costs or email setup:

```bash
# Use the included mock mode script
./run-with-mock.sh

# Or set environment variables manually
export OPENAI_MOCK_MODE=true
export EMAIL_MOCK_MODE=true
mvn spring-boot:run
```

### Running with Real OpenAI API

```bash
# Use the included real API script
./run-with-real-api.sh

# Or set your API key and run
export OPENAI_API_KEY="sk-your-key-here"
export MAIL_USERNAME="your-email@gmail.com"
export MAIL_PASSWORD="your-app-password"
mvn spring-boot:run
```

## Architecture

### Package Structure

```
com.kc.autodetectandfix/
├── api/                    # REST controllers
│   ├── ErrorMonitorController.java
│   ├── TestController.java
│   └── GlobalExceptionHandler.java
├── logging/                # Log file monitoring
│   ├── LogFileMonitor.java
│   └── LogFileWatcher.java
├── analysis/               # Exception parsing and classification
│   ├── ExceptionParser.java
│   ├── ErrorClassifier.java      (uses OpenAI for classification)
│   ├── CodeAnalyzer.java
│   └── FixSuggestionGenerator.java (uses OpenAI for fix suggestions)
├── ai/                     # OpenAI integration
│   ├── OpenAiService.java         (main service for AI calls)
│   └── dto/
│       ├── OpenAiRequest.java
│       └── OpenAiResponse.java
├── notification/           # Email notifications
│   └── EmailNotificationService.java (sends HTML emails via Thymeleaf)
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
│   ├── ErrorDetectedEvent.java
│   └── ErrorStoredEvent.java
├── config/                 # Configuration
│   ├── AsyncConfig.java
│   ├── GitConfig.java
│   ├── OpenAiConfig.java          (OpenAI settings)
│   ├── EmailConfig.java           (Email settings)
│   ├── WebClientConfig.java       (HTTP client for OpenAI)
│   └── RetryConfiguration.java    (Resilience4j retry)
└── resources/
    └── templates/
        └── error-notification.html (Thymeleaf email template)
```

### Key Components

- **LogFileMonitor**: Scheduled service that polls log file for new entries
- **ExceptionParser**: Parses log entries to extract exception details and stack traces
- **ErrorClassifier**: Categorizes errors using OpenAI API (falls back to rule-based)
- **OpenAiService**: Core service for OpenAI API integration
  - `classifyError()`: Uses AI to categorize errors
  - `generateFixSuggestion()`: Uses AI to generate fix recommendations
  - Includes retry logic with Resilience4j
  - Supports mock mode for testing
- **CodeAnalyzer**: Orchestrates error analysis asynchronously
  - Retrieves source code context from Git
  - Generates fix suggestions via OpenAI
  - Publishes events for email notifications
- **FixSuggestionGenerator**: Generates fix suggestions using OpenAI or rules
- **EmailNotificationService**: Sends HTML email alerts asynchronously
  - Uses Thymeleaf templates for beautiful formatting
  - Includes error details, AI suggestions, and code comparison
  - Deduplicates to prevent spam
- **GitRepositoryService**: Integrates with Git to retrieve source code context
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
    com.kc.autodetectandfix: DEBUG
```

## Current Capabilities

✅ **Fully Implemented Features**:
- OpenAI GPT-4o-mini integration for error classification and fix suggestions
- HTML email notifications with Thymeleaf templates
- Side-by-side code comparison in emails (current vs. recommended fix)
- Mock mode for testing without API costs
- Resilience4j retry logic for API failures
- Asynchronous processing for non-blocking error analysis
- Error deduplication to avoid duplicate emails
- Git integration for source code context retrieval

## Limitations

- **In-Memory Storage**: Errors are lost on application restart (database persistence planned)
- **Single Log File**: Currently monitors one log file at a time
- **Local Repository**: Only works with local Git repositories
- **OpenAI API Costs**: AI-powered suggestions incur API usage costs (use mock mode for testing)
- **Manual Jira Integration**: No automated Jira issue creation yet
- **No Web Dashboard**: Currently REST API only, no visual dashboard
- **Email Only**: No Slack or other notification channels yet

## How OpenAI Powers the System (Currently Implemented)

The application uses OpenAI GPT models in two critical places:

### 1. Error Classification (OpenAiService.classifyError)
- **Input**: Exception type, message, complete stack trace, and source code context
- **AI Task**: Analyze the error and categorize it as CONFIG, DATA, INFRA, or CODE
- **Output**: Single word category (e.g., "CODE")
- **Fallback**: Rule-based classification if API fails
- **Benefit**: More accurate categorization than simple pattern matching

### 2. Fix Suggestion Generation (OpenAiService.generateFixSuggestion)
- **Input**: Full error context including:
  - Exception details and "Caused by" chain
  - Complete stack trace with app code markers
  - Source code with line numbers
  - Git commit information
- **AI Task**: Generate specific, actionable fix recommendations
- **Output**: JSON with:
  - Summary of what needs to be fixed
  - Step-by-step fix instructions
  - Original problematic code snippet
  - Fixed code snippet with corrections
  - Confidence level (HIGH/MEDIUM/LOW)
- **Fallback**: Rule-based suggestions if API fails
- **Benefit**: Context-aware fixes tailored to your specific code

### Example OpenAI Prompt Structure

For error classification:
```
System: You are an expert error classifier...
Categories: CONFIG, DATA, INFRA, CODE, UNKNOWN

User:
=== EXCEPTION DETAILS ===
Type: java.lang.NullPointerException
Message: Cannot invoke "getEmail()" because "user" is null

=== STACK TRACE ===
>>> at com.kc.app.UserService.getUserEmail(UserService.java:45) [APP CODE]
    at com.kc.app.UserController.getUser(UserController.java:23) [APP CODE]
```

For fix suggestions:
```
System: You are a senior Java developer...
Respond with JSON: {summary, steps[], confidence, originalCode, fixedCode}

User:
=== EXCEPTION DETAILS ===
[error details]
=== SOURCE CODE ===
43: public User getUser(String userId) {
44:     User user = userRepository.findById(userId);
>>>  45:     return user.getEmail();
46: }
```

The AI responds with specific code changes, not generic advice.

---

## Future Implementations

These features are **NOT yet implemented** but are planned for future releases:

### 1. Automated Jira Issue Creation

**Description**: Automatically create Jira issues directly from the application dashboard when errors are detected.

**Features**:
- **Dashboard Integration**: Add "Create Jira Issue" button for each detected error
- **Auto-Population**: Pre-fill Jira issue with error details:
  - Issue summary: Error type and brief description
  - Description: Full stack trace, error category, and AI-suggested fixes
  - Priority: Auto-assigned based on error category (e.g., INFRA = High, CODE = Medium)
  - Labels: Auto-tagged with error category and component name
  - Attachments: Include source code context and log snippets
- **Jira REST API Integration**: Use Jira Cloud REST API for issue creation
- **Custom Fields**: Map application-specific data to Jira custom fields
- **Issue Linking**: Track relationship between detected errors and Jira issues
- **Duplicate Prevention**: Check for existing Jira issues before creating new ones

**Configuration**:
```yaml
app:
  jira:
    enabled: true
    base-url: https://your-company.atlassian.net
    api-token: ${JIRA_API_TOKEN}
    project-key: DEV
    issue-type: Bug
    default-assignee: auto-assign
```

**API Endpoints**:
- `POST /api/errors/{id}/create-jira-issue` - Create Jira issue for specific error
- `GET /api/errors/{id}/jira-link` - Get linked Jira issue details

---

### 2. Automated Git Branch Creation

**Description**: Automatically create Git branches with Jira issue number and description for fixing detected errors.

**Features**:
- **Branch Naming Convention**: `fix/JIRA-123-null-pointer-exception-in-user-service`
- **Auto-Checkout**: Create and checkout branch from specified base branch (main/develop)
- **Branch Metadata**: Include error ID and Jira issue number in branch description
- **Remote Push**: Optionally push branch to remote repository
- **Branch Protection**: Validate branch naming against team conventions

**Workflow**:
1. Error detected → Jira issue created (e.g., `DEV-456`)
2. User clicks "Create Fix Branch" in dashboard
3. System creates branch: `fix/DEV-456-arithmetic-exception-in-calculator`
4. Branch is checked out and ready for development

**Configuration**:
```yaml
app:
  git:
    auto-branch:
      enabled: true
      base-branch: main
      prefix: fix
      push-to-remote: true
      remote-name: origin
```

**API Endpoints**:
- `POST /api/errors/{id}/create-branch` - Create Git branch for error fix
- `GET /api/git/branches` - List all auto-created branches

---

### 3. AI-Assisted Code Changes

**Description**: Use OpenAI to generate actual code changes and apply them to the codebase automatically.

**Features**:
- **Code Diff Generation**: AI generates specific code changes based on error analysis
- **Multi-File Support**: Handle fixes that span multiple files
- **Test Generation**: Automatically generate unit tests for the fix
- **Code Review**: AI performs self-review of generated changes
- **Safety Checks**: Validate that changes don't introduce new issues

**Workflow**:
1. AI analyzes error and source code context
2. Generates specific code changes (add null checks, fix logic, etc.)
3. Creates unified diff format for review
4. Applies changes to working directory
5. Runs tests to validate fix

**Configuration**:
```yaml
app:
  openai:
    auto-fix:
      enabled: false              # Disabled by default for safety
      require-approval: true      # Require human approval before applying
      run-tests: true            # Run tests after applying changes
      max-files-changed: 5       # Safety limit on number of files
```

**API Endpoints**:
- `POST /api/errors/{id}/generate-fix` - Generate code changes using AI
- `POST /api/errors/{id}/apply-fix` - Apply generated changes to codebase
- `GET /api/errors/{id}/fix-preview` - Preview changes before applying

---

### 4. Automated Pull Request Creation (Human-in-the-Loop)

**Description**: Automatically create pull requests with AI-generated fixes for human review and approval.

**Features**:
- **PR Auto-Creation**: Create PR on GitHub/GitLab/Bitbucket
- **Rich PR Description**: Include:
  - Error summary and root cause analysis
  - AI-generated fix explanation
  - Code changes diff
  - Test results
  - Link to original Jira issue
  - Before/after code comparison
- **Human Review**: PR requires human approval before merging
- **CI/CD Integration**: Trigger automated tests and checks
- **Review Assignment**: Auto-assign reviewers based on code ownership
- **Draft Mode**: Create as draft PR for work-in-progress

**PR Template**:
```markdown
## Error Fix: [Error Type]

**Jira Issue**: [DEV-456](link)
**Error Category**: CODE
**Detected**: 2025-12-16 10:30:00

### Root Cause
[AI analysis of root cause]

### Changes Made
- Fixed NullPointerException in UserService.java:45
- Added null check before accessing user object
- Added unit test to prevent regression

### AI Suggestion
[Original AI suggestion with code snippets]

### Test Results
✅ All unit tests passing
✅ Integration tests passing
✅ Code coverage: 85% (+2%)

### Human Review Required
Please review the following:
- [ ] Verify null check logic is correct
- [ ] Ensure no edge cases are missed
- [ ] Validate test coverage is adequate
```

**Configuration**:
```yaml
app:
  pull-request:
    enabled: true
    provider: github              # github, gitlab, bitbucket
    base-branch: main
    draft-mode: true             # Create as draft PR
    auto-assign-reviewers: true
    reviewers:
      - senior-dev@example.com
      - tech-lead@example.com
    labels:
      - auto-fix
      - ai-generated
      - needs-review
```

**API Endpoints**:
- `POST /api/errors/{id}/create-pr` - Create pull request with fix
- `GET /api/pull-requests` - List all auto-created PRs
- `GET /api/pull-requests/{id}` - Get PR details and status

---

### 5. Complete Automated Workflow (End-to-End)

**Full Human-in-the-Loop Workflow**:

```
1. Error Detected
   └─> Log monitoring detects exception

2. AI Analysis
   └─> OpenAI analyzes error and generates fix suggestion

3. Email Notification
   └─> Team receives email alert with error details

4. Jira Issue Created (Manual or Auto)
   └─> Click "Create Jira Issue" in dashboard
   └─> Issue created: DEV-456

5. Git Branch Created
   └─> Click "Create Fix Branch"
   └─> Branch created: fix/DEV-456-null-pointer-exception
   └─> Branch pushed to remote

6. AI Generates Code Fix
   └─> Click "Generate Fix"
   └─> AI creates code changes
   └─> Preview diff shown in dashboard

7. Human Review & Approval
   └─> Developer reviews AI-generated changes
   └─> Can modify or approve as-is
   └─> Click "Apply Changes"

8. Changes Applied & Tested
   └─> Code changes applied to branch
   └─> Unit tests run automatically
   └─> Results displayed in dashboard

9. Pull Request Created
   └─> Click "Create PR"
   └─> PR created with full context
   └─> Reviewers auto-assigned

10. Human Review & Merge
    └─> Senior developer reviews PR
    └─> CI/CD runs all checks
    └─> PR approved and merged
    └─> Jira issue auto-closed
```

---

### 6. Additional Future Enhancements

- **Database Persistence**: PostgreSQL/MongoDB for error storage and history
- **Multiple Log Sources**: Support for multiple log files and formats
- **Distributed Log Aggregation**: Integration with ELK Stack, Splunk
- **Web UI Dashboard**: React-based dashboard for error visualization
- **Real-time WebSocket Updates**: Live error notifications in browser
- **Slack Integration**: Send notifications to Slack channels
- **Custom Classification Rules**: User-defined error categories via YAML
- **Error Analytics**: Trending, patterns, and insights dashboard
- **Rollback Automation**: Automatic rollback on deployment failures
- **A/B Testing for Fixes**: Test multiple AI-generated solutions
- **Cost Tracking**: Monitor OpenAI API usage and costs
- **Multi-Language Support**: Support for Python, Node.js, Go logs

---

## Summary: What's Working Today vs. Future Plans

### ✅ Currently Fully Functional

**AI-Powered Error Analysis:**
- OpenAI GPT-4o-mini integration for intelligent error classification
- AI-generated fix suggestions with code examples
- Context-aware analysis using source code and stack traces
- Fallback to rule-based analysis if API unavailable
- Mock mode for cost-free testing

**Email Notifications:**
- Beautiful HTML emails with Thymeleaf templates
- Side-by-side code comparison (current vs. recommended fix)
- Color-coded error categories
- Professional email design with syntax highlighting
- Asynchronous sending with deduplication

**Core Features:**
- Real-time log monitoring
- Git integration for source code context
- Async error processing
- REST API for all operations
- Resilience4j retry logic

### 🚧 Planned for Future (Not Yet Implemented)

- Automated Jira issue creation from dashboard
- Automated Git branch creation with Jira issue numbers
- AI-assisted code change generation and application
- Automated pull request creation with human-in-the-loop
- Web UI dashboard (currently REST API only)
- Database persistence (currently in-memory)
- Slack notifications
- Multiple log file support
- Advanced analytics and trending

The system is **production-ready** for error detection and AI-powered analysis with email notifications. Future enhancements will add automation for the full development workflow from error detection to PR creation.

---

## License

This project is for demonstration and educational purposes.

## Contributing

This is a proof-of-concept application. Contributions and suggestions are welcome!

## Contact

For questions or issues, please create an issue in the repository.
