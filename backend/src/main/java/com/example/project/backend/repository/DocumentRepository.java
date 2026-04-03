package com.example.project.backend.repository;

import com.example.project.backend.model.entity.Document;
import com.example.project.backend.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

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
}
