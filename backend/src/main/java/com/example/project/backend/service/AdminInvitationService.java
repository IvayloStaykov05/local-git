package com.example.project.backend.service;

import com.example.project.backend.dto.request.admin.AcceptAdminInvitationRequest;
import com.example.project.backend.dto.request.admin.CreateAdminProfileRequest;
import com.example.project.backend.dto.request.admin.InviteAdminRequest;
import com.example.project.backend.dto.response.admin.AdminInvitationResponse;
import com.example.project.backend.dto.response.admin.CreateAdminProfileResponse;
import com.example.project.backend.model.entity.AdminInvitation;
import com.example.project.backend.model.entity.User;
import com.example.project.backend.model.enums.InvitationStatus;
import com.example.project.backend.model.enums.NotificationType;
import com.example.project.backend.model.enums.SystemRole;
import com.example.project.backend.repository.AdminInvitationRepository;
import com.example.project.backend.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import com.example.project.backend.dto.response.invite.ActionResponse;


import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AdminInvitationService {

    private final AdminInvitationRepository adminInvitationRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final PasswordEncoder passwordEncoder;
    private static final Logger logger = LoggerFactory.getLogger(AdminInvitationService.class);


    @Transactional
    public AdminInvitationResponse inviteToBecomeAdmin(String adminUsername, InviteAdminRequest request) {
        String errorMsg = "Cannot invite to become admin -";

        User admin = userRepository.findByUsername(adminUsername)
                .orElseThrow(() -> {
                    logger.error("{} admin with username {} not found", errorMsg, adminUsername);
                    return new IllegalArgumentException("Admin not found.");
                });

        if (admin.getSystemRole() != SystemRole.ADMIN) {
            logger.error("{} user with id {} is not an admin", errorMsg, admin.getId());
            throw new IllegalArgumentException("Only admins can send invitations.");
        }

        User recipient = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> {
                    logger.error("{} target user with username {} not found", errorMsg, request.getUsername());
                    return new IllegalArgumentException("Target user not found.");
                });

        if (admin.getId().equals(recipient.getId())) {
            logger.error("{} user with id {} tried to invite themself", errorMsg, admin.getId());
            throw new IllegalArgumentException(("You cannot invite yourself."));
        }
        if (adminInvitationRepository.existsByRecipientAndStatus(recipient, InvitationStatus.PENDING)) {
            logger.error("{} user with id {} already has a pending admin invitation", errorMsg, recipient.getId());
            throw new IllegalArgumentException("This user already has a pending admin invitation");
        }

        AdminInvitation invitation = AdminInvitation.builder()
                .sender(admin)
                .recipient(recipient)
                .status(InvitationStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();

        AdminInvitation savedInvitation = adminInvitationRepository.save(invitation);

        notificationService.send(
                recipient,
                admin,
                admin.getUsername() + " invited you to become an admin",
                NotificationType.ADMIN_REQUEST
        );

        logger.info("Admin with id {} successfully invited user with id {} to become an admin", admin.getId(), recipient.getId());

        return new AdminInvitationResponse(
                savedInvitation.getId(),
                admin.getUsername(),
                recipient.getUsername(),
                savedInvitation.getStatus().name(),
                "Admin invitation sent successfully"
        );

    }
    @Transactional
    public CreateAdminProfileResponse acceptAdminInvitation(
            Long invitationId,
            String username,
            AcceptAdminInvitationRequest request
    ) {
        String errorMsg = "Cannot accept admin invitation -";

        User loggedUser = userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    logger.error("{} logged user with username {} not found", errorMsg, username);
                    return new IllegalArgumentException("Logged user not found");
                });

        AdminInvitation invitation = adminInvitationRepository.findById(invitationId)
                .orElseThrow(() -> {
                    logger.error("{} admin invitation with id {} not found", errorMsg, invitationId);
                    return new IllegalArgumentException("Admin invitation not found");
                });

        if (!invitation.getRecipient().getId().equals(loggedUser.getId())) {
            throw new IllegalArgumentException("You cannot accept this admin invitation");
        }

        if (invitation.getStatus() != InvitationStatus.PENDING) {
            throw new IllegalArgumentException("This admin invitation is no longer pending");
        }

        if (userRepository.existsByLinkedUserAndSystemRole(loggedUser, SystemRole.ADMIN)) {
            throw new IllegalArgumentException("This user already has an admin profile");
        }

        if (userRepository.existsByUsername(request.getAdminUsername())) {
            throw new IllegalArgumentException("Admin username already exists");
        }

        if (userRepository.existsByEmail(request.getAdminEmail())) {
            throw new IllegalArgumentException("Admin email already exists");
        }

        invitation.setStatus(InvitationStatus.ACCEPTED);
        invitation.setProcessedAt(LocalDateTime.now());

        User adminProfile = User.builder()
                .username(request.getAdminUsername())
                .firstName(loggedUser.getFirstName())
                .lastName(loggedUser.getLastName())
                .email(request.getAdminEmail())
                .password(passwordEncoder.encode(request.getAdminPassword()))
                .enabled(true)
                .active(true)
                .systemRole(SystemRole.ADMIN)
                .linkedUser(loggedUser)
                .myInfo(loggedUser.getMyInfo())
                .build();

        User savedAdminProfile = userRepository.save(adminProfile);

        notificationService.send(
                invitation.getSender(),
                loggedUser,
                loggedUser.getUsername() + " accepted your admin invitation",
                NotificationType.ADMIN_ACCEPTED
        );

        return new CreateAdminProfileResponse(
                savedAdminProfile.getId(),
                loggedUser.getId(),
                savedAdminProfile.getUsername(),
                savedAdminProfile.getEmail(),
                "Admin invitation accepted and admin profile created successfully"
        );
    }

    @Transactional
    public ActionResponse rejectAdminInvitation(Long invitationId, String username) {
        String errorMsg = "Cannot reject admin invitation -";

        User loggedUser = userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    logger.error("{} logged user with username {] not found", errorMsg, username);
                    return new IllegalArgumentException("Logged user not found");
                });

        AdminInvitation invitation = adminInvitationRepository.findById(invitationId)
                .orElseThrow(() -> {
                    logger.error("{} admin invitation with id {} not found", errorMsg, invitationId);
                    return new IllegalArgumentException("Admin invitation not found");
                });

        if (!invitation.getRecipient().getId().equals(loggedUser.getId())) {
            logger.error("{} logged user id {} does not match the recipient id {} of admin invitation with id {}", errorMsg, loggedUser.getId(), invitation.getRecipient().getId(), invitationId);
            throw new IllegalArgumentException("You cannot reject this admin invitation");
        }

        if (invitation.getStatus() != InvitationStatus.PENDING) {
            logger.error("{} admin invitation with id {} is no longer pending", errorMsg, invitationId);
            throw new IllegalArgumentException("This admin invitation is no longer pending");
        }

        invitation.setStatus(InvitationStatus.REJECTED);
        invitation.setProcessedAt(LocalDateTime.now());

        notificationService.send(
                invitation.getSender(),
                loggedUser,
                loggedUser.getUsername() + " rejected your admin invitation",
                NotificationType.ADMIN_REJECTED
        );

        logger.info("User with id {} successfully rejected admin invitation with id {}", loggedUser.getId(), invitationId);

        return new ActionResponse("Admin invitation rejected successfully");
    }

    @Transactional
    public CreateAdminProfileResponse createAdminProfile(String adminUsername, CreateAdminProfileRequest request) {
        String errorMsg = "Cannot create admin profile -";

        User admin = userRepository.findByUsername(adminUsername)
                .orElseThrow(() -> {
                    logger.error("{} admin with username {} not found", errorMsg, adminUsername);
                    return new IllegalArgumentException("Admin not found");
                });

        if (admin.getSystemRole() != SystemRole.ADMIN) {
            logger.error("{} user with id {} is not an admin", errorMsg, admin.getId());
            throw new IllegalArgumentException("Only admins can create admin profiles");
        }

        AdminInvitation invitation = adminInvitationRepository.findById(request.getInvitationId())
                .orElseThrow(() -> {
                    logger.error("{} admin invitation with id {} not found", errorMsg, request.getInvitationId());
                    return new IllegalArgumentException("Admin invitation not found");
                });

        if (invitation.getStatus() != InvitationStatus.ACCEPTED) {
            logger.error("{} admin invitation with id {} is not yet accepted", errorMsg, invitation.getId());
            throw new IllegalArgumentException("The admin invitation must be accepted first");
        }

        User baseUser = invitation.getRecipient();

        if (userRepository.existsByLinkedUserAndSystemRole(baseUser, SystemRole.ADMIN)) {
            logger.error("{} user with id {} already has an admin profile", errorMsg, baseUser.getId());
            throw new IllegalArgumentException("This user already has an admin profile");
        }

        if (userRepository.existsByUsername(request.getAdminUsername())) {
            logger.error("{} admin username {} already exists", errorMsg, request.getAdminUsername());
            throw new IllegalArgumentException("Admin username already exists");
        }

        if (userRepository.existsByEmail(request.getAdminEmail())) {
            logger.error("{} admin email {} already exists", errorMsg, request.getAdminEmail());
            throw new IllegalArgumentException("Admin email already exists");
        }

        User adminProfile = User.builder()
                .username(request.getAdminUsername())
                .firstName(baseUser.getFirstName())
                .lastName(baseUser.getLastName())
                .email(request.getAdminEmail())
                .password(passwordEncoder.encode(request.getAdminPassword()))
                .enabled(true)
                .active(true)
                .systemRole(SystemRole.ADMIN)
                .linkedUser(baseUser)
                .myInfo(baseUser.getMyInfo())
                .build();

        User savedAdminProfile = userRepository.save(adminProfile);

        logger.info("Admin profile of user with id {} successfully created by admin with id {}", baseUser.getId(), admin.getId());

        return new CreateAdminProfileResponse(
                savedAdminProfile.getId(),
                baseUser.getId(),
                savedAdminProfile.getUsername(),
                savedAdminProfile.getEmail(),
                "Admin profile created successfully"
        );
    }
}