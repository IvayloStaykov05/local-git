package com.example.project.backend.controller;

import com.example.project.backend.dto.response.document.CreateFirstDocumentResponse;
import com.example.project.backend.dto.response.document.DocumentDetailsResponse;
import com.example.project.backend.dto.response.document.DocumentListResponse;
import com.example.project.backend.service.DocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;

    @PostMapping(value = "/createFirst", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CreateFirstDocumentResponse> createDocument(
            @RequestParam("title") String title,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam("file") MultipartFile file,
            Authentication authentication
    ) {
        CreateFirstDocumentResponse response =
                documentService.createFirstDocument(title, description, file, authentication.getName());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/my")
    public ResponseEntity<List<DocumentListResponse>> getMyDocuments(
            Authentication authentication,
            @RequestParam(required = false) String search
    ) {
        List<DocumentListResponse> response =
                documentService.getLoggedUserDocuments(authentication.getName(), search);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{documentId}")
    public ResponseEntity<DocumentDetailsResponse> getDocumentDetails(
            @PathVariable Long documentId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(documentService.getDocumentDetails(documentId, authentication.getName()));
    }
}