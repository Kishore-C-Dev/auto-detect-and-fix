package com.kc.autodetectandfix.notification;

import com.kc.autodetectandfix.config.EmailConfig;
import com.kc.autodetectandfix.model.DetectedError;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

/**
 * Service for sending email notifications when errors are detected.
 * Sends HTML-formatted emails using Thymeleaf templates.
 */
@Service
@ConditionalOnProperty(name = "app.notification.email.enabled", havingValue = "true", matchIfMissing = false)
public class EmailNotificationService {

    private static final Logger logger = LoggerFactory.getLogger(EmailNotificationService.class);

    private final JavaMailSender mailSender;
    private final EmailConfig emailConfig;
    private final TemplateEngine templateEngine;

    public EmailNotificationService(JavaMailSender mailSender, EmailConfig emailConfig, TemplateEngine templateEngine) {
        this.mailSender = mailSender;
        this.emailConfig = emailConfig;
        this.templateEngine = templateEngine;
    }

    /**
     * Sends an error notification email asynchronously.
     * Failures do not throw exceptions to prevent breaking error processing.
     *
     * @param error The detected error to notify about
     */
    @Async
    public void sendErrorNotification(DetectedError error) {
        if (emailConfig.isMockMode()) {
            logger.info("MOCK MODE: Would send email for error {} ({})",
                error.getId(), error.getExceptionType());
            logger.info("MOCK MODE: Recipients: {}", emailConfig.getToRecipients());
            logger.info("MOCK MODE: Subject: {} {} - {}",
                emailConfig.getSubjectPrefix(), error.getCategory(), error.getExceptionType());
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            // Set email headers
            helper.setFrom(emailConfig.getFrom());
            helper.setTo(emailConfig.getToRecipients().toArray(new String[0]));
            helper.setSubject(buildSubject(error));

            // Generate HTML content from template
            String htmlContent = generateEmailContent(error);
            helper.setText(htmlContent, true);

            // Send email
            mailSender.send(message);

            logger.info("Sent email notification for error: {} ({})",
                error.getId(), error.getExceptionType());

        } catch (MessagingException e) {
            logger.error("Failed to create email message for error {}: {}",
                error.getId(), e.getMessage());
            // Don't throw - email failure shouldn't break error processing

        } catch (Exception e) {
            logger.error("Failed to send email for error {}: {}",
                error.getId(), e.getMessage(), e);
            // Don't throw - email failure shouldn't break error processing
        }
    }

    /**
     * Builds the email subject line.
     */
    private String buildSubject(DetectedError error) {
        return String.format("%s %s - %s",
            emailConfig.getSubjectPrefix(),
            error.getCategory(),
            error.getExceptionType());
    }

    /**
     * Generates HTML email content using Thymeleaf template.
     */
    private String generateEmailContent(DetectedError error) {
        Context context = new Context();
        context.setVariable("error", error);
        context.setVariable("includeSourceCode", emailConfig.isIncludeSourceCode());

        return templateEngine.process("error-notification", context);
    }
}
