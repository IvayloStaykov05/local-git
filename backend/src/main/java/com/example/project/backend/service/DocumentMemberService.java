package com.example.project.backend.service;

import com.example.project.backend.dto.request.documentMember.CreateDocumentMemberRequest;
import com.example.project.backend.dto.request.documentMember.DeleteDocumentMemberRequest;
import com.example.project.backend.dto.response.documentMember.CreateDocumentMemberResponse;
import com.example.project.backend.dto.response.documentMember.DeleteDocumentMemberResponse;
import com.example.project.backend.dto.response.documentMember.SharedUserResponse;
import com.example.project.backend.model.entity.Document;
import com.example.project.backend.model.entity.DocumentMember;
import com.example.project.backend.model.entity.User;
import com.example.project.backend.model.enums.DocumentRole;
import com.example.project.backend.model.enums.SystemRole;
import com.example.project.backend.repository.DocumentMemberRepository;
import com.example.project.backend.repository.DocumentRepository;
import com.example.project.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DocumentMemberService {

    private final DocumentRepository documentRepository;
    private final DocumentMemberRepository documentMemberRepository;
    private final UserRepository userRepository;

    @Transactional
    public DeleteDocumentMemberResponse deleteDocumentMember(DeleteDocumentMemberRequest request, String username){

        User loggedUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("No logged user found"));

        Document document = documentRepository.findById(request.getDocumentId())
                .orElseThrow(() -> new IllegalArgumentException("No document found"));

        //Role of the logged user in the document
        DocumentMember loggedMember = null;
        if(loggedUser.getSystemRole() != SystemRole.ADMIN){
            loggedMember = documentMemberRepository.findByDocumentAndUser(document, loggedUser)
                    .orElseThrow(() -> new IllegalArgumentException("You don't have access to this document"));
        }

        if(loggedUser.getSystemRole() != SystemRole.ADMIN && loggedMember.getRole() != DocumentRole.OWNER){
            throw new IllegalArgumentException("You don't have the rights to change roles for this document");
        }

        //The one we will remove from the document
        User member = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        DocumentMember documentMember = documentMemberRepository.findByDocumentAndUser(document, member)
                .orElseThrow(() -> new IllegalArgumentException("The user has no role in the document"));

        if(loggedMember == documentMember){
            throw new IllegalArgumentException("You can't remove yourself from the document");
        }

        DeleteDocumentMemberResponse response = new DeleteDocumentMemberResponse(
                member.getUsername(),
                documentMember.getRole().toString(),
                document.getId(),
                "You removed the user successfully"
        );

        documentMemberRepository.delete(documentMember);

        return response;

    }

    @Transactional(readOnly = true)
    public List<SharedUserResponse> getSharedUsers(String username) {
        List<User> users = documentMemberRepository.findDistinctSharedUsersByUsername(username);

        return users.stream()
                .map(user -> new SharedUserResponse(
                        user.getId(),
                        user.getUsername(),
                        user.getFirstName(),
                        user.getLastName(),
                        user.getEmail()
                ))
                .toList();
    }

    @Transactional
    public DocumentMember addUserToDocument(
            Document document,
            User user,
            DocumentRole role,
            User addedBy
    ) {

        if (documentMemberRepository.findByDocumentAndUser(document, user).isPresent()) {
            throw new IllegalArgumentException("User is already a member of this document");
        }

        DocumentMember newMember = DocumentMember.builder()
                .document(document)
                .user(user)
                .role(role)
                .addedBy(addedBy)
                .build();

        return documentMemberRepository.save(newMember);
    }
}
