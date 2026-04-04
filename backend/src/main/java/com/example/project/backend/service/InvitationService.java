package com.example.project.backend.service;

import com.example.project.backend.dto.request.invite.InviteUserRequest;
import com.example.project.backend.dto.response.invite.ActionResponse;
import com.example.project.backend.dto.response.invite.InvitationResponse;
import com.example.project.backend.model.entity.Document;
import com.example.project.backend.model.entity.DocumentInvitation;
import com.example.project.backend.model.entity.DocumentMember;
import com.example.project.backend.model.entity.User;
import com.example.project.backend.model.enums.DocumentRole;
import com.example.project.backend.model.enums.InvitationStatus;
import com.example.project.backend.model.enums.NotificationType;
import com.example.project.backend.repository.DocumentInvitationRepository;
import com.example.project.backend.repository.DocumentMemberRepository;
import com.example.project.backend.repository.DocumentRepository;
import com.example.project.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class InvitationService {

    private final DocumentInvitationRepository invitationRepository;
    private final DocumentRepository documentRepository;
    private final DocumentMemberRepository documentMemberRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final DocumentMemberService documentMemberService;

    @Transactional
    public InvitationResponse inviteUser(String ownerUsername, InviteUserRequest request) {
        User owner = userRepository.findByUsername(ownerUsername)
                .orElseThrow(() -> new IllegalArgumentException("Logged user not found"));

        User targetUser = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("Target user not found"));

        Document document = documentRepository.findById(request.getDocumentId())
                .orElseThrow(() -> new IllegalArgumentException("Document not found"));

        DocumentMember ownerMember = documentMemberRepository.findByDocumentAndUser(document, owner)
                .orElseThrow(() -> new IllegalArgumentException("You do not have access to this document"));

        if (ownerMember.getRole() != DocumentRole.OWNER) {
            throw new IllegalArgumentException("Only the owner can invite users");
        }

        if (owner.getId().equals(targetUser.getId())) {
            throw new IllegalArgumentException("You cannot invite yourself");
        }

        documentMemberRepository.findByDocumentAndUser(document, targetUser)
                .ifPresent(member -> {
                    throw new IllegalArgumentException("This user is already a member of the document");
                });

        boolean hasPendingInvitation = invitationRepository
                .existsByDocumentAndRecipientAndStatus(document, targetUser, InvitationStatus.PENDING);

        if (hasPendingInvitation) {
            throw new IllegalArgumentException("This user already has a pending invitation for this document");
        }

        DocumentRole invitedRole;
        try {
            invitedRole = DocumentRole.valueOf(request.getRole().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Invalid document role");
        }

        DocumentInvitation invitation = DocumentInvitation.builder()
                .document(document)
                .sender(owner)
                .recipient(targetUser)
                .role(invitedRole)
                .status(InvitationStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();

        DocumentInvitation savedInvitation = invitationRepository.save(invitation);

        notificationService.send(
                targetUser,
                owner,
                owner.getUsername() + " invited you to join document \"" + document.getTitle() + "\" as " + invitedRole.name(),
                NotificationType.ROLE_REQUEST
        );

        return new InvitationResponse(
                savedInvitation.getId(),
                document.getId(),
                document.getTitle(),
                owner.getUsername(),
                targetUser.getUsername(),
                savedInvitation.getRole().name(),
                savedInvitation.getStatus().name(),
                "Invitation sent successfully"
        );
    }

    @Transactional
    public ActionResponse accept(Long invitationId, String username) {
        User loggedUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Logged user not found"));

        DocumentInvitation invitation = invitationRepository.findById(invitationId)
                .orElseThrow(() -> new IllegalArgumentException("Invitation not found"));

        if (!invitation.getRecipient().getId().equals(loggedUser.getId())) {
            throw new IllegalArgumentException("You cannot accept this invitation");
        }

        if (invitation.getStatus() != InvitationStatus.PENDING) {
            throw new IllegalArgumentException("This invitation is no longer pending");
        }

        documentMemberService.addUserToDocument(
                invitation.getDocument(),
                loggedUser,
                invitation.getRole(),
                invitation.getSender()
        );


        invitation.setStatus(InvitationStatus.ACCEPTED);

        notificationService.send(
                invitation.getSender(),
                loggedUser,
                loggedUser.getUsername() + " accepted your invitation for document \"" +
                        invitation.getDocument().getTitle() + "\"",
                NotificationType.ROLE_ACCEPTED
        );

        return new ActionResponse("Invitation accepted successfully");
    }

    @Transactional
    public ActionResponse reject(Long invitationId, String username) {
        User loggedUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Logged user not found"));

        DocumentInvitation invitation = invitationRepository.findById(invitationId)
                .orElseThrow(() -> new IllegalArgumentException("Invitation not found"));

        if (!invitation.getRecipient().getId().equals(loggedUser.getId())) {
            throw new IllegalArgumentException("You cannot reject this invitation");
        }

        if (invitation.getStatus() != InvitationStatus.PENDING) {
            throw new IllegalArgumentException("This invitation is no longer pending");
        }

        invitation.setStatus(InvitationStatus.REJECTED);

        notificationService.send(
                invitation.getSender(),
                loggedUser,
                loggedUser.getUsername() + " rejected your invitation for document \"" +
                        invitation.getDocument().getTitle() + "\"",
                NotificationType.ROLE_REJECTED
        );

        return new ActionResponse("Invitation rejected successfully");
    }
}