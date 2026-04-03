package com.example.project.backend.repository;


import com.example.project.backend.model.entity.Document;
import com.example.project.backend.model.entity.DocumentMember;
import com.example.project.backend.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DocumentMemberRepository extends JpaRepository<DocumentMember, Long> {
    Optional<DocumentMember> findByDocumentAndUser(Document document, User user);

    @Query("""
        SELECT dm
        FROM DocumentMember dm
        JOIN FETCH dm.document d
        JOIN FETCH d.createdBy
        LEFT JOIN FETCH d.activeVersion
        WHERE dm.user = :user
    """)
    List<DocumentMember> findAllByUserWithDocumentCreatorAndActiveVersion(User user);

    @Query("""
        SELECT DISTINCT sharedMember.user
        FROM DocumentMember loggedMember
        JOIN loggedMember.document d
        JOIN d.members sharedMember
        WHERE loggedMember.user.username = :username
          AND sharedMember.user.username <> :username
    """)
    List<User> findDistinctSharedUsersByUsername(String username);

    @Query("""
    SELECT DISTINCT dm
    FROM DocumentMember dm
    JOIN FETCH dm.document d
    JOIN FETCH d.createdBy
    LEFT JOIN FETCH d.activeVersion av
    WHERE dm.user.username = :username
      AND (
            :search IS NULL
            OR :search = ''
            OR LOWER(d.title) LIKE LOWER(CONCAT('%', :search, '%'))
          )
    """)
    List<DocumentMember> findMyDocumentsByUsernameAndSearch(
            @Param("username") String username,
            @Param("search") String search
    );

    @Query("""
    SELECT dm
    FROM DocumentMember dm
    JOIN FETCH dm.user u
    WHERE dm.document.id = :documentId
    ORDER BY CASE dm.role
        WHEN com.example.project.backend.model.enums.DocumentRole.OWNER THEN 0
        WHEN com.example.project.backend.model.enums.DocumentRole.AUTHOR THEN 1
        WHEN com.example.project.backend.model.enums.DocumentRole.REVIEWER THEN 2
        ELSE 3
    END, u.username
    """)
    List<DocumentMember> findAllByDocumentIdWithUser(@Param("documentId") Long documentId);
}
