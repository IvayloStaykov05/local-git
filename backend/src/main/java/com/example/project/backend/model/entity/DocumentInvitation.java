package com.example.project.backend.model.entity;

import com.example.project.backend.model.enums.DocumentRole;
import com.example.project.backend.model.enums.InvitationStatus;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.ManyToOne;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentInvitation extends BaseEntity{

    @ManyToOne
    private Document document;

    @ManyToOne
    private User sender;

    @ManyToOne
    private User recipient;

    @Enumerated(EnumType.STRING)
    private DocumentRole role;

    @Enumerated(EnumType.STRING)
    private InvitationStatus status;

    private LocalDateTime createdAt;
}
