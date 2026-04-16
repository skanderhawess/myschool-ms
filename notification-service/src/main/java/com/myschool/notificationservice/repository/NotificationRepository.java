package com.myschool.notificationservice.repository;

import com.myschool.notificationservice.entity.Notification;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

/**
 * Repository MongoDB pour les notifications.
 */
public interface NotificationRepository extends MongoRepository<Notification, String> {

    List<Notification> findByRecipientEmail(String email);

    List<Notification> findByRecipientId(Long id);

    List<Notification> findByType(Notification.NotificationType type);

    List<Notification> findByStatus(Notification.NotificationStatus status);

    long countByStatus(Notification.NotificationStatus status);
}
