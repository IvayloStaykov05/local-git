package com.example.project.backend.controller;

import com.example.project.backend.dto.request.user.UserActivationRequest;
import com.example.project.backend.dto.request.user.UserDeactivationRequest;
import com.example.project.backend.dto.response.documentVersion.DeleteDocumentResponse;
import com.example.project.backend.dto.response.user.UserActivationResponse;
import com.example.project.backend.dto.response.user.UserDeactivationResponse;
import com.example.project.backend.service.AdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.example.project.backend.dto.response.documentVersion.DeleteDocumentVersionResponse;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;

    @PatchMapping("/users/deactivate")
    public ResponseEntity<UserDeactivationResponse> deactivateUser(
            @RequestBody @Valid UserDeactivationRequest request,
            Authentication authentication
    ){
        return ResponseEntity.ok(
                adminService.deactivateUser(request.getUserId(), authentication.getName())
        );
    }

    @PatchMapping("/users/activate")
    public ResponseEntity<UserActivationResponse> activateUser(
            @RequestBody @Valid UserActivationRequest request,
            Authentication authentication
    ){
        return  ResponseEntity.ok(
                adminService.activateUser(request.getUserId(), authentication.getName())
        );
    }

    @DeleteMapping("/documents/versions/{versionId}")
    public ResponseEntity<DeleteDocumentVersionResponse> deleteDocumentVersion(
            @PathVariable Long versionId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(
                adminService.deleteDocumentVersion(versionId, authentication.getName())
        );
    }

    @DeleteMapping("/documents/{documentId}")
    public ResponseEntity<DeleteDocumentResponse> deleteDocument(
            @PathVariable Long documentId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(
                adminService.deleteDocument(documentId, authentication.getName())
        );
    }

}
