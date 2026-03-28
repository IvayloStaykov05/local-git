package com.example.project.backend.service;

import com.example.project.backend.dto.request.documentVersion.CreateDocumentVersionRequest;
import com.example.project.backend.dto.response.documentVersion.CreateDocumentVersionResponse;
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
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DocumentVersionService {

    private final DocumentRepository documentRepository;
    private final DocumentVersionRepository documentVersionRepository;
    private final DocumentMemberRepository documentMemberRepository;
    private final UserRepository userRepository;

    @Transactional
    public CreateDocumentVersionResponse createDocumentVersion(CreateDocumentVersionRequest request, String username){

        User loggedUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Logged user not found"));

        User documentAuthor = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("The author of the document not found"));

        Document document = documentRepository.findByTitleAndCreatedBy(request.getTitle(), documentAuthor)
                .orElseThrow(() -> new IllegalArgumentException("Document not found"));

        DocumentMember documentMember = documentMemberRepository.findByDocumentAndUser(document, loggedUser)
                .orElseThrow(() -> new IllegalArgumentException("You don't have access to the document"));

        if(documentMember.getRole() != DocumentRole.AUTHOR && documentMember.getRole() != DocumentRole.OWNER){
            throw new IllegalArgumentException("You don't have the rights to make changes to this document");
        }

        DocumentVersion newVersion = DocumentVersion.builder()
                .document(document)
                .versionNumber(document.getActiveVersion().getVersionNumber() + 1)
                .content(request.getContent())
                .status(VersionStatus.DRAFT)
                .createdBy(loggedUser)
                .parentVersion(document.getActiveVersion())
                .build();

        DocumentVersion savedVersion = documentVersionRepository.save(newVersion);
        document.setActiveVersion(savedVersion);
        //documentRepository.save(document);

        return new CreateDocumentVersionResponse(
                savedVersion.getId(),
                document.getTitle(),
                loggedUser.getUsername(),
                savedVersion.getVersionNumber(),
                "Document version created successfully"
        );
    }

}
