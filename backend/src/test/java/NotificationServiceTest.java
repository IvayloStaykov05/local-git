import com.example.project.backend.dto.response.notification.NotificationResponse;
import com.example.project.backend.model.entity.Notification;
import com.example.project.backend.model.entity.User;
import com.example.project.backend.model.enums.NotificationType;
import com.example.project.backend.repository.NotificationRepository;
import com.example.project.backend.repository.UserRepository;
import com.example.project.backend.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private NotificationService notificationService;

    @Test
    void shouldSendNotificationSuccessfully() {
        User recipient = User.builder().username("maria").build();
        User sender = User.builder().username("ownerUser").build();

        notificationService.send(
                recipient,
                sender,
                "ownerUser invited you",
                NotificationType.ROLE_REQUEST
        );

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());

        Notification saved = captor.getValue();
        assertEquals(recipient, saved.getRecipient());
        assertEquals(sender, saved.getSender());
        assertEquals("ownerUser invited you", saved.getMessage());
        assertEquals(NotificationType.ROLE_REQUEST, saved.getType());
        assertFalse(saved.isRead());
        assertNotNull(saved.getCreatedAt());
    }

    @Test
    void shouldReturnNotificationsSuccessfully() {
        User user = User.builder().username("maria").build();
        User sender = User.builder().username("ownerUser").build();

        Notification notification = Notification.builder()
                .recipient(user)
                .sender(sender)
                .message("Invitation sent")
                .type(NotificationType.ROLE_REQUEST)
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build();
        notification.setId(1L);

        when(userRepository.findByUsername("maria")).thenReturn(Optional.of(user));
        when(notificationRepository.findByRecipientOrderByCreatedAtDesc(user))
                .thenReturn(List.of(notification));

        List<NotificationResponse> response = notificationService.getNotifications("maria");

        assertEquals(1, response.size());
        assertEquals(1L, response.get(0).getId());
        assertEquals("Invitation sent", response.get(0).getMessage());
        assertEquals("ROLE_REQUEST", response.get(0).getType());
        assertFalse(response.get(0).isRead());
        assertEquals("ownerUser", response.get(0).getSender());
    }

    @Test
    void shouldMarkNotificationAsRead() {
        Notification notification = Notification.builder()
                .message("Invitation sent")
                .isRead(false)
                .build();
        notification.setId(1L);

        when(notificationRepository.findById(1L)).thenReturn(Optional.of(notification));

        notificationService.markAsRead(1L);

        assertTrue(notification.isRead());
    }

    @Test
    void shouldMarkAllNotificationsAsRead() {
        User user = User.builder().username("maria").build();

        Notification n1 = Notification.builder().isRead(false).build();
        Notification n2 = Notification.builder().isRead(false).build();

        when(userRepository.findByUsername("maria")).thenReturn(Optional.of(user));
        when(notificationRepository.findByRecipientAndIsReadFalse(user)).thenReturn(List.of(n1, n2));

        notificationService.markAllAsRead("maria");

        assertTrue(n1.isRead());
        assertTrue(n2.isRead());
    }
}