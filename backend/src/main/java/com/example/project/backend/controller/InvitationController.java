package com.example.project.backend.controller;

import com.example.project.backend.dto.request.invite.InviteUserRequest;
import com.example.project.backend.dto.response.invite.ActionResponse;
import com.example.project.backend.dto.response.invite.InvitationResponse;
import com.example.project.backend.service.InvitationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/invitations")
@RequiredArgsConstructor
public class InvitationController {

    private final InvitationService invitationService;

    @PostMapping
    public ResponseEntity<InvitationResponse> inviteUser(
            @Valid @RequestBody InviteUserRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(
                invitationService.inviteUser(authentication.getName(), request)
        );
    }

    @PostMapping("/{invitationId}/accept")
    public ResponseEntity<ActionResponse> acceptInvitation(
            @PathVariable Long invitationId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(
                invitationService.accept(invitationId, authentication.getName())
        );
    }

    @PostMapping("/{invitationId}/reject")
    public ResponseEntity<ActionResponse> rejectInvitation(
            @PathVariable Long invitationId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(
                invitationService.reject(invitationId, authentication.getName())
        );
    }
}