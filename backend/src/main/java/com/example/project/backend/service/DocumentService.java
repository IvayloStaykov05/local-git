package com.example.project.backend.service;

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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final DocumentVersionRepository documentVersionRepository;
    private final DocumentMemberRepository documentMemberRepository;
    private final UserRepository userRepository;

    @Transactional
    public CreateFirstDocumentResponse createFirstDocument(CreateFirstDocumentRequest request, String username) {

        User loggedUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Logged user not found"));

        Document document = Document.builder()
                .title(request.getTitle())
                .description(request.getDescription())
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


        DocumentVersion firstVersion = DocumentVersion.builder()
                .document(savedDocument)
                .versionNumber(1)
                .content(request.getContent())
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
    public List<DocumentListResponse> getLoggedUserDocuments(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<DocumentMember> memberships =
                documentMemberRepository.findAllByUserWithDocumentCreatorAndActiveVersion(user);

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
                                ? member.getDocument().getActiveVersion().getContent()
                                : null
                ))
                .toList();
    }
}
