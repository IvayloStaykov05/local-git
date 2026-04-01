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

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
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
        request.setTitle("Project Plan");
        request.setOwner("ownerUser");
        request.setUsername("authorUser");
        request.setRole("AUTHOR");

        User loggedUser = User.builder()
                .username("ownerUser")
                .systemRole(SystemRole.USER)
                .build();

        User owner = User.builder()
                .username("ownerUser")
                .systemRole(SystemRole.USER)
                .build();

        User userMember = User.builder()
                .username("authorUser")
                .build();

        Document document = Document.builder()
                .title("Project Plan")
                .createdBy(owner)
                .build();

        DocumentMember loggedMember = DocumentMember.builder()
                .document(document)
                .user(loggedUser)
                .role(DocumentRole.OWNER)
                .build();

        DocumentMember savedMember = DocumentMember.builder()
                .document(document)
                .user(userMember)
                .role(DocumentRole.AUTHOR)
                .addedBy(loggedUser)
                .build();
        savedMember.setId(55L);

        when(userRepository.findByUsername("ownerUser"))
                .thenReturn(Optional.of(loggedUser), Optional.of(owner));
        when(userRepository.findByUsername("authorUser"))
                .thenReturn(Optional.of(userMember));
        when(documentRepository.findByTitleAndCreatedBy("Project Plan", owner))
                .thenReturn(Optional.of(document));
        when(documentMemberRepository.findByDocumentAndUser(document, loggedUser))
                .thenReturn(Optional.of(loggedMember));
        when(documentMemberRepository.save(any(DocumentMember.class)))
                .thenReturn(savedMember);

        CreateDocumentMemberResponse response =
                documentMemberService.createDocumentMember(request, "ownerUser");

        assertNotNull(response);
        assertEquals(55L, response.getId());
        assertEquals(DocumentRole.AUTHOR, response.getRole());
        assertEquals("authorUser", response.getUsername());
        assertEquals("Project Plan", response.getTitle());
        assertEquals("Document role added successfully", response.getMessage());

        verify(documentMemberRepository).save(any(DocumentMember.class));
    }

    @Test
    void shouldCreateDocumentMemberSuccessfullyWhenLoggedUserIsAdmin() {
        CreateDocumentMemberRequest request = new CreateDocumentMemberRequest();
        request.setTitle("Project Plan");
        request.setOwner("ownerUser");
        request.setUsername("reviewerUser");
        request.setRole("REVIEWER");

        User admin = User.builder()
                .username("adminUser")
                .systemRole(SystemRole.ADMIN)
                .build();

        User owner = User.builder()
                .username("ownerUser")
                .systemRole(SystemRole.USER)
                .build();

        User userMember = User.builder()
                .username("reviewerUser")
                .build();

        Document document = Document.builder()
                .title("Project Plan")
                .createdBy(owner)
                .build();

        DocumentMember savedMember = DocumentMember.builder()
                .document(document)
                .user(userMember)
                .role(DocumentRole.REVIEWER)
                .addedBy(admin)
                .build();
        savedMember.setId(77L);

        when(userRepository.findByUsername("adminUser"))
                .thenReturn(Optional.of(admin));
        when(userRepository.findByUsername("ownerUser"))
                .thenReturn(Optional.of(owner));
        when(userRepository.findByUsername("reviewerUser"))
                .thenReturn(Optional.of(userMember));
        when(documentRepository.findByTitleAndCreatedBy("Project Plan", owner))
                .thenReturn(Optional.of(document));
        when(documentMemberRepository.save(any(DocumentMember.class)))
                .thenReturn(savedMember);

        CreateDocumentMemberResponse response =
                documentMemberService.createDocumentMember(request, "adminUser");

        assertNotNull(response);
        assertEquals(77L, response.getId());
        assertEquals(DocumentRole.REVIEWER, response.getRole());
        assertEquals("reviewerUser", response.getUsername());
        assertEquals("Project Plan", response.getTitle());
        assertEquals("Document role added successfully", response.getMessage());

        verify(documentMemberRepository, never()).findByDocumentAndUser(document, admin);
        verify(documentMemberRepository).save(any(DocumentMember.class));
    }

    @Test
    void shouldThrowWhenLoggedUserIsNotOwnerOrAdmin() {
        CreateDocumentMemberRequest request = new CreateDocumentMemberRequest();
        request.setTitle("Project Plan");
        request.setOwner("ownerUser");
        request.setUsername("readerUser");
        request.setRole("READER");

        User loggedUser = User.builder()
                .username("authorUser")
                .systemRole(SystemRole.USER)
                .build();

        User owner = User.builder()
                .username("ownerUser")
                .systemRole(SystemRole.USER)
                .build();

        User userMember = User.builder()
                .username("readerUser")
                .build();

        Document document = Document.builder()
                .title("Project Plan")
                .createdBy(owner)
                .build();

        DocumentMember loggedMember = DocumentMember.builder()
                .document(document)
                .user(loggedUser)
                .role(DocumentRole.AUTHOR)
                .build();

        when(userRepository.findByUsername("authorUser"))
                .thenReturn(Optional.of(loggedUser));
        when(userRepository.findByUsername("ownerUser"))
                .thenReturn(Optional.of(owner));
        when(userRepository.findByUsername("readerUser"))
                .thenReturn(Optional.of(userMember));
        when(documentRepository.findByTitleAndCreatedBy("Project Plan", owner))
                .thenReturn(Optional.of(document));
        when(documentMemberRepository.findByDocumentAndUser(document, loggedUser))
                .thenReturn(Optional.of(loggedMember));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> documentMemberService.createDocumentMember(request, "authorUser")
        );

        assertEquals("You don't have the rights to change roles for this document", exception.getMessage());
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

        when(documentMemberRepository.findDistinctSharedUsersByUsernameAndSearch("ivan123", "mar"))
                .thenReturn(List.of(sharedUser1, sharedUser2));

        List<SharedUserResponse> response = documentMemberService.getSharedUsers("ivan123", "mar");

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
}