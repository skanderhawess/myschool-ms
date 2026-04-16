package com.myschool.notificationservice.dto;

import com.myschool.notificationservice.entity.Notification;
import lombok.*;

import java.time.LocalDateTime;

/**
 * DTO de sortie représentant une notification persistée.
 */
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class NotificationResponse {

    private String id;
    private String recipientEmail;
    private Long recipientId;
    private String subject;
    private String content;
    private Notification.NotificationType type;
    private Notification.NotificationStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime sentAt;
    private String errorMessage;

    /** Mapping entité → DTO */
    public static NotificationResponse from(Notification n) {
        return NotificationResponse.builder()
                .id(n.getId())
                .recipientEmail(n.getRecipientEmail())
                .recipientId(n.getRecipientId())
                .subject(n.getSubject())
                .content(n.getContent())
                .type(n.getType())
                .status(n.getStatus())
                .createdAt(n.getCreatedAt())
                .sentAt(n.getSentAt())
                .errorMessage(n.getErrorMessage())
                .build();
    }
}
