package com.myschool.userservice.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final JavaMailSender mailSender;
    private final SpringTemplateEngine templateEngine;

    public void sendWelcomeEmail(String to, String studentName) {
        try {
            Context ctx = new Context();
            ctx.setVariable("studentName", studentName);
            String html = templateEngine.process("welcome-email", ctx);
            sendHtmlEmail(to, "Welcome to MySchool 🎓", html);
            log.info("Welcome email sent to {}", to);
        } catch (Exception e) {
            log.error("Failed to send welcome email to {}", to, e);
        }
    }

    public void sendEnrollmentConfirmation(String to, String studentName,
                                           String courseTitle, String courseDate) {
        try {
            Context ctx = new Context();
            ctx.setVariable("studentName", studentName);
            ctx.setVariable("courseTitle", courseTitle);
            ctx.setVariable("courseDate", courseDate);
            String html = templateEngine.process("enrollment-confirmation", ctx);
            sendHtmlEmail(to, "Enrollment Confirmed — " + courseTitle, html);
            log.info("Enrollment email sent to {}", to);
        } catch (Exception e) {
            log.error("Failed to send enrollment email to {}", to, e);
        }
    }

    public void sendCapacityAlert(String adminEmail, String courseTitle,
                                   String courseDate, int enrolledCount,
                                   int maxCapacity) {
        try {
            Context ctx = new Context();
            ctx.setVariable("courseTitle", courseTitle);
            ctx.setVariable("courseDate", courseDate);
            ctx.setVariable("enrolledCount", enrolledCount);
            ctx.setVariable("maxCapacity", maxCapacity);
            String html = templateEngine.process("capacity-alert", ctx);
            sendHtmlEmail(
                adminEmail,
                "🚨 Course Full: " + courseTitle,
                html
            );
            log.info("Capacity alert sent for course: {}", courseTitle);
        } catch (Exception e) {
            log.error("Failed to send capacity alert for course: {}", courseTitle, e);
        }
    }

    private void sendHtmlEmail(String to, String subject, String html)
            throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(html, true);
        mailSender.send(message);
    }
}
