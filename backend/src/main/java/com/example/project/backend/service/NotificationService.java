package com.example.project.backend.service;

import com.example.project.backend.dto.response.notification.NotificationResponse;
import com.example.project.backend.model.entity.Notification;
import com.example.project.backend.model.entity.User;
import com.example.project.backend.model.enums.NotificationType;
import com.example.project.backend.repository.NotificationRepository;
import com.example.project.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    @Transactional
    public void send(User recipient, User sender, String message, NotificationType type) {
        Notification notification = Notification.builder()
                .recipient(recipient)
                .sender(sender)
                .message(message)
                .type(type)
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build();

        notificationRepository.save(notification);
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> getNotifications(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        return notificationRepository.findByRecipientOrderByCreatedAtDesc(user)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    private NotificationResponse mapToResponse(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getMessage(),
                notification.getType().name(),
                notification.isRead(),
                notification.getSender().getUsername(),
                notification.getCreatedAt()
        );
    }

    @Transactional
    public void markAsRead(Long id) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Notification not found"));

        notification.setRead(true);
    }


    @Transactional
    public void markAllAsRead(String username) {
        User user = userRepository.findByUsername(username).orElseThrow();

        List<Notification> notifications =
                notificationRepository.findByRecipientAndIsReadFalse(user);

        notifications.forEach(n -> n.setRead(true));
    }
}