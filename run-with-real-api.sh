#!/bin/bash
# Run application with real OpenAI API and email
# IMPORTANT: Set your API keys before running!

# Check if .env file exists
if [ -f .env ]; then
    echo "Loading configuration from .env file..."
    set -a  # Automatically export all variables
    source .env
    set +a
else
    echo "WARNING: .env file not found!"
    echo "Please create .env file with your API keys:"
    echo "  cp .env.example .env"
    echo "  # Edit .env with your actual keys"
    exit 1
fi

# Validate required keys
if [ -z "$OPENAI_API_KEY" ] || [ "$OPENAI_API_KEY" = "sk-your-api-key-here" ]; then
    echo "ERROR: Please set OPENAI_API_KEY in .env file"
    exit 1
fi

# Show loaded configuration (masked)
echo "Loaded Configuration:"
echo "  OPENAI_ENABLED: $OPENAI_ENABLED"
echo "  OPENAI_MOCK_MODE: $OPENAI_MOCK_MODE"
echo "  OPENAI_API_KEY: ${OPENAI_API_KEY:0:20}... (${#OPENAI_API_KEY} chars)"
echo "  EMAIL_MOCK_MODE: $EMAIL_MOCK_MODE"
echo ""

echo "Starting application in PRODUCTION MODE..."
echo "- OpenAI: Real API calls (costs apply)"
echo "- Email: $([ "$EMAIL_MOCK_MODE" = "true" ] && echo "Mock mode (no emails)" || echo "Real SMTP (emails will be sent)")"
echo ""
echo "Press Ctrl+C to stop"
echo ""

mvn spring-boot:run
