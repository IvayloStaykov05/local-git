import com.example.project.backend.dto.request.documentVersion.ApproveDocumentVersionRequest;
import com.example.project.backend.dto.request.documentVersion.CreateDocumentVersionRequest;
import com.example.project.backend.dto.request.documentVersion.RejectDocumentVersionRequest;
import com.example.project.backend.dto.response.documentVersion.ApproveDocumentVersionResponse;
import com.example.project.backend.dto.response.documentVersion.CreateDocumentVersionResponse;
import com.example.project.backend.dto.response.documentVersion.DocumentVersionHistoryResponse;
import com.example.project.backend.dto.response.documentVersion.RejectDocumentVersionResponse;
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
import com.example.project.backend.service.DocumentVersionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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

    @InjectMocks
    private DocumentVersionService documentVersionService;

    @Test
    void shouldCreateDocumentVersionSuccessfully() {
        CreateDocumentVersionRequest request = new CreateDocumentVersionRequest();
        request.setTitle("Project Plan");
        request.setOwner("ownerUser");
        request.setContent("New content for version 2");

        User loggedUser = User.builder().username("authorUser").build();
        User owner = User.builder().username("ownerUser").build();

        DocumentVersion activeVersion = DocumentVersion.builder()
                .versionNumber(1)
                .content("Old content")
                .status(VersionStatus.APPROVED)
                .createdBy(owner)
                .build();

        Document document = Document.builder()
                .title("Project Plan")
                .createdBy(owner)
                .activeVersion(activeVersion)
                .build();

        DocumentMember membership = DocumentMember.builder()
                .document(document)
                .user(loggedUser)
                .role(DocumentRole.AUTHOR)
                .build();

        DocumentVersion savedVersion = DocumentVersion.builder()
                .document(document)
                .versionNumber(2)
                .content("New content for version 2")
                .status(VersionStatus.DRAFT)
                .createdBy(loggedUser)
                .parentVersion(activeVersion)
                .build();
        savedVersion.setId(200L);

        when(userRepository.findByUsername("authorUser")).thenReturn(Optional.of(loggedUser));
        when(userRepository.findByUsername("ownerUser")).thenReturn(Optional.of(owner));
        when(documentRepository.findByTitleAndCreatedBy("Project Plan", owner)).thenReturn(Optional.of(document));
        when(documentMemberRepository.findByDocumentAndUser(document, loggedUser)).thenReturn(Optional.of(membership));
        when(documentVersionRepository.save(any(DocumentVersion.class))).thenReturn(savedVersion);

        CreateDocumentVersionResponse response =
                documentVersionService.createDocumentVersion(request, "authorUser");

        assertNotNull(response);
        assertEquals(200L, response.getId());
        assertEquals("Project Plan", response.getTitle());
        assertEquals("authorUser", response.getCreatedByUsername());
        assertEquals(2, response.getVersion());
        assertEquals("Document version created successfully", response.getMessage());

        verify(documentVersionRepository).save(any(DocumentVersion.class));
    }

    @Test
    void shouldThrowWhenUserHasNoRightsToCreateDocumentVersion() {
        CreateDocumentVersionRequest request = new CreateDocumentVersionRequest();
        request.setTitle("Project Plan");
        request.setOwner("ownerUser");
        request.setContent("New content");

        User loggedUser = User.builder().username("readerUser").build();
        User owner = User.builder().username("ownerUser").build();

        DocumentVersion activeVersion = DocumentVersion.builder()
                .versionNumber(1)
                .content("Old content")
                .status(VersionStatus.APPROVED)
                .createdBy(owner)
                .build();

        Document document = Document.builder()
                .title("Project Plan")
                .createdBy(owner)
                .activeVersion(activeVersion)
                .build();

        DocumentMember membership = DocumentMember.builder()
                .document(document)
                .user(loggedUser)
                .role(DocumentRole.READER)
                .build();

        when(userRepository.findByUsername("readerUser")).thenReturn(Optional.of(loggedUser));
        when(userRepository.findByUsername("ownerUser")).thenReturn(Optional.of(owner));
        when(documentRepository.findByTitleAndCreatedBy("Project Plan", owner)).thenReturn(Optional.of(document));
        when(documentMemberRepository.findByDocumentAndUser(document, loggedUser)).thenReturn(Optional.of(membership));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> documentVersionService.createDocumentVersion(request, "readerUser")
        );

        assertEquals("You don't have the rights to make changes to this document", exception.getMessage());
        verify(documentVersionRepository, never()).save(any(DocumentVersion.class));
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
                .content("Version 2 content")
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
                .content("Version 3 content")
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
    }

    @Test
    void shouldReturnVersionHistorySuccessfully() {
        User author = User.builder().username("authorUser").build();
        User reviewer = User.builder().username("reviewerUser").build();

        DocumentVersion v2 = DocumentVersion.builder()
                .versionNumber(2)
                .status(VersionStatus.APPROVED)
                .createdBy(author)
                .approvedBy(reviewer)
                .createdAt(LocalDateTime.now().minusDays(1))
                .approvedAt(LocalDateTime.now())
                .build();
        v2.setId(2L);

        DocumentVersion v1 = DocumentVersion.builder()
                .versionNumber(1)
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
}