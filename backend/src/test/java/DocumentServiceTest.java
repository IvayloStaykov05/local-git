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
import com.example.project.backend.service.DocumentFileStorageService;
import com.example.project.backend.service.DocumentService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import com.example.project.backend.dto.response.document.DocumentDetailsResponse;
import com.example.project.backend.model.entity.DocumentMember;
import com.example.project.backend.model.entity.DocumentVersion;
import com.example.project.backend.model.entity.User;
import com.example.project.backend.model.enums.DocumentRole;
import com.example.project.backend.model.enums.SystemRole;
import com.example.project.backend.dto.response.document.DocumentDetailsResponse;
import com.example.project.backend.dto.response.document.DocumentDetailsResponse;
import com.example.project.backend.model.entity.DocumentMember;
import com.example.project.backend.model.entity.DocumentVersion;
import com.example.project.backend.model.entity.User;
import com.example.project.backend.model.enums.DocumentRole;
import com.example.project.backend.model.enums.SystemRole;
import java.util.List;
import java.util.Optional;

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

    @Mock
    private DocumentFileStorageService documentFileStorageService;

    @InjectMocks
    private DocumentService documentService;

    @Test
    void shouldCreateFirstDocumentSuccessfully() {
        MultipartFile file = new MockMultipartFile(
                "file",
                "plan.pdf",
                "application/pdf",
                "dummy-content".getBytes()
        );

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

        DocumentFileStorageService.StoredFileData storedFile =
                new DocumentFileStorageService.StoredFileData(
                        "/uploads/documents/10/v1-plan.pdf",
                        "plan.pdf",
                        "application/pdf",
                        123L
                );

        DocumentVersion savedVersion = DocumentVersion.builder()
                .document(savedDocument)
                .versionNumber(1)
                .filePath("/uploads/documents/10/v1-plan.pdf")
                .originalFileName("plan.pdf")
                .contentType("application/pdf")
                .fileSize(123L)
                .status(VersionStatus.DRAFT)
                .createdBy(loggedUser)
                .build();
        savedVersion.setId(100L);

        when(userRepository.findByUsername("ivan123")).thenReturn(Optional.of(loggedUser));
        when(documentRepository.save(any(Document.class))).thenReturn(savedDocument);
        when(documentFileStorageService.saveFile(10L, 1, file)).thenReturn(storedFile);
        when(documentVersionRepository.save(any(DocumentVersion.class))).thenReturn(savedVersion);

        CreateFirstDocumentResponse response = documentService.createFirstDocument(
                "Project Plan",
                "Initial project document",
                file,
                "ivan123"
        );

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
        verify(documentFileStorageService).saveFile(10L, 1, file);
        verify(documentVersionRepository).save(any(DocumentVersion.class));
    }

    @Test
    void shouldThrowWhenFileIsMissingWhileCreatingFirstDocument() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> documentService.createFirstDocument(
                        "Project Plan",
                        "Initial project document",
                        null,
                        "ivan123"
                )
        );

        assertEquals("File is required", exception.getMessage());

        verify(userRepository, never()).findByUsername(anyString());
        verify(documentRepository, never()).save(any(Document.class));
        verify(documentMemberRepository, never()).save(any(DocumentMember.class));
        verify(documentVersionRepository, never()).save(any(DocumentVersion.class));
        verify(documentFileStorageService, never()).saveFile(anyLong(), anyInt(), any());
    }

    @Test
    void shouldReturnLoggedUserDocumentsSuccessfully() {
        User creator = User.builder()
                .username("ownerUser")
                .email("owner@example.com")
                .build();

        DocumentVersion activeVersion = DocumentVersion.builder()
                .versionNumber(2)
                .filePath("/uploads/documents/20/v2-spec.pdf")
                .originalFileName("spec.pdf")
                .contentType("application/pdf")
                .fileSize(456L)
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
        assertEquals("spec.pdf", response.get(0).getOriginalFileName());
        assertEquals("application/pdf", response.get(0).getContentType());
        assertEquals(456L, response.get(0).getFileSize());

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

    @Test
    void shouldReturnDocumentDetailsSuccessfully() {
        User loggedUser = User.builder()
                .username("ivan123")
                .build();

        User owner = User.builder()
                .username("ownerUser")
                .build();

        DocumentVersion activeVersion = DocumentVersion.builder()
                .versionNumber(2)
                .originalFileName("v2-plan.pdf")
                .contentType("application/pdf")
                .fileSize(222L)
                .build();
        activeVersion.setId(20L);

        Document document = Document.builder()
                .title("Project Plan")
                .description("Project description")
                .createdBy(owner)
                .activeVersion(activeVersion)
                .build();
        document.setId(10L);

        DocumentMember membership = DocumentMember.builder()
                .document(document)
                .user(loggedUser)
                .role(DocumentRole.AUTHOR)
                .build();

        when(userRepository.findByUsername("ivan123")).thenReturn(Optional.of(loggedUser));
        when(documentRepository.findDetailsById(10L)).thenReturn(Optional.of(document));
        when(documentMemberRepository.findByDocumentAndUser(document, loggedUser)).thenReturn(Optional.of(membership));
        when(documentMemberRepository.findAllByDocumentIdWithUser(10L)).thenReturn(List.of(membership));

        DocumentDetailsResponse response = documentService.getDocumentDetails(10L, "ivan123");

        assertNotNull(response);
        assertEquals(10L, response.getId());
        assertEquals("Project Plan", response.getTitle());
        assertEquals("Project description", response.getDescription());
        assertEquals("ownerUser", response.getCreatedBy());
        assertEquals("AUTHOR", response.getCurrentUserRole());
        assertEquals(2, response.getActiveVersionNumber());
        assertEquals(20L, response.getActiveVersionId());
        assertEquals("v2-plan.pdf", response.getActiveFileName());
        assertEquals("application/pdf", response.getActiveContentType());
        assertEquals(222L, response.getActiveFileSize());
        assertEquals(1, response.getTeamMembers().size());
    }

    @Test
    void shouldThrowWhenGettingDocumentDetailsWithoutAccess() {
        User loggedUser = User.builder()
                .username("ivan123")
                .systemRole(SystemRole.USER)
                .build();

        User owner = User.builder()
                .username("ownerUser")
                .build();

        Document document = Document.builder()
                .title("Project Plan")
                .description("Project description")
                .createdBy(owner)
                .build();
        document.setId(10L);

        when(userRepository.findByUsername("ivan123")).thenReturn(Optional.of(loggedUser));
        when(documentRepository.findDetailsById(10L)).thenReturn(Optional.of(document));
        when(documentMemberRepository.findByDocumentAndUser(document, loggedUser)).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> documentService.getDocumentDetails(10L, "ivan123")
        );

        assertEquals("You don't have access to this document", exception.getMessage());
        verify(documentMemberRepository, never()).findAllByDocumentIdWithUser(anyLong());
    }

    @Test
    void shouldAllowAdminToGetDocumentDetailsWithoutMembership() {
        User admin = User.builder()
                .username("adminUser")
                .systemRole(SystemRole.ADMIN)
                .build();

        User owner = User.builder()
                .username("ownerUser")
                .build();

        DocumentVersion activeVersion = DocumentVersion.builder()
                .versionNumber(1)
                .originalFileName("v1-plan.pdf")
                .contentType("application/pdf")
                .fileSize(111L)
                .build();
        activeVersion.setId(11L);

        Document document = Document.builder()
                .title("Project Plan")
                .description("Admin can inspect this")
                .createdBy(owner)
                .activeVersion(activeVersion)
                .build();
        document.setId(10L);

        when(userRepository.findByUsername("adminUser")).thenReturn(Optional.of(admin));
        when(documentRepository.findDetailsById(10L)).thenReturn(Optional.of(document));
        when(documentMemberRepository.findByDocumentAndUser(document, admin)).thenReturn(Optional.empty());
        when(documentMemberRepository.findAllByDocumentIdWithUser(10L)).thenReturn(List.of());

        DocumentDetailsResponse response = documentService.getDocumentDetails(10L, "adminUser");

        assertNotNull(response);
        assertEquals(10L, response.getId());
        assertEquals("Project Plan", response.getTitle());
        assertEquals("ownerUser", response.getCreatedBy());
        assertEquals("ADMIN", response.getCurrentUserRole());
        assertEquals(1, response.getActiveVersionNumber());
        assertEquals(11L, response.getActiveVersionId());
        assertEquals("v1-plan.pdf", response.getActiveFileName());
        assertEquals("application/pdf", response.getActiveContentType());
        assertEquals(111L, response.getActiveFileSize());
        assertTrue(response.getTeamMembers().isEmpty());
    }

}