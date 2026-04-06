package com.example.project.backend.controller;

import com.example.project.backend.dto.request.documentVersion.ApproveDocumentVersionRequest;
import com.example.project.backend.dto.request.documentVersion.CreateDocumentVersionRequest;
import com.example.project.backend.dto.request.documentVersion.DocumentVersionHistoryRequest;
import com.example.project.backend.dto.request.documentVersion.RejectDocumentVersionRequest;
import com.example.project.backend.dto.response.documentVersion.*;
import com.example.project.backend.model.entity.DocumentVersion;
import com.example.project.backend.service.DocumentVersionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/documentVersions")
@RequiredArgsConstructor
public class DocumentVersionController {

    private final DocumentVersionService documentVersionService;

    @PostMapping(value = "/createNew", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CreateDocumentVersionResponse> createVersion(
            @RequestParam("documentId") Long documentId,
            @RequestParam("file") MultipartFile file,
            Authentication authentication
    ) {
        CreateDocumentVersionRequest request = new CreateDocumentVersionRequest();
        request.setDocumentId(documentId);

        CreateDocumentVersionResponse response =
                documentVersionService.createDocumentVersion(request, file, authentication.getName());

        return ResponseEntity.status(201).body(response);
    }

    @PatchMapping("/approve")
    public ResponseEntity<ApproveDocumentVersionResponse> approveVersion(
            @RequestBody @Valid ApproveDocumentVersionRequest request,
            Authentication authentication
    ) {
        ApproveDocumentVersionResponse response =
                documentVersionService.approveVersion(request, authentication.getName());

        return ResponseEntity.ok(response);
    }

    @PatchMapping("/reject")
    public ResponseEntity<RejectDocumentVersionResponse> rejectVersion(
            @RequestBody @Valid RejectDocumentVersionRequest request,
            Authentication authentication
    ) {
        RejectDocumentVersionResponse response =
                documentVersionService.rejectVersion(request, authentication.getName());

        return ResponseEntity.ok(response);
    }

    @PostMapping("/history")
    public ResponseEntity<List<DocumentVersionHistoryResponse>> getVersionHistory(
            @RequestBody @Valid DocumentVersionHistoryRequest request
    ) {
        List<DocumentVersionHistoryResponse> response =
                documentVersionService.getVersionHistory(request.getDocumentId());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{documentId}/active")
    public ResponseEntity<DocumentVersionDetailsResponse> getActiveVersion(
            @PathVariable Long documentId,
            Authentication authentication
    ) {
        DocumentVersionDetailsResponse response =
                documentVersionService.getActiveVersion(documentId, authentication.getName());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{documentId}/parent")
    public ResponseEntity<DocumentVersionDetailsResponse> getParentVersion(
            @PathVariable Long documentId,
            Authentication authentication
    ) {
        DocumentVersionDetailsResponse response =
                documentVersionService.getParentVersion(documentId, authentication.getName());

        return ResponseEntity.ok(response);
    }


    @GetMapping("/{documentId}/{versionId}/download")
    public ResponseEntity<ByteArrayResource> downloadVersionFile(
            @PathVariable Long documentId,
            @PathVariable Long versionId
    ) {
        DocumentFileResponse fileResponse =
                documentVersionService.downloadVersionFile(versionId, documentId);

        ByteArrayResource resource = new ByteArrayResource(fileResponse.getContent());

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment()
                                .filename(fileResponse.getFileName())
                                .build()
                                .toString()
                )
                .contentType(MediaType.parseMediaType(fileResponse.getContentType()))
                .contentLength(fileResponse.getContent().length)
                .body(resource);
    }
}
