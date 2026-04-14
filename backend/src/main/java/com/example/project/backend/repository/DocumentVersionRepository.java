package com.example.project.backend.repository;

import com.example.project.backend.model.entity.Document;
import com.example.project.backend.model.entity.DocumentVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DocumentVersionRepository extends JpaRepository<DocumentVersion, Long>  {

    Optional <DocumentVersion> findByIdAndDocumentId (Long versionId, Long documentId);
    List <DocumentVersion> findAllByDocumentIdOrderByVersionNumberDesc(Long documentId);

    boolean existsByParentVersion(DocumentVersion parentVersion);
    long countByDocument(Document document);

}
