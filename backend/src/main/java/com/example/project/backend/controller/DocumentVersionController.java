package com.example.project.backend.controller;

import com.example.project.backend.dto.request.documentVersion.CreateDocumentVersionRequest;
import com.example.project.backend.dto.response.documentVersion.CreateDocumentVersionResponse;
import com.example.project.backend.service.DocumentVersionService;
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
@RequestMapping("/api/documentVersions")
@RequiredArgsConstructor
public class DocumentVersionController {

    private final DocumentVersionService documentVersionService;

    @PostMapping("/createNew")
    public ResponseEntity<CreateDocumentVersionResponse> createVersion(
            @RequestBody @Valid CreateDocumentVersionRequest request,
            Authentication authentication
            ) {
        CreateDocumentVersionResponse response = documentVersionService.createDocumentVersion(request, authentication.getName());
        System.out.println(authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
