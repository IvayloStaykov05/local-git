package com.example.project.backend.service;

import com.example.project.backend.dto.response.admin.AdminDocumentTableResponse;
import com.example.project.backend.dto.response.documentVersion.DeleteDocumentResponse;
import com.example.project.backend.dto.response.documentVersion.DeleteDocumentVersionResponse;
import com.example.project.backend.dto.response.user.UserActivationResponse;
import com.example.project.backend.dto.response.user.UserDeactivationResponse;
import com.example.project.backend.dto.response.user.UserSearchResponse;
import com.example.project.backend.model.entity.Document;
import com.example.project.backend.model.entity.DocumentVersion;
import com.example.project.backend.model.entity.User;
import com.example.project.backend.model.enums.SystemRole;
import com.example.project.backend.repository.DocumentRepository;
import com.example.project.backend.repository.DocumentVersionRepository;
import com.example.project.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final DocumentRepository documentRepository;
    private final DocumentVersionRepository documentVersionRepository;
    private static final Logger logger = LoggerFactory.getLogger(AdminService.class);

    private User getValidatedAdmin(String adminUsername, String errorMsgPrefix) {
        User admin = userRepository.findByUsername(adminUsername)
                .orElseThrow(() -> {
                    logger.error("{} admin with username {} not found", errorMsgPrefix, adminUsername);
                    return new IllegalArgumentException("Admin not found.");
                });

        if (admin.getSystemRole() != SystemRole.ADMIN) {
            logger.error("{} user with id {} is not an admin", errorMsgPrefix, admin.getId());
            throw new IllegalArgumentException("Only admins can access this resource.");
        }

        return admin;
    }

    @Transactional(readOnly = true)
    public List<AdminDocumentTableResponse> getAdminDocuments(String adminUsername, String search) {
        User admin = getValidatedAdmin(adminUsername, "Cannot load admin documents -");
        logger.info("Admin with id {} loaded admin documents table with search '{}'", admin.getId(), search);
        return documentRepository.findAllForAdminTable(search);
    }

    @Transactional(readOnly = true)
    public List<UserSearchResponse> searchUsersForAdmin(String adminUsername, String search) {
        User admin = getValidatedAdmin(adminUsername, "Cannot search users for admin -");

        logger.info("Admin with id {} searched users by '{}'", admin.getId(), search);

        return userRepository.searchUsers(search)
                .stream()
                .map(user -> new UserSearchResponse(
                        user.getId(),
                        user.getUsername(),
                        user.getFirstName(),
                        user.getLastName(),
                        user.getEmail()
                ))
                .toList();
    }

    @Transactional
    public UserDeactivationResponse deactivateUser(Long userId, String adminUsername) {
        User admin = getValidatedAdmin(adminUsername, "Cannot deactivate user -");

        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    logger.error("Cannot deactivate user - user with id {} not found", userId);
                    return new IllegalArgumentException("User not found.");
                });

        user.setActive(false);

        logger.info("Admin with id {} successfully deactivated the account of user with id {}", admin.getId(), userId);

        return new UserDeactivationResponse(
                user.getId(),
                user.getUsername(),
                user.isActive(),
                "User account is deactivated successfully."
        );
    }

    @Transactional
    public UserActivationResponse activateUser(Long userId, String adminUsername) {
        User admin = getValidatedAdmin(adminUsername, "Cannot activate user -");

        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    logger.error("Cannot activate user - user with id {} not found", userId);
                    return new IllegalArgumentException("User not found.");
                });

        user.setActive(true);

        logger.info("Admin with id {} successfully activated the account of user with id {}", admin.getId(), userId);

        return new UserActivationResponse(
                user.getId(),
                user.getUsername(),
                user.isActive(),
                "User account is activated successfully."
        );
    }

    @Transactional
    public DeleteDocumentVersionResponse deleteDocumentVersion(Long versionId, String adminUsername) {
        User admin = getValidatedAdmin(adminUsername, "Cannot delete version -");

        DocumentVersion version = documentVersionRepository.findById(versionId)
                .orElseThrow(() -> {
                    logger.error("Cannot delete version - version with id {} not found", versionId);
                    return new IllegalArgumentException("Document version not found.");
                });

        Document document = version.getDocument();

        long versionCount = documentVersionRepository.countByDocument(document);
        if (versionCount == 1) {
            logger.error("Cannot delete version - version with id {} is the only version of document with id {}", versionId, document.getId());
            throw new IllegalArgumentException("Cannot delete the only version of a document. Delete the whole document instead.");
        }

        boolean isParentOfAnotherVersion = documentVersionRepository.existsByParentVersion(version);
        if (isParentOfAnotherVersion) {
            logger.error("Cannot delete version - version with id {} is parent of another version", versionId);
            throw new IllegalArgumentException("Cannot delete a version that is parent of another version.");
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

        logger.info("Successfully deleted version with id {} of document with id {}", deletedVersionId, documentId);

        return new DeleteDocumentVersionResponse(
                deletedVersionId,
                documentId,
                versionNumber,
                "Document version deleted successfully."
        );
    }

    @Transactional
    public DeleteDocumentResponse deleteDocument(Long documentId, String adminUsername) {
        User admin = getValidatedAdmin(adminUsername, "Cannot delete document -");

        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> {
                    logger.error("Cannot delete document - document with id {} not found", documentId);
                    return new IllegalArgumentException("Document not found.");
                });

        Long deletedDocumentId = document.getId();
        String deletedTitle = document.getTitle();

        documentRepository.delete(document);

        logger.info("Successfully deleted document with id {}", deletedDocumentId);

        return new DeleteDocumentResponse(
                deletedDocumentId,
                deletedTitle,
                "Document deleted successfully."
        );
    }
}