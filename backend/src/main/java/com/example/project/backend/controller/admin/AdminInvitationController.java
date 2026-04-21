package com.example.project.backend.controller.admin;

import com.example.project.backend.dto.request.admin.CreateAdminProfileRequest;
import com.example.project.backend.dto.request.admin.InviteAdminRequest;
import com.example.project.backend.dto.response.admin.AdminInvitationResponse;
import com.example.project.backend.dto.response.admin.CreateAdminProfileResponse;
import com.example.project.backend.dto.response.invite.ActionResponse;
import com.example.project.backend.service.AdminInvitationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin-invitations")
@RequiredArgsConstructor
public class AdminInvitationController {

    private final AdminInvitationService adminInvitationService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AdminInvitationResponse> inviteToBecomeAdmin(
            @RequestBody @Valid InviteAdminRequest request,
            Authentication authentication
    ){
        return ResponseEntity.ok(
                adminInvitationService.inviteToBecomeAdmin(authentication.getName(), request)
        );
    }

    @PostMapping("/{invitationId}/accept")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ActionResponse> acceptAdminInvitation(
            @PathVariable Long invitationId,
            Authentication authentication
    ){

        return ResponseEntity.ok(
                adminInvitationService.acceptAdminInvitation(invitationId, authentication.getName())
        );
    }

    @PostMapping("/{invitationId}/reject")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ActionResponse> rejectAdminInvitation(
            @PathVariable Long invitationId,
            Authentication authentication
    ){
        return ResponseEntity.ok(
                adminInvitationService.rejectAdminInvitation(invitationId, authentication.getName())
        );
    }

    @PostMapping("/create-profile")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CreateAdminProfileResponse> createAdminProfile(
            @RequestBody @Valid CreateAdminProfileRequest request,
            Authentication authentication
    ){
        return ResponseEntity.ok(
                adminInvitationService.createAdminProfile(authentication.getName(), request)
        );
    }
}
