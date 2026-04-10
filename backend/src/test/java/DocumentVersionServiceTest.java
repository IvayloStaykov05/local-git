import com.example.project.backend.dto.request.documentVersion.ApproveDocumentVersionRequest;
import com.example.project.backend.dto.request.documentVersion.CreateDocumentVersionRequest;
import com.example.project.backend.dto.request.documentVersion.RejectDocumentVersionRequest;
import com.example.project.backend.dto.response.documentVersion.ApproveDocumentVersionResponse;
import com.example.project.backend.dto.response.documentVersion.CreateDocumentVersionResponse;
import com.example.project.backend.dto.response.documentVersion.DocumentFileResponse;
import com.example.project.backend.dto.response.documentVersion.DocumentVersionDetailsResponse;
import com.example.project.backend.dto.response.documentVersion.DocumentVersionHistoryResponse;
import com.example.project.backend.dto.response.documentVersion.RejectDocumentVersionResponse;
import com.example.project.backend.model.entity.Comment;
import com.example.project.backend.model.entity.Document;
import com.example.project.backend.model.entity.DocumentMember;
import com.example.project.backend.model.entity.DocumentVersion;
import com.example.project.backend.model.entity.User;
import com.example.project.backend.model.enums.DocumentRole;
import com.example.project.backend.model.enums.VersionStatus;
import com.example.project.backend.repository.CommentRepository;
import com.example.project.backend.repository.DocumentMemberRepository;
import com.example.project.backend.repository.DocumentRepository;
import com.example.project.backend.repository.DocumentVersionRepository;
import com.example.project.backend.repository.UserRepository;
import com.example.project.backend.service.DocumentFileStorageService;
import com.example.project.backend.service.DocumentVersionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DocumentVersionServiceTest {

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private DocumentVersionRepository documentVersionRepository;

    @Mock
    private DocumentMemberRepository documentMemberRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private DocumentFileStorageService documentFileStorageService;

    @InjectMocks
    private DocumentVersionService documentVersionService;

    @Test
    void shouldCreateDocumentVersionSuccessfully() {
        CreateDocumentVersionRequest request = new CreateDocumentVersionRequest();
        request.setDocumentId(1L);

        MultipartFile file = new MockMultipartFile(
                "file",
                "v2-plan.pdf",
                "application/pdf",
                "new-file".getBytes()
        );

        User loggedUser = User.builder().username("authorUser").build();

        DocumentVersion activeVersion = DocumentVersion.builder()
                .versionNumber(1)
                .filePath("/uploads/documents/1/v1-plan.pdf")
                .originalFileName("v1-plan.pdf")
                .contentType("application/pdf")
                .fileSize(100L)
                .status(VersionStatus.APPROVED)
                .createdBy(loggedUser)
                .build();

        Document document = Document.builder()
                .title("Project Plan")
                .activeVersion(activeVersion)
                .numberOfVersions(1)
                .createdBy(loggedUser)
                .build();
        document.setId(1L);

        DocumentMember membership = DocumentMember.builder()
                .document(document)
                .user(loggedUser)
                .role(DocumentRole.AUTHOR)
                .build();

        DocumentFileStorageService.StoredFileData storedFile =
                new DocumentFileStorageService.StoredFileData(
                        "/uploads/documents/1/v2-plan.pdf",
                        "v2-plan.pdf",
                        "application/pdf",
                        222L
                );

        DocumentVersion savedVersion = DocumentVersion.builder()
                .document(document)
                .versionNumber(2)
                .filePath("/uploads/documents/1/v2-plan.pdf")
                .originalFileName("v2-plan.pdf")
                .contentType("application/pdf")
                .fileSize(222L)
                .status(VersionStatus.DRAFT)
                .createdBy(loggedUser)
                .parentVersion(activeVersion)
                .build();
        savedVersion.setId(200L);

        when(userRepository.findByUsername("authorUser")).thenReturn(Optional.of(loggedUser));
        when(documentRepository.findById(1L)).thenReturn(Optional.of(document));
        when(documentMemberRepository.findByDocumentAndUser(document, loggedUser)).thenReturn(Optional.of(membership));
        when(documentFileStorageService.saveFile(1L, 2, file)).thenReturn(storedFile);
        when(documentVersionRepository.save(any(DocumentVersion.class))).thenReturn(savedVersion);

        CreateDocumentVersionResponse response =
                documentVersionService.createDocumentVersion(request, file, "authorUser");

        assertNotNull(response);
        assertEquals(200L, response.getId());
        assertEquals("Project Plan", response.getTitle());
        assertEquals("authorUser", response.getCreatedByUsername());
        assertEquals(2, response.getVersion());
        assertEquals("Document version created successfully", response.getMessage());
        assertEquals(2, document.getNumberOfVersions());

        verify(documentFileStorageService).saveFile(1L, 2, file);
        verify(documentVersionRepository).save(any(DocumentVersion.class));
    }

    @Test
    void shouldThrowWhenUserHasNoRightsToCreateDocumentVersion() {
        CreateDocumentVersionRequest request = new CreateDocumentVersionRequest();
        request.setDocumentId(1L);

        MultipartFile file = new MockMultipartFile(
                "file",
                "v2-plan.pdf",
                "application/pdf",
                "new-file".getBytes()
        );

        User loggedUser = User.builder().username("readerUser").build();

        DocumentVersion activeVersion = DocumentVersion.builder()
                .versionNumber(1)
                .filePath("/uploads/documents/1/v1-plan.pdf")
                .originalFileName("v1-plan.pdf")
                .contentType("application/pdf")
                .fileSize(100L)
                .status(VersionStatus.APPROVED)
                .createdBy(loggedUser)
                .build();

        Document document = Document.builder()
                .title("Project Plan")
                .activeVersion(activeVersion)
                .numberOfVersions(1)
                .createdBy(loggedUser)
                .build();
        document.setId(1L);

        DocumentMember membership = DocumentMember.builder()
                .document(document)
                .user(loggedUser)
                .role(DocumentRole.READER)
                .build();

        when(userRepository.findByUsername("readerUser")).thenReturn(Optional.of(loggedUser));
        when(documentRepository.findById(1L)).thenReturn(Optional.of(document));
        when(documentMemberRepository.findByDocumentAndUser(document, loggedUser)).thenReturn(Optional.of(membership));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> documentVersionService.createDocumentVersion(request, file, "readerUser")
        );

        assertEquals("You don't have the rights to make changes to this document", exception.getMessage());
        verify(documentVersionRepository, never()).save(any(DocumentVersion.class));
        verify(documentFileStorageService, never()).saveFile(anyLong(), anyInt(), any());
    }

    @Test
    void shouldApproveVersionSuccessfully() {
        ApproveDocumentVersionRequest request = new ApproveDocumentVersionRequest();
        request.setDocumentId(1L);
        request.setVersionId(2L);
        request.setComment("Looks good");

        User reviewer = User.builder().username("reviewerUser").build();

        Document document = Document.builder()
                .title("Project Plan")
                .build();
        document.setId(1L);

        DocumentVersion version = DocumentVersion.builder()
                .document(document)
                .versionNumber(2)
                .filePath("/uploads/documents/1/v2-plan.pdf")
                .originalFileName("v2-plan.pdf")
                .contentType("application/pdf")
                .fileSize(222L)
                .status(VersionStatus.DRAFT)
                .build();
        version.setId(2L);

        DocumentMember membership = DocumentMember.builder()
                .document(document)
                .user(reviewer)
                .role(DocumentRole.REVIEWER)
                .build();

        when(userRepository.findByUsername("reviewerUser")).thenReturn(Optional.of(reviewer));
        when(documentVersionRepository.findByIdAndDocumentId(2L, 1L)).thenReturn(Optional.of(version));
        when(documentMemberRepository.findByDocumentAndUser(document, reviewer)).thenReturn(Optional.of(membership));

        ApproveDocumentVersionResponse response =
                documentVersionService.approveVersion(request, "reviewerUser");

        assertNotNull(response);
        assertEquals(1L, response.getDocumentId());
        assertEquals("Project Plan", response.getTitle());
        assertEquals(2, response.getVersion());
        assertEquals("reviewerUser", response.getReviewedBy());
        assertEquals("APPROVED", response.getStatus());
        assertEquals("Looks good", response.getComment());

        assertEquals(VersionStatus.APPROVED, version.getStatus());
        assertEquals(reviewer, version.getApprovedBy());
        assertNotNull(version.getApprovedAt());
        assertEquals(version, document.getActiveVersion());

        verify(documentVersionRepository).save(version);
        verify(documentRepository).save(document);
        verify(commentRepository).save(any(Comment.class));
    }

    @Test
    void shouldRejectVersionSuccessfully() {
        RejectDocumentVersionRequest request = new RejectDocumentVersionRequest();
        request.setDocumentId(1L);
        request.setVersionId(3L);
        request.setReason("Needs more changes");

        User reviewer = User.builder().username("reviewerUser").build();

        Document document = Document.builder()
                .title("Project Plan")
                .build();
        document.setId(1L);

        DocumentVersion version = DocumentVersion.builder()
                .document(document)
                .versionNumber(3)
                .filePath("/uploads/documents/1/v3-plan.pdf")
                .originalFileName("v3-plan.pdf")
                .contentType("application/pdf")
                .fileSize(333L)
                .status(VersionStatus.DRAFT)
                .build();
        version.setId(3L);

        DocumentMember membership = DocumentMember.builder()
                .document(document)
                .user(reviewer)
                .role(DocumentRole.REVIEWER)
                .build();

        when(userRepository.findByUsername("reviewerUser")).thenReturn(Optional.of(reviewer));
        when(documentVersionRepository.findByIdAndDocumentId(3L, 1L)).thenReturn(Optional.of(version));
        when(documentMemberRepository.findByDocumentAndUser(document, reviewer)).thenReturn(Optional.of(membership));

        RejectDocumentVersionResponse response =
                documentVersionService.rejectVersion(request, "reviewerUser");

        assertNotNull(response);
        assertEquals(1L, response.getDocumentId());
        assertEquals("Project Plan", response.getTitle());
        assertEquals(3, response.getVersion());
        assertEquals("reviewerUser", response.getRejectedBy());
        assertEquals("REJECTED", response.getStatus());
        assertEquals("Needs more changes", response.getComment());

        assertEquals(VersionStatus.REJECTED, version.getStatus());
        assertEquals(reviewer, version.getRejectedBy());
        assertNotNull(version.getRejectedAt());

        verify(documentVersionRepository).save(version);
        verify(commentRepository).save(any(Comment.class));
    }

    @Test
    void shouldReturnVersionHistorySuccessfully() {
        User author = User.builder().username("authorUser").build();
        User reviewer = User.builder().username("reviewerUser").build();

        DocumentVersion v2 = DocumentVersion.builder()
                .versionNumber(2)
                .filePath("/uploads/documents/10/v2.pdf")
                .originalFileName("v2.pdf")
                .contentType("application/pdf")
                .fileSize(222L)
                .status(VersionStatus.APPROVED)
                .createdBy(author)
                .approvedBy(reviewer)
                .createdAt(LocalDateTime.now().minusDays(1))
                .approvedAt(LocalDateTime.now())
                .build();
        v2.setId(2L);

        DocumentVersion v1 = DocumentVersion.builder()
                .versionNumber(1)
                .filePath("/uploads/documents/10/v1.pdf")
                .originalFileName("v1.pdf")
                .contentType("application/pdf")
                .fileSize(111L)
                .status(VersionStatus.REJECTED)
                .createdBy(author)
                .rejectedBy(reviewer)
                .createdAt(LocalDateTime.now().minusDays(2))
                .rejectedAt(LocalDateTime.now().minusDays(1))
                .build();
        v1.setId(1L);

        when(documentVersionRepository.findAllByDocumentIdOrderByVersionNumberDesc(10L))
                .thenReturn(List.of(v2, v1));

        List<DocumentVersionHistoryResponse> response = documentVersionService.getVersionHistory(10L);

        assertEquals(2, response.size());

        assertEquals(2L, response.get(0).getVersionId());
        assertEquals(2, response.get(0).getVersionNumber());
        assertEquals("APPROVED", response.get(0).getStatus());
        assertEquals("authorUser", response.get(0).getCreatedBy());
        assertEquals("reviewerUser", response.get(0).getApprovedBy());

        assertEquals(1L, response.get(1).getVersionId());
        assertEquals(1, response.get(1).getVersionNumber());
        assertEquals("REJECTED", response.get(1).getStatus());
        assertEquals("reviewerUser", response.get(1).getRejectedBy());
    }

    @Test
    void shouldThrowWhenFileIsMissingWhileCreatingDocumentVersion() {
        CreateDocumentVersionRequest request = new CreateDocumentVersionRequest();
        request.setDocumentId(1L);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> documentVersionService.createDocumentVersion(request, null, "authorUser")
        );

        assertEquals("File is required", exception.getMessage());

        verify(userRepository, never()).findByUsername(anyString());
        verify(documentRepository, never()).findById(anyLong());
        verify(documentMemberRepository, never()).findByDocumentAndUser(any(), any());
        verify(documentVersionRepository, never()).save(any(DocumentVersion.class));
        verify(documentFileStorageService, never()).saveFile(anyLong(), anyInt(), any());
    }

    @Test
    void shouldThrowWhenApprovingNonDraftVersion() {
        ApproveDocumentVersionRequest request = new ApproveDocumentVersionRequest();
        request.setDocumentId(1L);
        request.setVersionId(2L);
        request.setComment("Looks good");

        User reviewer = User.builder().username("reviewerUser").build();

        Document document = Document.builder()
                .title("Project Plan")
                .build();
        document.setId(1L);

        DocumentVersion version = DocumentVersion.builder()
                .document(document)
                .versionNumber(2)
                .filePath("/uploads/documents/1/v2-plan.pdf")
                .originalFileName("v2-plan.pdf")
                .contentType("application/pdf")
                .fileSize(222L)
                .status(VersionStatus.APPROVED)
                .build();
        version.setId(2L);

        when(userRepository.findByUsername("reviewerUser")).thenReturn(Optional.of(reviewer));
        when(documentVersionRepository.findByIdAndDocumentId(2L, 1L)).thenReturn(Optional.of(version));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> documentVersionService.approveVersion(request, "reviewerUser")
        );

        assertEquals("Only draft versions can be approved", exception.getMessage());

        verify(documentMemberRepository, never()).findByDocumentAndUser(any(), any());
        verify(documentVersionRepository, never()).save(any(DocumentVersion.class));
        verify(documentRepository, never()).save(any(Document.class));
        verify(commentRepository, never()).save(any(Comment.class));
    }

    @Test
    void shouldThrowWhenRejectingNonDraftVersion() {
        RejectDocumentVersionRequest request = new RejectDocumentVersionRequest();
        request.setDocumentId(1L);
        request.setVersionId(3L);
        request.setReason("Needs more changes");

        User reviewer = User.builder().username("reviewerUser").build();

        Document document = Document.builder()
                .title("Project Plan")
                .build();
        document.setId(1L);

        DocumentVersion version = DocumentVersion.builder()
                .document(document)
                .versionNumber(3)
                .filePath("/uploads/documents/1/v3-plan.pdf")
                .originalFileName("v3-plan.pdf")
                .contentType("application/pdf")
                .fileSize(333L)
                .status(VersionStatus.REJECTED)
                .build();
        version.setId(3L);

        when(userRepository.findByUsername("reviewerUser")).thenReturn(Optional.of(reviewer));
        when(documentVersionRepository.findByIdAndDocumentId(3L, 1L)).thenReturn(Optional.of(version));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> documentVersionService.rejectVersion(request, "reviewerUser")
        );

        assertEquals("Only draft versions can be rejected", exception.getMessage());

        verify(documentMemberRepository, never()).findByDocumentAndUser(any(), any());
        verify(documentVersionRepository, never()).save(any(DocumentVersion.class));
        verify(commentRepository, never()).save(any(Comment.class));
    }

    @Test
    void shouldDownloadVersionFileSuccessfully() {
        Document document = Document.builder()
                .title("Project Plan")
                .build();
        document.setId(10L);

        DocumentVersion version = DocumentVersion.builder()
                .document(document)
                .versionNumber(2)
                .filePath("/uploads/documents/10/v2-plan.pdf")
                .originalFileName("plan.pdf")
                .contentType("application/pdf")
                .fileSize(999L)
                .status(VersionStatus.APPROVED)
                .build();
        version.setId(20L);

        DocumentFileResponse fileResponse = new DocumentFileResponse(
                "plan.pdf",
                "application/pdf",
                "dummy-bytes".getBytes()
        );

        when(documentVersionRepository.findByIdAndDocumentId(20L, 10L)).thenReturn(Optional.of(version));
        when(documentFileStorageService.readFile(version)).thenReturn(fileResponse);

        DocumentFileResponse response = documentVersionService.downloadVersionFile(20L, 10L);

        assertNotNull(response);
        assertEquals("plan.pdf", response.getFileName());
        assertEquals("application/pdf", response.getContentType());
        assertArrayEquals("dummy-bytes".getBytes(), response.getContent());

        verify(documentVersionRepository).findByIdAndDocumentId(20L, 10L);
        verify(documentFileStorageService).readFile(version);
    }

    @Test
    void shouldThrowWhenDownloadingVersionFileThatDoesNotExist() {
        when(documentVersionRepository.findByIdAndDocumentId(20L, 10L)).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> documentVersionService.downloadVersionFile(20L, 10L)
        );

        assertEquals("Version not found", exception.getMessage());

        verify(documentVersionRepository).findByIdAndDocumentId(20L, 10L);
        verify(documentFileStorageService, never()).readFile(any(DocumentVersion.class));
    }

    @Test
    void shouldReturnActiveVersionSuccessfully() {
        User loggedUser = User.builder().username("authorUser").build();

        Document document = Document.builder()
                .title("Project Plan")
                .build();
        document.setId(10L);

        DocumentVersion activeVersion = DocumentVersion.builder()
                .document(document)
                .versionNumber(3)
                .filePath("/uploads/documents/10/v3-plan.pdf")
                .originalFileName("v3-plan.pdf")
                .contentType("application/pdf")
                .fileSize(333L)
                .status(VersionStatus.APPROVED)
                .createdBy(loggedUser)
                .createdAt(LocalDateTime.now())
                .build();
        activeVersion.setId(30L);

        document.setActiveVersion(activeVersion);

        when(userRepository.findByUsername("authorUser")).thenReturn(Optional.of(loggedUser));
        when(documentRepository.findById(10L)).thenReturn(Optional.of(document));
        when(documentMemberRepository.findByDocumentAndUser(document, loggedUser))
                .thenReturn(Optional.of(DocumentMember.builder()
                        .document(document)
                        .user(loggedUser)
                        .role(DocumentRole.AUTHOR)
                        .build()));

        DocumentVersionDetailsResponse response = documentVersionService.getActiveVersion(10L, "authorUser");

        assertNotNull(response);
        assertEquals(30L, response.getVersionId());
        assertEquals(10L, response.getDocumentId());
        assertEquals("Project Plan", response.getDocumentTitle());
        assertEquals(3, response.getVersionNumber());
        assertEquals("APPROVED", response.getStatus());
        assertEquals("authorUser", response.getCreatedBy());
        assertEquals("v3-plan.pdf", response.getOriginalFileName());
        assertEquals("application/pdf", response.getContentType());
        assertEquals(333L, response.getFileSize());
    }

    @Test
    void shouldReturnParentVersionSuccessfully() {
        User loggedUser = User.builder().username("authorUser").build();

        Document document = Document.builder()
                .title("Project Plan")
                .build();
        document.setId(10L);

        DocumentVersion parentVersion = DocumentVersion.builder()
                .document(document)
                .versionNumber(2)
                .filePath("/uploads/documents/10/v2-plan.pdf")
                .originalFileName("v2-plan.pdf")
                .contentType("application/pdf")
                .fileSize(222L)
                .status(VersionStatus.APPROVED)
                .createdBy(loggedUser)
                .createdAt(LocalDateTime.now().minusDays(1))
                .build();
        parentVersion.setId(20L);

        DocumentVersion activeVersion = DocumentVersion.builder()
                .document(document)
                .versionNumber(3)
                .filePath("/uploads/documents/10/v3-plan.pdf")
                .originalFileName("v3-plan.pdf")
                .contentType("application/pdf")
                .fileSize(333L)
                .status(VersionStatus.APPROVED)
                .createdBy(loggedUser)
                .createdAt(LocalDateTime.now())
                .parentVersion(parentVersion)
                .build();
        activeVersion.setId(30L);

        document.setActiveVersion(activeVersion);

        when(userRepository.findByUsername("authorUser")).thenReturn(Optional.of(loggedUser));
        when(documentRepository.findById(10L)).thenReturn(Optional.of(document));
        when(documentMemberRepository.findByDocumentAndUser(document, loggedUser))
                .thenReturn(Optional.of(DocumentMember.builder()
                        .document(document)
                        .user(loggedUser)
                        .role(DocumentRole.AUTHOR)
                        .build()));

        DocumentVersionDetailsResponse response = documentVersionService.getParentVersion(10L, "authorUser");

        assertNotNull(response);
        assertEquals(20L, response.getVersionId());
        assertEquals(10L, response.getDocumentId());
        assertEquals("Project Plan", response.getDocumentTitle());
        assertEquals(2, response.getVersionNumber());
        assertEquals("APPROVED", response.getStatus());
        assertEquals("authorUser", response.getCreatedBy());
        assertEquals("v2-plan.pdf", response.getOriginalFileName());
        assertEquals("application/pdf", response.getContentType());
        assertEquals(222L, response.getFileSize());
    }
}