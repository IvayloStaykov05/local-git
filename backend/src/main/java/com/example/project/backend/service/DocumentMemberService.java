package com.example.project.backend.service;

import com.example.project.backend.dto.request.documentMember.CreateDocumentMemberRequest;
import com.example.project.backend.dto.response.documentMember.CreateDocumentMemberResponse;
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
    public CreateDocumentMemberResponse createDocumentMember(CreateDocumentMemberRequest request, String username){

        //Logged user
        User loggedUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Logged user not found"));

        //Owner of the document
        User owner = userRepository.findByUsername(request.getOwner())
                .orElseThrow(() -> new IllegalArgumentException("Logged user not found"));

        //The user who will be granted a role
        User userMember = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("Logged user not found"));

        Document document = documentRepository.findByTitleAndCreatedBy(request.getTitle(), owner)
                .orElseThrow(() -> new IllegalArgumentException("Document not found"));

        //Role of the logged user in the document
        DocumentMember loggedMember = null;
        if(loggedUser.getSystemRole() != SystemRole.ADMIN){
            loggedMember = documentMemberRepository.findByDocumentAndUser(document, loggedUser)
                    .orElseThrow(() -> new IllegalArgumentException("You don't have access to this document"));
        }


        if(loggedUser.getSystemRole() != SystemRole.ADMIN && loggedMember.getRole() != DocumentRole.OWNER){
            throw new IllegalArgumentException("You don't have the rights to change roles for this document");
        }

        DocumentMember newMember = DocumentMember.builder()
                .document(document)
                .user(userMember)
                .role(DocumentRole.valueOf(request.getRole().toUpperCase()))
                .addedBy(loggedUser)
                .build();

        DocumentMember savedMember = documentMemberRepository.save(newMember);

        return new CreateDocumentMemberResponse(
                savedMember.getId(),
                savedMember.getRole(),
                savedMember.getUser().getUsername(),
                document.getTitle(),
                "Document role added successfully"
        );
    }

    @Transactional(readOnly = true)
    public List<SharedUserResponse> getSharedUsers(String username, String search) {
        List<User> users = documentMemberRepository
                .findDistinctSharedUsersByUsernameAndSearch(username, search);

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
}
