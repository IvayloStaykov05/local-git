package com.example.project.backend.repository;


import com.example.project.backend.model.entity.Document;
import com.example.project.backend.model.entity.DocumentMember;
import com.example.project.backend.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
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

}
