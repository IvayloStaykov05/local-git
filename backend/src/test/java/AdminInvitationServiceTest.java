import com.example.project.backend.dto.request.admin.CreateAdminProfileRequest;
import com.example.project.backend.dto.request.admin.InviteAdminRequest;
import com.example.project.backend.dto.response.admin.AdminInvitationResponse;
import com.example.project.backend.dto.response.admin.CreateAdminProfileResponse;
import com.example.project.backend.dto.response.invite.ActionResponse;
import com.example.project.backend.model.entity.AdminInvitation;
import com.example.project.backend.model.entity.User;
import com.example.project.backend.model.enums.InvitationStatus;
import com.example.project.backend.model.enums.NotificationType;
import com.example.project.backend.model.enums.SystemRole;
import com.example.project.backend.repository.AdminInvitationRepository;
import com.example.project.backend.repository.UserRepository;
import com.example.project.backend.service.AdminInvitationService;
import com.example.project.backend.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminInvitationServiceTest {

    @Mock
    private AdminInvitationRepository adminInvitationRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private NotificationService notificationService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AdminInvitationService adminInvitationService;

    @Test
    void shouldSendAdminInvitationSuccessfully() {
        User admin = User.builder()
                .username("admin")
                .systemRole(SystemRole.ADMIN)
                .build();
        admin.setId(1L);

        User recipient = User.builder()
                .username("ivan")
                .systemRole(SystemRole.USER)
                .build();
        recipient.setId(2L);

        InviteAdminRequest request = new InviteAdminRequest();
        request.setUsername("ivan");

        AdminInvitation savedInvitation = AdminInvitation.builder()
                .sender(admin)
                .recipient(recipient)
                .status(InvitationStatus.PENDING)
                .build();
        savedInvitation.setId(100L);

        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(admin));
        when(userRepository.findByUsername("ivan")).thenReturn(Optional.of(recipient));
        when(adminInvitationRepository.existsByRecipientAndStatus(recipient, InvitationStatus.PENDING)).thenReturn(false);
        when(adminInvitationRepository.save(any(AdminInvitation.class))).thenReturn(savedInvitation);

        AdminInvitationResponse response = adminInvitationService.inviteToBecomeAdmin("admin", request);

        assertEquals(100L, response.getInvitationId());
        assertEquals("admin", response.getSenderUsername());
        assertEquals("ivan", response.getRecipientUsername());
        assertEquals("PENDING", response.getStatus());
        assertEquals("Admin invitation sent successfully", response.getMessage());

        verify(notificationService).send(
                eq(recipient),
                eq(admin),
                contains("invited you to become an admin"),
                eq(NotificationType.ADMIN_REQUEST)
        );
    }

    @Test
    void shouldAcceptAdminInvitationSuccessfully() {
        User sender = User.builder().username("admin").build();
        sender.setId(1L);

        User recipient = User.builder().username("ivan").build();
        recipient.setId(2L);

        AdminInvitation invitation = AdminInvitation.builder()
                .sender(sender)
                .recipient(recipient)
                .status(InvitationStatus.PENDING)
                .build();
        invitation.setId(100L);

        when(userRepository.findByUsername("ivan")).thenReturn(Optional.of(recipient));
        when(adminInvitationRepository.findById(100L)).thenReturn(Optional.of(invitation));

        ActionResponse response = adminInvitationService.acceptAdminInvitation(100L, "ivan");

        assertEquals("Admin invitation accepted successfully", response.getMessage());
        assertEquals(InvitationStatus.ACCEPTED, invitation.getStatus());

        verify(notificationService).send(
                eq(sender),
                eq(recipient),
                contains("accepted your admin invitation"),
                eq(NotificationType.ADMIN_ACCEPTED)
        );
    }

    @Test
    void shouldRejectAdminInvitationSuccessfully() {
        User sender = User.builder().username("admin").build();
        sender.setId(1L);

        User recipient = User.builder().username("ivan").build();
        recipient.setId(2L);

        AdminInvitation invitation = AdminInvitation.builder()
                .sender(sender)
                .recipient(recipient)
                .status(InvitationStatus.PENDING)
                .build();
        invitation.setId(100L);

        when(userRepository.findByUsername("ivan")).thenReturn(Optional.of(recipient));
        when(adminInvitationRepository.findById(100L)).thenReturn(Optional.of(invitation));

        ActionResponse response = adminInvitationService.rejectAdminInvitation(100L, "ivan");

        assertEquals("Admin invitation rejected successfully", response.getMessage());
        assertEquals(InvitationStatus.REJECTED, invitation.getStatus());

        verify(notificationService).send(
                eq(sender),
                eq(recipient),
                contains("rejected your admin invitation"),
                eq(NotificationType.ADMIN_REJECTED)
        );
    }

    @Test
    void shouldCreateAdminProfileSuccessfully() {
        User admin = User.builder()
                .username("admin")
                .systemRole(SystemRole.ADMIN)
                .build();

        User baseUser = User.builder()
                .username("ivan")
                .firstName("Ivan")
                .lastName("Petrov")
                .email("ivan@example.com")
                .myInfo("About Ivan")
                .build();
        baseUser.setId(2L);

        AdminInvitation invitation = AdminInvitation.builder()
                .recipient(baseUser)
                .status(InvitationStatus.ACCEPTED)
                .build();
        invitation.setId(100L);

        CreateAdminProfileRequest request = new CreateAdminProfileRequest();
        request.setInvitationId(100L);
        request.setAdminUsername("ivan_admin");
        request.setAdminEmail("ivan.admin@example.com");
        request.setAdminPassword("secret123");

        User savedAdminProfile = User.builder()
                .username("ivan_admin")
                .email("ivan.admin@example.com")
                .systemRole(SystemRole.ADMIN)
                .linkedUser(baseUser)
                .build();
        savedAdminProfile.setId(10L);

        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(admin));
        when(adminInvitationRepository.findById(100L)).thenReturn(Optional.of(invitation));
        when(userRepository.existsByLinkedUserAndSystemRole(baseUser, SystemRole.ADMIN)).thenReturn(false);
        when(userRepository.existsByUsername("ivan_admin")).thenReturn(false);
        when(userRepository.existsByEmail("ivan.admin@example.com")).thenReturn(false);
        when(passwordEncoder.encode("secret123")).thenReturn("encodedSecret");
        when(userRepository.save(any(User.class))).thenReturn(savedAdminProfile);

        CreateAdminProfileResponse response = adminInvitationService.createAdminProfile("admin", request);

        assertEquals(10L, response.getAdminProfileId());
        assertEquals(2L, response.getLinkedUserId());
        assertEquals("ivan_admin", response.getAdminUsername());
        assertEquals("ivan.admin@example.com", response.getAdminEmail());
        assertEquals("Admin profile created successfully", response.getMessage());
    }

    @Test
    void shouldThrowWhenNonAdminSendsAdminInvitation() {
        User user = User.builder()
                .username("ivan")
                .systemRole(SystemRole.USER)
                .build();

        InviteAdminRequest request = new InviteAdminRequest();
        request.setUsername("maria");

        when(userRepository.findByUsername("ivan")).thenReturn(Optional.of(user));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> adminInvitationService.inviteToBecomeAdmin("ivan", request)
        );

        assertEquals("Only admins can send invitations.", exception.getMessage());
    }

    @Test
    void shouldThrowWhenAdminInvitesSelf() {
        User admin = User.builder()
                .username("admin")
                .systemRole(SystemRole.ADMIN)
                .build();
        admin.setId(1L);

        InviteAdminRequest request = new InviteAdminRequest();
        request.setUsername("admin");

        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(admin));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> adminInvitationService.inviteToBecomeAdmin("admin", request)
        );

        assertEquals("You cannot invite yourself.", exception.getMessage());
    }

    @Test
    void shouldThrowWhenPendingAdminInvitationAlreadyExists() {
        User admin = User.builder()
                .username("admin")
                .systemRole(SystemRole.ADMIN)
                .build();
        admin.setId(1L);

        User recipient = User.builder()
                .username("ivan")
                .build();
        recipient.setId(2L);

        InviteAdminRequest request = new InviteAdminRequest();
        request.setUsername("ivan");

        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(admin));
        when(userRepository.findByUsername("ivan")).thenReturn(Optional.of(recipient));
        when(adminInvitationRepository.existsByRecipientAndStatus(recipient, InvitationStatus.PENDING)).thenReturn(true);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> adminInvitationService.inviteToBecomeAdmin("admin", request)
        );

        assertEquals("This user already has a pending admin invitation", exception.getMessage());
    }

    @Test
    void shouldThrowWhenUserAcceptsAdminInvitationThatIsNotHis() {
        User loggedUser = User.builder().username("ivan").build();
        loggedUser.setId(2L);

        User otherRecipient = User.builder().username("maria").build();
        otherRecipient.setId(3L);

        AdminInvitation invitation = AdminInvitation.builder()
                .recipient(otherRecipient)
                .status(InvitationStatus.PENDING)
                .build();
        invitation.setId(100L);

        when(userRepository.findByUsername("ivan")).thenReturn(Optional.of(loggedUser));
        when(adminInvitationRepository.findById(100L)).thenReturn(Optional.of(invitation));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> adminInvitationService.acceptAdminInvitation(100L, "ivan")
        );

        assertEquals("You cannot accept this admin invitation", exception.getMessage());
    }

    @Test
    void shouldThrowWhenAcceptingNonPendingAdminInvitation() {
        User loggedUser = User.builder().username("ivan").build();
        loggedUser.setId(2L);

        AdminInvitation invitation = AdminInvitation.builder()
                .recipient(loggedUser)
                .status(InvitationStatus.ACCEPTED)
                .build();
        invitation.setId(100L);

        when(userRepository.findByUsername("ivan")).thenReturn(Optional.of(loggedUser));
        when(adminInvitationRepository.findById(100L)).thenReturn(Optional.of(invitation));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> adminInvitationService.acceptAdminInvitation(100L, "ivan")
        );

        assertEquals("This admin invitation is no longer pending", exception.getMessage());
    }

    @Test
    void shouldThrowWhenCreateAdminProfileWithoutAcceptedInvitation() {
        User admin = User.builder()
                .username("admin")
                .systemRole(SystemRole.ADMIN)
                .build();

        User baseUser = User.builder()
                .username("ivan")
                .build();
        baseUser.setId(2L);

        AdminInvitation invitation = AdminInvitation.builder()
                .recipient(baseUser)
                .status(InvitationStatus.PENDING)
                .build();
        invitation.setId(100L);

        CreateAdminProfileRequest request = new CreateAdminProfileRequest();
        request.setInvitationId(100L);
        request.setAdminUsername("ivan_admin");
        request.setAdminEmail("ivan.admin@example.com");
        request.setAdminPassword("secret123");

        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(admin));
        when(adminInvitationRepository.findById(100L)).thenReturn(Optional.of(invitation));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> adminInvitationService.createAdminProfile("admin", request)
        );

        assertEquals("The admin invitation must be accepted first", exception.getMessage());
    }

    @Test
    void shouldThrowWhenAdminProfileAlreadyExists() {
        User admin = User.builder()
                .username("admin")
                .systemRole(SystemRole.ADMIN)
                .build();

        User baseUser = User.builder()
                .username("ivan")
                .build();
        baseUser.setId(2L);

        AdminInvitation invitation = AdminInvitation.builder()
                .recipient(baseUser)
                .status(InvitationStatus.ACCEPTED)
                .build();
        invitation.setId(100L);

        CreateAdminProfileRequest request = new CreateAdminProfileRequest();
        request.setInvitationId(100L);
        request.setAdminUsername("ivan_admin");
        request.setAdminEmail("ivan.admin@example.com");
        request.setAdminPassword("secret123");

        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(admin));
        when(adminInvitationRepository.findById(100L)).thenReturn(Optional.of(invitation));
        when(userRepository.existsByLinkedUserAndSystemRole(baseUser, SystemRole.ADMIN)).thenReturn(true);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> adminInvitationService.createAdminProfile("admin", request)
        );

        assertEquals("This user already has an admin profile", exception.getMessage());
    }

}