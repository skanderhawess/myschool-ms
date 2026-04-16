package com.myschool.notificationservice.dto;

import com.myschool.notificationservice.entity.Notification;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

/**
 * DTO d'entrée pour l'envoi d'une notification.
 * Les champs optionnels (studentName, courseTitle, etc.) servent
 * à alimenter les templates Thymeleaf selon le type.
 */
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class NotificationRequest {

    @NotBlank(message = "L'email du destinataire est obligatoire")
    @Email(message = "Format d'email invalide")
    private String recipientEmail;

    /** Identifiant optionnel du destinataire (étudiant) */
    private Long recipientId;

    @NotBlank(message = "Le sujet est obligatoire")
    private String subject;

    /** Contenu brut (utilisé uniquement si type = OTHER) */
    private String content;

    @NotNull(message = "Le type de notification est obligatoire")
    private Notification.NotificationType type;

    // --- Variables Thymeleaf optionnelles ---
    private String studentName;
    private String courseTitle;
    private String courseDate;
    private Integer enrolledCount;
    private Integer maxCapacity;
}
