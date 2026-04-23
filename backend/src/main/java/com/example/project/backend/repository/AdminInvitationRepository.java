package com.example.project.backend.repository;

import com.example.project.backend.model.entity.AdminInvitation;
import com.example.project.backend.model.entity.User;
import com.example.project.backend.model.enums.InvitationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AdminInvitationRepository extends JpaRepository<AdminInvitation, Long> {
    boolean existsByRecipientAndStatus(User recipient, InvitationStatus status);

    Optional<AdminInvitation> findFirstByRecipientAndSenderAndStatusOrderByCreatedAtDesc(User recipient, User sender, InvitationStatus status);
}