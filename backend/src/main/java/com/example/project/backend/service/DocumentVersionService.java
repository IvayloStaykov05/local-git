package com.example.project.backend.service;

import com.example.project.backend.dto.request.documentVersion.ApproveDocumentVersionRequest;
import com.example.project.backend.dto.request.documentVersion.CreateDocumentVersionRequest;
import com.example.project.backend.dto.request.documentVersion.RejectDocumentVersionRequest;
import com.example.project.backend.dto.response.documentVersion.ApproveDocumentVersionResponse;
import com.example.project.backend.dto.response.documentVersion.CreateDocumentVersionResponse;
import com.example.project.backend.dto.response.documentVersion.DocumentFileResponse;
import com.example.project.backend.dto.response.documentVersion.DocumentVersionHistoryResponse;
import com.example.project.backend.dto.response.documentVersion.RejectDocumentVersionResponse;
import com.example.project.backend.model.entity.*;
import com.example.project.backend.model.enums.DocumentRole;
import com.example.project.backend.model.enums.NotificationType;
import com.example.project.backend.model.enums.VersionStatus;
import com.example.project.backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DocumentVersionService {

    private final DocumentRepository documentRepository;
    private final DocumentVersionRepository documentVersionRepository;
    private final DocumentMemberRepository documentMemberRepository;
    private final UserRepository userRepository;
    private final CommentRepository commentRepository;
    private final DocumentFileStorageService documentFileStorageService;

    @Transactional
    public CreateDocumentVersionResponse createDocumentVersion(
            CreateDocumentVersionRequest request,
            MultipartFile file,
            String username
    ) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is required");
        }

        User loggedUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Logged user not found"));

        Document document = documentRepository.findById(request.getDocumentId())
                .orElseThrow(() -> new IllegalArgumentException("Document not found"));

        DocumentMember documentMember = documentMemberRepository.findByDocumentAndUser(document, loggedUser)
                .orElseThrow(() -> new IllegalArgumentException("You don't have access to the document"));

        if (documentMember.getRole() != DocumentRole.AUTHOR && documentMember.getRole() != DocumentRole.OWNER) {
            throw new IllegalArgumentException("You don't have the rights to make changes to this document");
        }

        DocumentVersion currentActiveVersion = document.getActiveVersion();
        if (currentActiveVersion == null) {
            throw new IllegalArgumentException("Document has no active version");
        }

        document.setNumberOfVersions(document.getNumberOfVersions() + 1);
        int newVersionNumber = document.getNumberOfVersions();

        DocumentFileStorageService.StoredFileData storedFile =
                documentFileStorageService.saveFile(document.getId(), newVersionNumber, file);

        DocumentVersion newVersion = DocumentVersion.builder()
                .document(document)
                .versionNumber(newVersionNumber)
                .filePath(storedFile.filePath())
                .originalFileName(storedFile.originalFileName())
                .contentType(storedFile.contentType())
                .fileSize(storedFile.fileSize())
                .status(VersionStatus.DRAFT)
                .createdBy(loggedUser)
                .parentVersion(currentActiveVersion)
                .build();

        DocumentVersion savedVersion = documentVersionRepository.save(newVersion);

        return new CreateDocumentVersionResponse(
                savedVersion.getId(),
                document.getTitle(),
                loggedUser.getUsername(),
                savedVersion.getVersionNumber(),
                "Document version created successfully"
        );
    }

    @Transactional
    public ApproveDocumentVersionResponse approveVersion(
            ApproveDocumentVersionRequest request,
            String reviewerUsername
    ) {
        User reviewer = userRepository.findByUsername(reviewerUsername)
                .orElseThrow(() -> new IllegalArgumentException("Reviewer not found"));

        DocumentVersion version = documentVersionRepository
                .findByIdAndDocumentId(request.getVersionId(), request.getDocumentId())
                .orElseThrow(() -> new IllegalArgumentException("Version not found"));

        if (version.getStatus() != VersionStatus.DRAFT) {
            throw new IllegalArgumentException("Only draft versions can be approved");
        }

        Document document = version.getDocument();

        DocumentMember membership = documentMemberRepository.findByDocumentAndUser(document, reviewer)
                .orElseThrow(() -> new IllegalArgumentException("User is not an authorized member for this action"));

        if (membership.getRole() != DocumentRole.REVIEWER) {
            throw new IllegalArgumentException("Only reviewers can approve versions");
        }

        if(request.getComment() != null){
            Comment comment = Comment.builder()
                    .version(version)
                    .user(reviewer)
                    .commentText(request.getComment())
                    .createdAt(LocalDateTime.now())
                    .build();
            commentRepository.save(comment);
        }

        version.setStatus(VersionStatus.APPROVED);
        version.setApprovedBy(reviewer);
        version.setApprovedAt(LocalDateTime.now());
        document.setActiveVersion(version);

        documentVersionRepository.save(version);
        documentRepository.save(document);

        return new ApproveDocumentVersionResponse(
                document.getId(),
                document.getTitle(),
                version.getVersionNumber(),
                reviewer.getUsername(),
                version.getStatus().name(),
                request.getComment()
        );
    }

    @Transactional
    public RejectDocumentVersionResponse rejectVersion(
            RejectDocumentVersionRequest request,
            String reviewerUsername
    ) {
        User reviewer = userRepository.findByUsername(reviewerUsername)
                .orElseThrow(() -> new IllegalArgumentException("Reviewer not found"));

        DocumentVersion version = documentVersionRepository
                .findByIdAndDocumentId(request.getVersionId(), request.getDocumentId())
                .orElseThrow(() -> new IllegalArgumentException("Version not found"));

        if (version.getStatus() != VersionStatus.DRAFT) {
            throw new IllegalArgumentException("Only draft versions can be rejected");
        }

        Document document = version.getDocument();

        DocumentMember membership = documentMemberRepository.findByDocumentAndUser(document, reviewer)
                .orElseThrow(() -> new IllegalArgumentException("User is not an authorized member for this action"));

        if (membership.getRole() != DocumentRole.REVIEWER) {
            throw new IllegalArgumentException("Only reviewers can reject versions");
        }

        Comment comment = Comment.builder()
                .version(version)
                .user(reviewer)
                .commentText(request.getReason())
                .createdAt(LocalDateTime.now())
                .build();

        version.setStatus(VersionStatus.REJECTED);
        version.setRejectedBy(reviewer);
        version.setRejectedAt(LocalDateTime.now());

        documentVersionRepository.save(version);
        commentRepository.save(comment);

        return new RejectDocumentVersionResponse(
                document.getId(),
                document.getTitle(),
                version.getVersionNumber(),
                reviewer.getUsername(),
                version.getStatus().name(),
                request.getReason()
        );
    }

    @Transactional(readOnly = true)
    public List<DocumentVersionHistoryResponse> getVersionHistory(Long documentId) {
        return documentVersionRepository.findAllByDocumentIdOrderByVersionNumberDesc(documentId)
                .stream()
                .map(version -> new DocumentVersionHistoryResponse(
                        version.getId(),
                        version.getVersionNumber(),
                        version.getStatus().name(),
                        version.getCreatedBy() != null ? version.getCreatedBy().getUsername() : null,
                        version.getApprovedBy() != null ? version.getApprovedBy().getUsername() : null,
                        version.getRejectedBy() != null ? version.getRejectedBy().getUsername() : null,
                        version.getCreatedAt(),
                        version.getApprovedAt(),
                        version.getRejectedAt()
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public DocumentFileResponse downloadVersionFile(Long versionId, Long documentId) {
        DocumentVersion version = documentVersionRepository
                .findByIdAndDocumentId(versionId, documentId)
                .orElseThrow(() -> new IllegalArgumentException("Version not found"));

        return documentFileStorageService.readFile(version);
    }
}