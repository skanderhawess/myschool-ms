package com.myschool.notificationservice.service;

import com.myschool.notificationservice.client.UserClient;
import com.myschool.notificationservice.dto.NotificationRequest;
import com.myschool.notificationservice.dto.NotificationResponse;
import com.myschool.notificationservice.entity.Notification;
import com.myschool.notificationservice.entity.Notification.NotificationStatus;
import com.myschool.notificationservice.entity.Notification.NotificationType;
import com.myschool.notificationservice.repository.NotificationRepository;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service principal de gestion des notifications.
 * Gère l'envoi d'emails via Thymeleaf + JavaMail et la persistance MongoDB.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final JavaMailSender mailSender;
    private final SpringTemplateEngine templateEngine;
    private final UserClient userClient;

    /**
     * Envoie une notification par email et la persiste en base.
     * En cas d'erreur d'envoi, la notification est marquée FAILED
     * sans propager l'exception (le service reste disponible).
     */
    public NotificationResponse sendNotification(NotificationRequest request) {

        // 1. Créer l'entité en statut PENDING
        Notification notification = Notification.builder()
                .recipientEmail(request.getRecipientEmail())
                .recipientId(request.getRecipientId())
                .subject(request.getSubject())
                .type(request.getType())
                .status(NotificationStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();

        notification = notificationRepository.save(notification);

        try {
            // 2. Générer le contenu HTML selon le type
            String htmlContent = generateHtmlContent(request);
            notification.setContent(htmlContent);

            // 3. Envoyer l'email
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(request.getRecipientEmail());
            helper.setSubject(request.getSubject());
            helper.setText(htmlContent, true);

            mailSender.send(message);

            // 4. Succès → marquer SENT
            notification.setStatus(NotificationStatus.SENT);
            notification.setSentAt(LocalDateTime.now());
            log.info("Notification envoyée avec succès à {} (type={})",
                    request.getRecipientEmail(), request.getType());

        } catch (Exception e) {
            // 5. Echec → marquer FAILED, ne pas propager
            notification.setStatus(NotificationStatus.FAILED);
            notification.setErrorMessage(e.getMessage());
            log.error("Echec d'envoi de notification à {} : {}",
                    request.getRecipientEmail(), e.getMessage());
        }

        notification = notificationRepository.save(notification);
        return NotificationResponse.from(notification);
    }

    /** Retourne toutes les notifications */
    public List<NotificationResponse> getAllNotifications() {
        return notificationRepository.findAll()
                .stream()
                .map(NotificationResponse::from)
                .collect(Collectors.toList());
    }

    /** Retourne une notification par son ID MongoDB */
    public NotificationResponse getById(String id) {
        Notification n = notificationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Notification introuvable : " + id));
        return NotificationResponse.from(n);
    }

    /** Retourne les notifications d'un destinataire donné */
    public List<NotificationResponse> getByRecipientId(Long recipientId) {
        return notificationRepository.findByRecipientId(recipientId)
                .stream()
                .map(NotificationResponse::from)
                .collect(Collectors.toList());
    }

    /** Filtre les notifications par statut (PENDING, SENT, FAILED) */
    public List<NotificationResponse> getByStatus(NotificationStatus status) {
        return notificationRepository.findByStatus(status)
                .stream()
                .map(NotificationResponse::from)
                .collect(Collectors.toList());
    }

    // ──────────────────────────────────────────────
    // Méthode privée : sélection du template Thymeleaf
    // ──────────────────────────────────────────────

    private String generateHtmlContent(NotificationRequest request) {
        if (request.getType() == NotificationType.OTHER) {
            // Pour le type OTHER, on utilise le contenu brut fourni
            return request.getContent() != null ? request.getContent() : "";
        }

        // Sélection du template selon le type
        String templateName = switch (request.getType()) {
            case WELCOME_EMAIL -> "welcome-email";
            case ENROLLMENT_CONFIRMATION -> "enrollment-confirmation";
            case CAPACITY_ALERT -> "capacity-alert";
            default -> throw new IllegalArgumentException("Type non supporté : " + request.getType());
        };

        // Préparation du contexte Thymeleaf avec les variables
        Context context = new Context();
        context.setVariable("studentName", request.getStudentName());
        context.setVariable("courseTitle", request.getCourseTitle());
        context.setVariable("courseDate", request.getCourseDate());
        context.setVariable("enrolledCount", request.getEnrolledCount());
        context.setVariable("maxCapacity", request.getMaxCapacity());

        return templateEngine.process(templateName, context);
    }
}
