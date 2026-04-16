package com.myschool.userservice.client.dto;

import lombok.*;

import java.time.LocalDateTime;

/**
 * DTO minimal pour la réponse du notification-service.
 */
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class NotificationResponseDto {

    private String id;
    private String recipientEmail;
    private String subject;
    private String status;
    private LocalDateTime sentAt;
    private String errorMessage;
}
