package com.example.project.backend.repository;

import com.example.project.backend.model.entity.Document;
import com.example.project.backend.model.entity.DocumentInvitation;
import com.example.project.backend.model.entity.User;
import com.example.project.backend.model.enums.InvitationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DocumentInvitationRepository extends JpaRepository<DocumentInvitation, Long> {
    List<DocumentInvitation> findByRecipientAndStatus(User user, InvitationStatus status);

    boolean existsByDocumentAndRecipientAndStatus(Document document, User recipient, InvitationStatus invitationStatus);
}