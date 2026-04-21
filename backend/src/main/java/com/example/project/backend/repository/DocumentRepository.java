package com.example.project.backend.repository;

import com.example.project.backend.dto.response.admin.AdminDocumentTableResponse;
import com.example.project.backend.model.entity.Document;
import com.example.project.backend.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {

    Optional<Document> findByTitleAndCreatedBy(String title, User createdBy);

    @Query("""
        SELECT d
        FROM Document d
        JOIN FETCH d.createdBy
        LEFT JOIN FETCH d.activeVersion
        WHERE d.id = :documentId
    """)
    Optional<Document> findDetailsById(@Param("documentId") Long documentId);

    @Query("""
        SELECT new com.example.project.backend.dto.response.admin.AdminDocumentTableResponse(
            d.id,
            d.title,
            creator.username,
            COUNT(v.id),
            d.createdAt
        )
        FROM Document d
        JOIN d.createdBy creator
        LEFT JOIN d.versions v
        WHERE :search IS NULL
           OR :search = ''
           OR LOWER(d.title) LIKE LOWER(CONCAT('%', :search, '%'))
           OR LOWER(creator.username) LIKE LOWER(CONCAT('%', :search, '%'))
        GROUP BY d.id, d.title, creator.username, d.createdAt
        ORDER BY d.createdAt DESC
    """)
    List<AdminDocumentTableResponse> findAllForAdminTable(@Param("search") String search);
}