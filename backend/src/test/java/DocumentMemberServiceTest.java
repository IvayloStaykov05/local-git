import com.example.project.backend.dto.request.documentMember.CreateDocumentMemberRequest;
import com.example.project.backend.dto.response.documentMember.CreateDocumentMemberResponse;
import com.example.project.backend.dto.response.documentMember.SharedUserResponse;
import com.example.project.backend.model.entity.Document;
import com.example.project.backend.model.entity.DocumentMember;
import com.example.project.backend.model.entity.User;
import com.example.project.backend.model.enums.DocumentRole;
import com.example.project.backend.model.enums.SystemRole;
import com.example.project.backend.repository.DocumentMemberRepository;
import com.example.project.backend.repository.DocumentRepository;
import com.example.project.backend.repository.UserRepository;
import com.example.project.backend.service.DocumentMemberService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.example.project.backend.dto.request.documentMember.DeleteDocumentMemberRequest;
import com.example.project.backend.dto.response.documentMember.DeleteDocumentMemberResponse;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DocumentMemberServiceTest {

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private DocumentMemberRepository documentMemberRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private DocumentMemberService documentMemberService;

    @Test
    void shouldCreateDocumentMemberSuccessfullyWhenLoggedUserIsOwner() {
        CreateDocumentMemberRequest request = new CreateDocumentMemberRequest();
        request.setDocumentId(1L);
        request.setOwner("ownerUser");
        request.setUsername("authorUser");
        request.setRole("AUTHOR");

        User loggedUser = User.builder()
                .username("ownerUser")
                .systemRole(SystemRole.USER)
                .build();

        User targetUser = User.builder()
                .username("authorUser")
                .build();

        Document document = Document.builder()
                .title("Project Plan")
                .createdBy(loggedUser)
                .build();
        document.setId(1L);

        DocumentMember loggedMember = DocumentMember.builder()
                .document(document)
                .user(loggedUser)
                .role(DocumentRole.OWNER)
                .build();

        DocumentMember savedMember = DocumentMember.builder()
                .document(document)
                .user(targetUser)
                .role(DocumentRole.AUTHOR)
                .addedBy(loggedUser)
                .build();
        savedMember.setId(55L);

        when(userRepository.findByUsername("ownerUser"))
                .thenReturn(Optional.of(loggedUser));
        when(userRepository.findByUsername("authorUser"))
                .thenReturn(Optional.of(targetUser));
        when(documentRepository.findById(1L))
                .thenReturn(Optional.of(document));
        when(documentMemberRepository.findByDocumentAndUser(document, loggedUser))
                .thenReturn(Optional.of(loggedMember));
        when(documentMemberRepository.findByDocumentAndUser(document, targetUser))
                .thenReturn(Optional.empty());
        when(documentMemberRepository.save(any(DocumentMember.class)))
                .thenReturn(savedMember);

        CreateDocumentMemberResponse response =
                documentMemberService.createDocumentMember(request, "ownerUser");

        assertNotNull(response);
        assertEquals(55L, response.getId());
        assertEquals(DocumentRole.AUTHOR, response.getRole());
        assertEquals("authorUser", response.getUsername());
        assertEquals("Project Plan", response.getTitle());
        assertEquals("User added successfully", response.getMessage());

        verify(documentMemberRepository).save(any(DocumentMember.class));
    }

    @Test
    void shouldThrowWhenLoggedUserHasNoAccessToDocument() {
        CreateDocumentMemberRequest request = new CreateDocumentMemberRequest();
        request.setDocumentId(1L);
        request.setOwner("ownerUser");
        request.setUsername("readerUser");
        request.setRole("READER");

        User loggedUser = User.builder()
                .username("authorUser")
                .systemRole(SystemRole.USER)
                .build();

        User targetUser = User.builder()
                .username("readerUser")
                .build();

        User owner = User.builder()
                .username("ownerUser")
                .systemRole(SystemRole.USER)
                .build();

        Document document = Document.builder()
                .title("Project Plan")
                .createdBy(owner)
                .build();
        document.setId(1L);

        when(userRepository.findByUsername("authorUser"))
                .thenReturn(Optional.of(loggedUser));
        when(userRepository.findByUsername("readerUser"))
                .thenReturn(Optional.of(targetUser));
        when(documentRepository.findById(1L))
                .thenReturn(Optional.of(document));
        when(documentMemberRepository.findByDocumentAndUser(document, loggedUser))
                .thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> documentMemberService.createDocumentMember(request, "authorUser")
        );

        assertEquals("You don't have access to this document", exception.getMessage());
        verify(documentMemberRepository, never()).save(any(DocumentMember.class));
    }

    @Test
    void shouldThrowWhenLoggedUserIsNotOwner() {
        CreateDocumentMemberRequest request = new CreateDocumentMemberRequest();
        request.setDocumentId(1L);
        request.setOwner("ownerUser");
        request.setUsername("readerUser");
        request.setRole("READER");

        User loggedUser = User.builder()
                .username("authorUser")
                .systemRole(SystemRole.USER)
                .build();

        User targetUser = User.builder()
                .username("readerUser")
                .build();

        User owner = User.builder()
                .username("ownerUser")
                .systemRole(SystemRole.USER)
                .build();

        Document document = Document.builder()
                .title("Project Plan")
                .createdBy(owner)
                .build();
        document.setId(1L);

        DocumentMember loggedMember = DocumentMember.builder()
                .document(document)
                .user(loggedUser)
                .role(DocumentRole.AUTHOR)
                .build();

        when(userRepository.findByUsername("authorUser"))
                .thenReturn(Optional.of(loggedUser));
        when(userRepository.findByUsername("readerUser"))
                .thenReturn(Optional.of(targetUser));
        when(documentRepository.findById(1L))
                .thenReturn(Optional.of(document));
        when(documentMemberRepository.findByDocumentAndUser(document, loggedUser))
                .thenReturn(Optional.of(loggedMember));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> documentMemberService.createDocumentMember(request, "authorUser")
        );

        assertEquals("Only the owner can add members to this document", exception.getMessage());
        verify(documentMemberRepository, never()).save(any(DocumentMember.class));
    }

    @Test
    void shouldThrowWhenTryingToAddYourselfAgain() {
        CreateDocumentMemberRequest request = new CreateDocumentMemberRequest();
        request.setDocumentId(1L);
        request.setOwner("ownerUser");
        request.setUsername("ownerUser");
        request.setRole("AUTHOR");

        User loggedUser = User.builder()
                .username("ownerUser")
                .systemRole(SystemRole.USER)
                .build();

        Document document = Document.builder()
                .title("Project Plan")
                .createdBy(loggedUser)
                .build();
        document.setId(1L);

        DocumentMember loggedMember = DocumentMember.builder()
                .document(document)
                .user(loggedUser)
                .role(DocumentRole.OWNER)
                .build();

        when(userRepository.findByUsername("ownerUser"))
                .thenReturn(Optional.of(loggedUser));
        when(documentRepository.findById(1L))
                .thenReturn(Optional.of(document));
        when(documentMemberRepository.findByDocumentAndUser(document, loggedUser))
                .thenReturn(Optional.of(loggedMember));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> documentMemberService.createDocumentMember(request, "ownerUser")
        );

        assertEquals("You cannot add yourself again", exception.getMessage());
        verify(documentMemberRepository, never()).save(any(DocumentMember.class));
    }

    @Test
    void shouldThrowWhenRoleIsInvalid() {
        CreateDocumentMemberRequest request = new CreateDocumentMemberRequest();
        request.setDocumentId(1L);
        request.setOwner("ownerUser");
        request.setUsername("readerUser");
        request.setRole("INVALID_ROLE");

        User loggedUser = User.builder()
                .username("ownerUser")
                .systemRole(SystemRole.USER)
                .build();

        User targetUser = User.builder()
                .username("readerUser")
                .build();

        Document document = Document.builder()
                .title("Project Plan")
                .createdBy(loggedUser)
                .build();
        document.setId(1L);

        DocumentMember loggedMember = DocumentMember.builder()
                .document(document)
                .user(loggedUser)
                .role(DocumentRole.OWNER)
                .build();

        when(userRepository.findByUsername("ownerUser"))
                .thenReturn(Optional.of(loggedUser));
        when(userRepository.findByUsername("readerUser"))
                .thenReturn(Optional.of(targetUser));
        when(documentRepository.findById(1L))
                .thenReturn(Optional.of(document));
        when(documentMemberRepository.findByDocumentAndUser(document, loggedUser))
                .thenReturn(Optional.of(loggedMember));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> documentMemberService.createDocumentMember(request, "ownerUser")
        );

        assertEquals("Invalid document role", exception.getMessage());
        verify(documentMemberRepository, never()).save(any(DocumentMember.class));
    }

    @Test
    void shouldReturnSharedUsersSuccessfully() {
        User sharedUser1 = User.builder()
                .username("maria")
                .firstName("Maria")
                .lastName("Ivanova")
                .email("maria@example.com")
                .build();
        sharedUser1.setId(1L);

        User sharedUser2 = User.builder()
                .username("georgi")
                .firstName("Georgi")
                .lastName("Petrov")
                .email("georgi@example.com")
                .build();
        sharedUser2.setId(2L);

        when(documentMemberRepository.findDistinctSharedUsersByUsername("ivan123"))
                .thenReturn(List.of(sharedUser1, sharedUser2));

        List<SharedUserResponse> response = documentMemberService.getSharedUsers("ivan123");

        assertEquals(2, response.size());

        assertEquals(1L, response.get(0).getId());
        assertEquals("maria", response.get(0).getUsername());
        assertEquals("Maria", response.get(0).getFirstName());
        assertEquals("Ivanova", response.get(0).getLastName());
        assertEquals("maria@example.com", response.get(0).getEmail());

        assertEquals(2L, response.get(1).getId());
        assertEquals("georgi", response.get(1).getUsername());
        assertEquals("Georgi", response.get(1).getFirstName());
        assertEquals("Petrov", response.get(1).getLastName());
        assertEquals("georgi@example.com", response.get(1).getEmail());
    }

    @Test
    void shouldDeleteDocumentMemberSuccessfully() {
        DeleteDocumentMemberRequest request = new DeleteDocumentMemberRequest();
        request.setDocumentId(1L);
        request.setUsername("readerUser");

        User loggedUser = User.builder()
                .username("ownerUser")
                .systemRole(SystemRole.USER)
                .build();

        User targetUser = User.builder()
                .username("readerUser")
                .build();

        Document document = Document.builder()
                .title("Project Plan")
                .build();
        document.setId(1L);

        DocumentMember loggedMember = DocumentMember.builder()
                .document(document)
                .user(loggedUser)
                .role(DocumentRole.OWNER)
                .build();

        DocumentMember targetMember = DocumentMember.builder()
                .document(document)
                .user(targetUser)
                .role(DocumentRole.READER)
                .build();

        when(userRepository.findByUsername("ownerUser")).thenReturn(Optional.of(loggedUser));
        when(documentRepository.findById(1L)).thenReturn(Optional.of(document));
        when(documentMemberRepository.findByDocumentAndUser(document, loggedUser)).thenReturn(Optional.of(loggedMember));
        when(userRepository.findByUsername("readerUser")).thenReturn(Optional.of(targetUser));
        when(documentMemberRepository.findByDocumentAndUser(document, targetUser)).thenReturn(Optional.of(targetMember));

        DeleteDocumentMemberResponse response =
                documentMemberService.deleteDocumentMember(request, "ownerUser");

        assertNotNull(response);
        assertEquals("readerUser", response.getUsername());
        assertEquals("READER", response.getRole());
        assertEquals(1L, response.getDocumentId());
        assertEquals("You removed the user successfully", response.getMessage());

        verify(documentMemberRepository).delete(targetMember);
    }

    @Test
    void shouldThrowWhenNonOwnerTriesToDeleteDocumentMember() {
        DeleteDocumentMemberRequest request = new DeleteDocumentMemberRequest();
        request.setDocumentId(1L);
        request.setUsername("readerUser");

        User loggedUser = User.builder()
                .username("authorUser")
                .systemRole(SystemRole.USER)
                .build();

        Document document = Document.builder()
                .title("Project Plan")
                .build();
        document.setId(1L);

        DocumentMember loggedMember = DocumentMember.builder()
                .document(document)
                .user(loggedUser)
                .role(DocumentRole.AUTHOR)
                .build();

        when(userRepository.findByUsername("authorUser")).thenReturn(Optional.of(loggedUser));
        when(documentRepository.findById(1L)).thenReturn(Optional.of(document));
        when(documentMemberRepository.findByDocumentAndUser(document, loggedUser)).thenReturn(Optional.of(loggedMember));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> documentMemberService.deleteDocumentMember(request, "authorUser")
        );

        assertEquals("You don't have the rights to change roles for this document", exception.getMessage());

        verify(documentMemberRepository, never()).delete(any(DocumentMember.class));
    }

    @Test
    void shouldThrowWhenTryingToRemoveYourselfFromDocument() {
        DeleteDocumentMemberRequest request = new DeleteDocumentMemberRequest();
        request.setDocumentId(1L);
        request.setUsername("ownerUser");

        User loggedUser = User.builder()
                .username("ownerUser")
                .systemRole(SystemRole.USER)
                .build();

        Document document = Document.builder()
                .title("Project Plan")
                .build();
        document.setId(1L);

        DocumentMember loggedMember = DocumentMember.builder()
                .document(document)
                .user(loggedUser)
                .role(DocumentRole.OWNER)
                .build();

        when(userRepository.findByUsername("ownerUser")).thenReturn(Optional.of(loggedUser));
        when(documentRepository.findById(1L)).thenReturn(Optional.of(document));
        when(documentMemberRepository.findByDocumentAndUser(document, loggedUser)).thenReturn(Optional.of(loggedMember));
        when(userRepository.findByUsername("ownerUser")).thenReturn(Optional.of(loggedUser));
        when(documentMemberRepository.findByDocumentAndUser(document, loggedUser)).thenReturn(Optional.of(loggedMember));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> documentMemberService.deleteDocumentMember(request, "ownerUser")
        );

        assertEquals("You can't remove yourself from the document", exception.getMessage());

        verify(documentMemberRepository, never()).delete(any(DocumentMember.class));
    }

    @Test
    void shouldThrowWhenDeletingUserThatHasNoRoleInDocument() {
        DeleteDocumentMemberRequest request = new DeleteDocumentMemberRequest();
        request.setDocumentId(1L);
        request.setUsername("readerUser");

        User loggedUser = User.builder()
                .username("ownerUser")
                .systemRole(SystemRole.USER)
                .build();

        User targetUser = User.builder()
                .username("readerUser")
                .build();

        Document document = Document.builder()
                .title("Project Plan")
                .build();
        document.setId(1L);

        DocumentMember loggedMember = DocumentMember.builder()
                .document(document)
                .user(loggedUser)
                .role(DocumentRole.OWNER)
                .build();

        when(userRepository.findByUsername("ownerUser")).thenReturn(Optional.of(loggedUser));
        when(documentRepository.findById(1L)).thenReturn(Optional.of(document));
        when(documentMemberRepository.findByDocumentAndUser(document, loggedUser)).thenReturn(Optional.of(loggedMember));
        when(userRepository.findByUsername("readerUser")).thenReturn(Optional.of(targetUser));
        when(documentMemberRepository.findByDocumentAndUser(document, targetUser)).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> documentMemberService.deleteDocumentMember(request, "ownerUser")
        );

        assertEquals("The user has no role in the document", exception.getMessage());

        verify(documentMemberRepository, never()).delete(any(DocumentMember.class));
    }

}