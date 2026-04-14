package com.example.project.backend.service;
import com.example.project.backend.dto.response.documentVersion.DeleteDocumentResponse;
import com.example.project.backend.dto.response.user.UserActivationResponse;
import com.example.project.backend.dto.response.user.UserDeactivationResponse;
import com.example.project.backend.model.entity.Document;
import com.example.project.backend.model.entity.User;
import com.example.project.backend.model.enums.SystemRole;
import com.example.project.backend.repository.UserRepository;
import com.example.project.backend.dto.response.documentVersion.DeleteDocumentVersionResponse;
import com.example.project.backend.model.entity.DocumentVersion;
import com.example.project.backend.repository.DocumentRepository;
import com.example.project.backend.repository.DocumentVersionRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final DocumentRepository documentRepository;
    private final DocumentVersionRepository documentVersionRepository;

    @Transactional
    public UserDeactivationResponse deactivateUser(Long userId, String adminUsername) {
        User admin = userRepository.findByUsername(adminUsername)
                .orElseThrow(() -> new IllegalArgumentException("Admin not found."));

        if (admin.getSystemRole() != SystemRole.ADMIN) {
            throw new IllegalArgumentException("Only admins can deactivate users.");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found."));

        user.setActive(false);

        return new UserDeactivationResponse(
                user.getId(),
                user.getUsername(),
                user.isActive(),
                "User account is deactivated successfully. "
        );
    }

    @Transactional
    public UserActivationResponse activateUser(Long userId, String adminUsername) {
        User admin = userRepository.findByUsername(adminUsername)
                .orElseThrow(() -> new IllegalArgumentException("Admin not found."));

        if (admin.getSystemRole() != SystemRole.ADMIN) {
            throw new IllegalArgumentException(" Only admins can activate users.");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found."));

        user.setActive(true);

        return new UserActivationResponse(
                user.getId(),
                user.getUsername(),
                user.isActive(),
                "User account is activated successfully."
        );
    }

    @Transactional
    public DeleteDocumentVersionResponse deleteDocumentVersion(Long versionId, String adminUsername) {
        User admin = userRepository.findByUsername(adminUsername)
                .orElseThrow(() -> new IllegalArgumentException("Admin not found."));

        if (admin.getSystemRole() != SystemRole.ADMIN) {
            throw new IllegalArgumentException("Only admins can delete document versions.");
        }

        DocumentVersion version = documentVersionRepository.findById(versionId)
                .orElseThrow(() -> new IllegalArgumentException("Document version not found."));

        Document document = version.getDocument();

        long versionCount = documentVersionRepository.countByDocument(document);
        if (versionCount == 1) {
            throw new IllegalArgumentException(
                    "Cannot delete the only version of a document. Delete the whole document instead."
            );
        }

        boolean isParentOfAnotherVersion = documentVersionRepository.existsByParentVersion(version);
        if (isParentOfAnotherVersion) {
            throw new IllegalArgumentException(
                    "Cannot delete a version that is parent of another version."
            );
        }

        if (document.getActiveVersion() != null &&
                document.getActiveVersion().getId().equals(version.getId())) {
            document.setActiveVersion(version.getParentVersion());
            documentRepository.save(document);
        }

        Long deletedVersionId = version.getId();
        Long documentId = document.getId();
        Integer versionNumber = version.getVersionNumber();

        documentVersionRepository.delete(version);

        return new DeleteDocumentVersionResponse(
                deletedVersionId,
                documentId,
                versionNumber,
                "Document version deleted successfully."
        );
    }
    @Transactional
    public DeleteDocumentResponse deleteDocument(Long documentId, String adminUsername) {
        User admin = userRepository.findByUsername(adminUsername)
                .orElseThrow(() -> new IllegalArgumentException("Admin not found."));

        if (admin.getSystemRole() != SystemRole.ADMIN) {
            throw new IllegalArgumentException("Only admins can delete documents.");
        }

        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new IllegalArgumentException("Document not found."));

        Long deletedDocumentId = document.getId();
        String deletedTitle = document.getTitle();

        documentRepository.delete(document);

        return new DeleteDocumentResponse(
                deletedDocumentId,
                deletedTitle,
                "Document deleted successfully."
        );
    }
}


