import com.example.project.backend.dto.request.invite.InviteUserRequest;
import com.example.project.backend.dto.response.invite.ActionResponse;
import com.example.project.backend.dto.response.invite.InvitationResponse;
import com.example.project.backend.model.entity.Document;
import com.example.project.backend.model.entity.DocumentInvitation;
import com.example.project.backend.model.entity.DocumentMember;
import com.example.project.backend.model.entity.User;
import com.example.project.backend.model.enums.DocumentRole;
import com.example.project.backend.model.enums.InvitationStatus;
import com.example.project.backend.model.enums.NotificationType;
import com.example.project.backend.repository.DocumentInvitationRepository;
import com.example.project.backend.repository.DocumentMemberRepository;
import com.example.project.backend.repository.DocumentRepository;
import com.example.project.backend.repository.UserRepository;
import com.example.project.backend.service.DocumentMemberService;
import com.example.project.backend.service.InvitationService;
import com.example.project.backend.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InvitationServiceTest {

    @Mock
    private DocumentInvitationRepository invitationRepository;

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private DocumentMemberRepository documentMemberRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private NotificationService notificationService;

    @Mock
    private DocumentMemberService documentMemberService;

    @InjectMocks
    private InvitationService invitationService;

    @Test
    void shouldInviteUserSuccessfully() {
        InviteUserRequest request = new InviteUserRequest();
        request.setDocumentId(10L);
        request.setUsername("maria");
        request.setRole("AUTHOR");

        User owner = User.builder().username("ownerUser").build();
        owner.setId(1L);

        User targetUser = User.builder().username("maria").build();
        targetUser.setId(2L);

        Document document = Document.builder()
                .title("Project Plan")
                .createdBy(owner)
                .build();
        document.setId(10L);

        DocumentMember ownerMember = DocumentMember.builder()
                .document(document)
                .user(owner)
                .role(DocumentRole.OWNER)
                .build();

        DocumentInvitation savedInvitation = DocumentInvitation.builder()
                .document(document)
                .sender(owner)
                .recipient(targetUser)
                .role(DocumentRole.AUTHOR)
                .status(InvitationStatus.PENDING)
                .build();
        savedInvitation.setId(100L);

        when(userRepository.findByUsername("ownerUser")).thenReturn(Optional.of(owner));
        when(userRepository.findByUsername("maria")).thenReturn(Optional.of(targetUser));
        when(documentRepository.findById(10L)).thenReturn(Optional.of(document));
        when(documentMemberRepository.findByDocumentAndUser(document, owner)).thenReturn(Optional.of(ownerMember));
        when(documentMemberRepository.findByDocumentAndUser(document, targetUser)).thenReturn(Optional.empty());
        when(invitationRepository.existsByDocumentAndRecipientAndStatus(document, targetUser, InvitationStatus.PENDING))
                .thenReturn(false);
        when(invitationRepository.save(any(DocumentInvitation.class))).thenReturn(savedInvitation);

        InvitationResponse response = invitationService.inviteUser("ownerUser", request);

        assertNotNull(response);
        assertEquals(100L, response.getId());
        assertEquals(10L, response.getDocumentId());
        assertEquals("Project Plan", response.getDocumentTitle());
        assertEquals("ownerUser", response.getSenderUsername());
        assertEquals("maria", response.getRecipientUsername());
        assertEquals("AUTHOR", response.getRole());
        assertEquals("PENDING", response.getStatus());
        assertEquals("Invitation sent successfully", response.getMessage());

        verify(notificationService).send(
                eq(targetUser),
                eq(owner),
                contains("invited you to join document"),
                eq(NotificationType.ROLE_REQUEST)
        );
    }

    @Test
    void shouldThrowWhenNonOwnerInvitesUser() {
        InviteUserRequest request = new InviteUserRequest();
        request.setDocumentId(10L);
        request.setUsername("maria");
        request.setRole("AUTHOR");

        User loggedUser = User.builder().username("authorUser").build();
        User targetUser = User.builder().username("maria").build();

        Document document = Document.builder().title("Project Plan").build();
        document.setId(10L);

        DocumentMember member = DocumentMember.builder()
                .document(document)
                .user(loggedUser)
                .role(DocumentRole.AUTHOR)
                .build();

        when(userRepository.findByUsername("authorUser")).thenReturn(Optional.of(loggedUser));
        when(userRepository.findByUsername("maria")).thenReturn(Optional.of(targetUser));
        when(documentRepository.findById(10L)).thenReturn(Optional.of(document));
        when(documentMemberRepository.findByDocumentAndUser(document, loggedUser)).thenReturn(Optional.of(member));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> invitationService.inviteUser("authorUser", request)
        );

        assertEquals("Only the owner can invite users", exception.getMessage());
        verify(invitationRepository, never()).save(any());
        verify(notificationService, never()).send(any(), any(), anyString(), any());
    }

    @Test
    void shouldThrowWhenOwnerInvitesSelf() {
        InviteUserRequest request = new InviteUserRequest();
        request.setDocumentId(10L);
        request.setUsername("ownerUser");
        request.setRole("AUTHOR");

        User owner = User.builder().username("ownerUser").build();
        owner.setId(1L);

        Document document = Document.builder().title("Project Plan").build();
        document.setId(10L);

        DocumentMember ownerMember = DocumentMember.builder()
                .document(document)
                .user(owner)
                .role(DocumentRole.OWNER)
                .build();

        when(userRepository.findByUsername("ownerUser")).thenReturn(Optional.of(owner));
        when(documentRepository.findById(10L)).thenReturn(Optional.of(document));
        when(documentMemberRepository.findByDocumentAndUser(document, owner)).thenReturn(Optional.of(ownerMember));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> invitationService.inviteUser("ownerUser", request)
        );

        assertEquals("You cannot invite yourself", exception.getMessage());
    }

    @Test
    void shouldThrowWhenPendingInvitationAlreadyExists() {
        InviteUserRequest request = new InviteUserRequest();
        request.setDocumentId(10L);
        request.setUsername("maria");
        request.setRole("AUTHOR");

        User owner = User.builder().username("ownerUser").build();
        owner.setId(1L);

        User targetUser = User.builder().username("maria").build();
        targetUser.setId(2L);

        Document document = Document.builder().title("Project Plan").build();
        document.setId(10L);

        DocumentMember ownerMember = DocumentMember.builder()
                .document(document)
                .user(owner)
                .role(DocumentRole.OWNER)
                .build();

        when(userRepository.findByUsername("ownerUser")).thenReturn(Optional.of(owner));
        when(userRepository.findByUsername("maria")).thenReturn(Optional.of(targetUser));
        when(documentRepository.findById(10L)).thenReturn(Optional.of(document));
        when(documentMemberRepository.findByDocumentAndUser(document, owner)).thenReturn(Optional.of(ownerMember));
        when(documentMemberRepository.findByDocumentAndUser(document, targetUser)).thenReturn(Optional.empty());
        when(invitationRepository.existsByDocumentAndRecipientAndStatus(document, targetUser, InvitationStatus.PENDING))
                .thenReturn(true);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> invitationService.inviteUser("ownerUser", request)
        );

        assertEquals("This user already has a pending invitation for this document", exception.getMessage());
        verify(invitationRepository, never()).save(any());
    }

    @Test
    void shouldAcceptInvitationSuccessfully() {
        User sender = User.builder().username("ownerUser").build();
        sender.setId(1L);

        User recipient = User.builder().username("maria").build();
        recipient.setId(2L);

        Document document = Document.builder().title("Project Plan").build();
        document.setId(10L);

        DocumentInvitation invitation = DocumentInvitation.builder()
                .document(document)
                .sender(sender)
                .recipient(recipient)
                .role(DocumentRole.AUTHOR)
                .status(InvitationStatus.PENDING)
                .build();
        invitation.setId(100L);

        when(userRepository.findByUsername("maria")).thenReturn(Optional.of(recipient));
        when(invitationRepository.findById(100L)).thenReturn(Optional.of(invitation));

        ActionResponse response = invitationService.accept(100L, "maria");

        assertNotNull(response);
        assertEquals("Invitation accepted successfully", response.getMessage());
        assertEquals(InvitationStatus.ACCEPTED, invitation.getStatus());

        verify(documentMemberService).addUserToDocument(document, recipient, DocumentRole.AUTHOR, sender);
        verify(notificationService).send(
                eq(sender),
                eq(recipient),
                contains("accepted your invitation"),
                eq(NotificationType.ROLE_ACCEPTED)
        );
    }

    @Test
    void shouldRejectInvitationSuccessfully() {
        User sender = User.builder().username("ownerUser").build();
        sender.setId(1L);

        User recipient = User.builder().username("maria").build();
        recipient.setId(2L);

        Document document = Document.builder().title("Project Plan").build();
        document.setId(10L);

        DocumentInvitation invitation = DocumentInvitation.builder()
                .document(document)
                .sender(sender)
                .recipient(recipient)
                .role(DocumentRole.AUTHOR)
                .status(InvitationStatus.PENDING)
                .build();
        invitation.setId(100L);

        when(userRepository.findByUsername("maria")).thenReturn(Optional.of(recipient));
        when(invitationRepository.findById(100L)).thenReturn(Optional.of(invitation));

        ActionResponse response = invitationService.reject(100L, "maria");

        assertNotNull(response);
        assertEquals("Invitation rejected successfully", response.getMessage());
        assertEquals(InvitationStatus.REJECTED, invitation.getStatus());

        verify(notificationService).send(
                eq(sender),
                eq(recipient),
                contains("rejected your invitation"),
                eq(NotificationType.ROLE_REJECTED)
        );
    }
}