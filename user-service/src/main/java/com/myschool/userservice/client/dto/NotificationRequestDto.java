package com.myschool.userservice.client.dto;

import lombok.*;

/**
 * DTO miroir de NotificationRequest côté notification-service.
 * Utilisé pour les appels Feign vers POST /notifications/send.
 */
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class NotificationRequestDto {

    private String recipientEmail;
    private Long recipientId;
    private String subject;
    private String content;
    private NotificationType type;

    // Variables Thymeleaf optionnelles
    private String studentName;
    private String courseTitle;
    private String courseDate;
    private Integer enrolledCount;
    private Integer maxCapacity;

    /** Types de notification (miroir de l'enum côté notification-service) */
    public enum NotificationType {
        WELCOME_EMAIL,
        ENROLLMENT_CONFIRMATION,
        CAPACITY_ALERT,
        OTHER
    }
}
