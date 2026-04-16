package com.myschool.notificationservice.controller;

import com.myschool.notificationservice.dto.NotificationRequest;
import com.myschool.notificationservice.dto.NotificationResponse;
import com.myschool.notificationservice.entity.Notification;
import com.myschool.notificationservice.service.NotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Contrôleur REST pour l'envoi et la consultation des notifications.
 */
@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    /** Envoyer une nouvelle notification */
    @PostMapping("/send")
    public ResponseEntity<NotificationResponse> send(
            @Valid @RequestBody NotificationRequest request) {
        NotificationResponse response = notificationService.sendNotification(request);
        return ResponseEntity.ok(response);
    }

    /** Lister toutes les notifications */
    @GetMapping
    public List<NotificationResponse> getAll() {
        return notificationService.getAllNotifications();
    }

    /** Détail d'une notification par ID */
    @GetMapping("/{id}")
    public NotificationResponse getById(@PathVariable String id) {
        return notificationService.getById(id);
    }

    /** Notifications d'un destinataire */
    @GetMapping("/recipient/{recipientId}")
    public List<NotificationResponse> getByRecipient(@PathVariable Long recipientId) {
        return notificationService.getByRecipientId(recipientId);
    }

    /** Filtrer par statut (PENDING, SENT, FAILED) */
    @GetMapping("/status/{status}")
    public List<NotificationResponse> getByStatus(
            @PathVariable Notification.NotificationStatus status) {
        return notificationService.getByStatus(status);
    }
}
