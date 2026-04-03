package com.example.project.backend.service;

import com.example.project.backend.dto.response.document.CreateFirstDocumentResponse;
import com.example.project.backend.dto.response.document.DocumentDetailsResponse;
import com.example.project.backend.dto.response.document.DocumentListResponse;
import com.example.project.backend.dto.response.document.DocumentTeamMemberResponse;
import com.example.project.backend.model.entity.Document;
import com.example.project.backend.model.entity.DocumentMember;
import com.example.project.backend.model.entity.DocumentVersion;
import com.example.project.backend.model.entity.User;
import com.example.project.backend.model.enums.DocumentRole;
import com.example.project.backend.model.enums.SystemRole;
import com.example.project.backend.model.enums.VersionStatus;
import com.example.project.backend.repository.DocumentMemberRepository;
import com.example.project.backend.repository.DocumentRepository;
import com.example.project.backend.repository.DocumentVersionRepository;
import com.example.project.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final DocumentVersionRepository documentVersionRepository;
    private final DocumentMemberRepository documentMemberRepository;
    private final UserRepository userRepository;
    private final DocumentFileStorageService documentFileStorageService;

    @Transactional
    public CreateFirstDocumentResponse createFirstDocument(
            String title,
            String description,
            MultipartFile file,
            String username
    ) {

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is required");
        }

        User loggedUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Logged user not found"));

        Document document = Document.builder()
                .title(title)
                .description(description != null ? description.trim() : null)
                .createdBy(loggedUser)
                .build();

        Document savedDocument = documentRepository.save(document);

        DocumentMember ownerMember = DocumentMember.builder()
                .document(savedDocument)
                .user(loggedUser)
                .role(DocumentRole.OWNER)
                .addedBy(loggedUser)
                .build();

        documentMemberRepository.save(ownerMember);

        DocumentFileStorageService.StoredFileData storedFile =
                documentFileStorageService.saveFile(savedDocument.getId(), 1, file);

        DocumentVersion firstVersion = DocumentVersion.builder()
                .document(savedDocument)
                .versionNumber(1)
                .filePath(storedFile.filePath())
                .originalFileName(storedFile.originalFileName())
                .contentType(storedFile.contentType())
                .fileSize(storedFile.fileSize())
                .status(VersionStatus.DRAFT)
                .createdBy(loggedUser)
                .parentVersion(null)
                .build();

        DocumentVersion savedVersion = documentVersionRepository.save(firstVersion);
        savedDocument.setActiveVersion(savedVersion);
        documentRepository.save(savedDocument);

        return new CreateFirstDocumentResponse(
                savedDocument.getId(),
                savedDocument.getTitle(),
                savedDocument.getDescription(),
                loggedUser.getUsername(),
                DocumentRole.OWNER,
                "Document created successfully"
        );
    }

    @Transactional(readOnly = true)
    public List<DocumentListResponse> getLoggedUserDocuments(String username, String search) {
        List<DocumentMember> memberships =
                documentMemberRepository.findMyDocumentsByUsernameAndSearch(username, search);

        return memberships.stream()
                .map(member -> new DocumentListResponse(
                        member.getDocument().getId(),
                        member.getDocument().getTitle(),
                        member.getDocument().getDescription(),
                        member.getRole().name(),
                        member.getDocument().getCreatedBy().getUsername(),
                        member.getDocument().getActiveVersion() != null
                                ? member.getDocument().getActiveVersion().getVersionNumber()
                                : null,
                        member.getDocument().getActiveVersion() != null
                                ? member.getDocument().getActiveVersion().getOriginalFileName()
                                : null,
                        member.getDocument().getActiveVersion() != null
                                ? member.getDocument().getActiveVersion().getContentType()
                                : null,
                        member.getDocument().getActiveVersion() != null
                                ? member.getDocument().getActiveVersion().getFileSize()
                                : null
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public DocumentDetailsResponse getDocumentDetails(Long documentId, String username) {
        User loggedUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Logged user not found"));

        Document document = documentRepository.findDetailsById(documentId)
                .orElseThrow(() -> new IllegalArgumentException("Document not found"));

        DocumentMember currentMembership = documentMemberRepository.findByDocumentAndUser(document, loggedUser)
                .orElse(null);

        if (loggedUser.getSystemRole() != SystemRole.ADMIN && currentMembership == null) {
            throw new IllegalArgumentException("You don't have access to this document");
        }

        List<DocumentTeamMemberResponse> teamMembers = documentMemberRepository.findAllByDocumentIdWithUser(documentId)
                .stream()
                .map(member -> new DocumentTeamMemberResponse(
                        member.getUser().getId(),
                        member.getUser().getUsername(),
                        member.getUser().getFirstName(),
                        member.getUser().getLastName(),
                        member.getUser().getEmail(),
                        member.getRole().name()
                ))
                .toList();

        DocumentVersion activeVersion = document.getActiveVersion();

        return new DocumentDetailsResponse(
                document.getId(),
                document.getTitle(),
                document.getDescription(),
                document.getCreatedBy().getUsername(),
                currentMembership != null ? currentMembership.getRole().name() : loggedUser.getSystemRole().name(),
                activeVersion != null ? activeVersion.getVersionNumber() : null,
                activeVersion != null ? activeVersion.getId() : null,
                activeVersion != null ? activeVersion.getOriginalFileName() : null,
                activeVersion != null ? activeVersion.getContentType() : null,
                activeVersion != null ? activeVersion.getFileSize() : null,
                teamMembers
        );
    }
}