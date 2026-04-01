import com.example.project.backend.dto.request.document.CreateFirstDocumentRequest;
import com.example.project.backend.dto.response.document.CreateFirstDocumentResponse;
import com.example.project.backend.dto.response.document.DocumentListResponse;
import com.example.project.backend.model.entity.Document;
import com.example.project.backend.model.entity.DocumentMember;
import com.example.project.backend.model.entity.DocumentVersion;
import com.example.project.backend.model.entity.User;
import com.example.project.backend.model.enums.DocumentRole;
import com.example.project.backend.model.enums.VersionStatus;
import com.example.project.backend.repository.DocumentMemberRepository;
import com.example.project.backend.repository.DocumentRepository;
import com.example.project.backend.repository.DocumentVersionRepository;
import com.example.project.backend.repository.UserRepository;
import com.example.project.backend.service.DocumentService;
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
class DocumentServiceTest {

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private DocumentVersionRepository documentVersionRepository;

    @Mock
    private DocumentMemberRepository documentMemberRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private DocumentService documentService;

    @Test
    void shouldCreateFirstDocumentSuccessfully() {
        CreateFirstDocumentRequest request = new CreateFirstDocumentRequest();
        request.setTitle("Project Plan");
        request.setDescription("Initial project document");
        request.setContent("Version 1 content");

        User loggedUser = User.builder()
                .username("ivan123")
                .email("ivan@example.com")
                .build();

        Document savedDocument = Document.builder()
                .title("Project Plan")
                .description("Initial project document")
                .createdBy(loggedUser)
                .build();
        savedDocument.setId(10L);

        DocumentVersion savedVersion = DocumentVersion.builder()
                .document(savedDocument)
                .versionNumber(1)
                .content("Version 1 content")
                .status(VersionStatus.DRAFT)
                .createdBy(loggedUser)
                .build();
        savedVersion.setId(100L);

        when(userRepository.findByUsername("ivan123")).thenReturn(Optional.of(loggedUser));
        when(documentRepository.save(any(Document.class))).thenReturn(savedDocument);
        when(documentVersionRepository.save(any(DocumentVersion.class))).thenReturn(savedVersion);

        CreateFirstDocumentResponse response = documentService.createFirstDocument(request, "ivan123");

        assertNotNull(response);
        assertEquals(10L, response.getId());
        assertEquals("Project Plan", response.getTitle());
        assertEquals("Initial project document", response.getDescription());
        assertEquals("ivan123", response.getCreatedByUsername());
        assertEquals(DocumentRole.OWNER, response.getRole());
        assertEquals("Document created successfully", response.getMessage());

        verify(userRepository).findByUsername("ivan123");
        verify(documentRepository, atLeastOnce()).save(any(Document.class));
        verify(documentMemberRepository).save(any(DocumentMember.class));
        verify(documentVersionRepository, atLeastOnce()).save(any(DocumentVersion.class));
    }

    @Test
    void shouldThrowWhenLoggedUserNotFoundWhileCreatingFirstDocument() {
        CreateFirstDocumentRequest request = new CreateFirstDocumentRequest();
        request.setTitle("Project Plan");
        request.setDescription("Initial project document");
        request.setContent("Version 1 content");

        when(userRepository.findByUsername("missingUser")).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> documentService.createFirstDocument(request, "missingUser")
        );

        assertEquals("Logged user not found", exception.getMessage());

        verify(userRepository).findByUsername("missingUser");
        verify(documentRepository, never()).save(any(Document.class));
        verify(documentMemberRepository, never()).save(any(DocumentMember.class));
        verify(documentVersionRepository, never()).save(any(DocumentVersion.class));
    }

    @Test
    void shouldReturnLoggedUserDocumentsSuccessfully() {
        User creator = User.builder()
                .username("ownerUser")
                .email("owner@example.com")
                .build();

        DocumentVersion activeVersion = DocumentVersion.builder()
                .versionNumber(2)
                .content("Approved content")
                .status(VersionStatus.APPROVED)
                .createdBy(creator)
                .build();

        Document document = Document.builder()
                .title("Project Plan")
                .description("Project description")
                .createdBy(creator)
                .activeVersion(activeVersion)
                .build();
        document.setId(20L);

        DocumentMember membership = DocumentMember.builder()
                .document(document)
                .role(DocumentRole.AUTHOR)
                .build();

        when(documentMemberRepository.findMyDocumentsByUsernameAndSearch("ivan123", "Project"))
                .thenReturn(List.of(membership));

        List<DocumentListResponse> response = documentService.getLoggedUserDocuments("ivan123", "Project");

        assertEquals(1, response.size());
        assertEquals(20L, response.get(0).getId());
        assertEquals("Project Plan", response.get(0).getTitle());
        assertEquals("Project description", response.get(0).getDescription());
        assertEquals("AUTHOR", response.get(0).getRole());
        assertEquals("ownerUser", response.get(0).getCreatedBy());
        assertEquals(2, response.get(0).getActiveVersionNumber());
        assertEquals("Approved content", response.get(0).getContent());

        verify(documentMemberRepository).findMyDocumentsByUsernameAndSearch("ivan123", "Project");
    }

    @Test
    void shouldReturnEmptyListWhenNoDocumentsMatchSearch() {
        when(documentMemberRepository.findMyDocumentsByUsernameAndSearch("ivan123", "missing"))
                .thenReturn(List.of());

        List<DocumentListResponse> response = documentService.getLoggedUserDocuments("ivan123", "missing");

        assertNotNull(response);
        assertTrue(response.isEmpty());

        verify(documentMemberRepository).findMyDocumentsByUsernameAndSearch("ivan123", "missing");
    }
}