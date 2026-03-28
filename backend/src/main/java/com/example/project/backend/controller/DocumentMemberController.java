package com.example.project.backend.controller;

import com.example.project.backend.dto.request.documentMember.CreateDocumentMemberRequest;
import com.example.project.backend.dto.response.documentMember.CreateDocumentMemberResponse;
import com.example.project.backend.service.DocumentMemberService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/documentMembers")
@RequiredArgsConstructor
public class DocumentMemberController {

    private final DocumentMemberService documentMemberService;

    @PostMapping("/createNewMember")
    public ResponseEntity<CreateDocumentMemberResponse> createDocumentMember(
            @RequestBody @Valid CreateDocumentMemberRequest request,
            Authentication authentication
            ) {
        CreateDocumentMemberResponse response = documentMemberService.createDocumentMember(request, authentication.getName());
        System.out.println(authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
