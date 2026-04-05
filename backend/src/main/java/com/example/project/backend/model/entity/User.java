package com.example.project.backend.model.entity;

import com.example.project.backend.model.enums.SystemRole;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "users")
public class User extends BaseEntity{

    @Column(nullable = false, unique = true, length = 100)
    private String username;

    @Column(name = "first_name")
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private boolean enabled = false;

    @Lob
    @Column(name = "my_info", columnDefinition = "LONGTEXT")
    private String myInfo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private SystemRole systemRole = SystemRole.USER;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "createdBy")
    @Builder.Default
    private List<Document> createdDocuments = new ArrayList<>();

    @OneToMany(mappedBy = "user")
    @Builder.Default
    private List<DocumentMember> memberships = new ArrayList<>();

    @OneToMany(mappedBy = "createdBy")
    @Builder.Default
    private List<DocumentVersion> createdVersions = new ArrayList<>();

    @OneToMany(mappedBy = "approvedBy")
    @Builder.Default
    private List<DocumentVersion> approvedVersions = new ArrayList<>();

    @OneToMany(mappedBy = "user")
    @Builder.Default
    private List<Comment> comments = new ArrayList<>();

    @OneToMany(mappedBy = "addedBy")
    @Builder.Default
    private List<DocumentMember> addedMembers = new ArrayList<>();

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (systemRole == null) {
            systemRole = SystemRole.USER;
        }
    }

}
