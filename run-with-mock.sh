#!/bin/bash
# Run application with mock mode enabled (no API keys needed)
export OPENAI_MOCK_MODE=true
export EMAIL_MOCK_MODE=true
export OPENAI_API_KEY=mock-key
export EMAIL_TO=test@example.com
export MAIL_USERNAME=test@example.com
export MAIL_PASSWORD=test

echo "Starting application in MOCK MODE..."
echo "- OpenAI: Mock responses (no API costs)"
echo "- Email: Log only (no emails sent)"
echo ""

mvn spring-boot:run
