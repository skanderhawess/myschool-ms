package com.myschool.notificationservice.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/**
 * Entité MongoDB représentant une notification envoyée ou en attente.
 */
@Document(collection = "notifications")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Notification {

    @Id
    private String id;

    private String recipientEmail;
    private Long recipientId;
    private String subject;
    private String content;

    private NotificationType type;
    private NotificationStatus status;

    private LocalDateTime createdAt;
    private LocalDateTime sentAt;
    private String errorMessage;

    /** Types de notification supportés */
    public enum NotificationType {
        WELCOME_EMAIL,
        ENROLLMENT_CONFIRMATION,
        CAPACITY_ALERT,
        OTHER
    }

    /** Statut du cycle de vie d'une notification */
    public enum NotificationStatus {
        PENDING,
        SENT,
        FAILED
    }
}
