package com.example.project.backend.service;

import com.example.project.backend.dto.response.notification.NotificationResponse;
import com.example.project.backend.model.entity.AdminInvitation;
import com.example.project.backend.model.entity.DocumentInvitation;
import com.example.project.backend.model.entity.Notification;
import com.example.project.backend.model.entity.User;
import com.example.project.backend.model.enums.InvitationStatus;
import com.example.project.backend.model.enums.NotificationType;
import com.example.project.backend.repository.AdminInvitationRepository;
import com.example.project.backend.repository.DocumentInvitationRepository;
import com.example.project.backend.repository.NotificationRepository;
import com.example.project.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final DocumentInvitationRepository documentInvitationRepository;
    private final AdminInvitationRepository adminInvitationRepository;
    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);

    private static final Pattern DOCUMENT_TITLE_PATTERN = Pattern.compile("join document \\\"(.*?)\\\"");
    private static final Pattern ROLE_PATTERN = Pattern.compile(" as ([A-Z_]+)$");

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

        logger.info("Notification with id {} was sent from user with id {} to user with id {}", notification.getId(), sender.getId(), recipient.getId());
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> getNotifications(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    logger.error("Unable to get notifications - user with username {} not found", username);
                    return new IllegalArgumentException("User not found");
                });

        logger.info("Notifications for user with id {} successfully obtained", user.getId());

        return notificationRepository.findByRecipientOrderByCreatedAtDesc(user)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    private NotificationResponse mapToResponse(Notification notification) {
        boolean actionable = false;
        Long invitationId = null;
        Long documentId = null;
        String documentTitle = null;
        String role = null;

        if (notification.getType() == NotificationType.ROLE_REQUEST) {
            List<DocumentInvitation> pendingInvitations = documentInvitationRepository
                    .findByRecipientAndSenderAndStatusOrderByCreatedAtDesc(
                            notification.getRecipient(),
                            notification.getSender(),
                            InvitationStatus.PENDING
                    );

            DocumentInvitation matchingInvitation = resolveMatchingDocumentInvitation(
                    pendingInvitations,
                    notification.getMessage()
            );

            if (matchingInvitation != null) {
                actionable = true;
                invitationId = matchingInvitation.getId();
                documentId = matchingInvitation.getDocument().getId();
                documentTitle = matchingInvitation.getDocument().getTitle();
                role = matchingInvitation.getRole().name();
            }
        } else if (notification.getType() == NotificationType.ADMIN_REQUEST) {
            AdminInvitation pendingInvitation = adminInvitationRepository
                    .findFirstByRecipientAndSenderAndStatusOrderByCreatedAtDesc(
                            notification.getRecipient(),
                            notification.getSender(),
                            InvitationStatus.PENDING
                    )
                    .orElse(null);

            if (pendingInvitation != null) {
                actionable = true;
                invitationId = pendingInvitation.getId();
            }
        }

        return new NotificationResponse(
                notification.getId(),
                notification.getMessage(),
                notification.getType().name(),
                notification.isRead(),
                notification.getSender().getUsername(),
                notification.getCreatedAt(),
                actionable,
                invitationId,
                documentId,
                documentTitle,
                role
        );
    }

    private DocumentInvitation resolveMatchingDocumentInvitation(List<DocumentInvitation> pendingInvitations, String message) {
        if (pendingInvitations == null || pendingInvitations.isEmpty()) {
            return null;
        }

        String extractedTitle = extractDocumentTitle(message);
        String extractedRole = extractRole(message);

        for (DocumentInvitation invitation : pendingInvitations) {
            boolean matchesTitle = extractedTitle == null || extractedTitle.equals(invitation.getDocument().getTitle());
            boolean matchesRole = extractedRole == null || extractedRole.equals(invitation.getRole().name());

            if (matchesTitle && matchesRole) {
                return invitation;
            }
        }

        return pendingInvitations.get(0);
    }

    private String extractDocumentTitle(String message) {
        if (message == null || message.isBlank()) {
            return null;
        }

        Matcher matcher = DOCUMENT_TITLE_PATTERN.matcher(message);
        if (matcher.find()) {
            return matcher.group(1);
        }

        return null;
    }

    private String extractRole(String message) {
        if (message == null || message.isBlank()) {
            return null;
        }

        Matcher matcher = ROLE_PATTERN.matcher(message);
        if (matcher.find()) {
            return matcher.group(1);
        }

        return null;
    }

    @Transactional
    public void markAsRead(Long id) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() ->
                {
                    logger.error("Notification with id {} not found", id);
                    return new IllegalArgumentException("Notification not found");
                });

        notification.setRead(true);
        logger.info("Notification with id {} was read", id);
    }

    @Transactional
    public void markAllAsRead(String username) {
        User user = userRepository.findByUsername(username).orElseThrow();

        List<Notification> notifications =
                notificationRepository.findByRecipientAndIsReadFalse(user);

        notifications.forEach(n -> n.setRead(true));

        logger.info("All notification of user with id {} were read", user.getId());
    }
}