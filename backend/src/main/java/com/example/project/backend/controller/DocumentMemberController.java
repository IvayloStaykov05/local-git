package com.example.project.backend.controller;

import com.example.project.backend.dto.request.documentMember.CreateDocumentMemberRequest;
import com.example.project.backend.dto.request.documentMember.DeleteDocumentMemberRequest;
import com.example.project.backend.dto.response.documentMember.CreateDocumentMemberResponse;
import com.example.project.backend.dto.response.documentMember.DeleteDocumentMemberResponse;
import com.example.project.backend.dto.response.documentMember.SharedUserResponse;
import com.example.project.backend.service.DocumentMemberService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/documentMembers")
@RequiredArgsConstructor
public class DocumentMemberController {

    private final DocumentMemberService documentMemberService;


    @DeleteMapping("/deleteMember")
    public ResponseEntity<DeleteDocumentMemberResponse> deleteDocumentMember(
            @RequestBody @Valid DeleteDocumentMemberRequest request,
            Authentication authentication
            ) {
        DeleteDocumentMemberResponse response = documentMemberService.deleteDocumentMember(request, authentication.getName());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/shared-users")
    public ResponseEntity<List<SharedUserResponse>> getSharedUsers(Authentication authentication) {
        List<SharedUserResponse> response =
                documentMemberService.getSharedUsers(authentication.getName());

        return ResponseEntity.ok(response);
    }
}