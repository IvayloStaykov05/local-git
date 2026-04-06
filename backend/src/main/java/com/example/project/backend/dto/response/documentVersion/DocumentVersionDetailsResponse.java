package com.example.project.backend.dto.response.documentVersion;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor

public class DocumentVersionDetailsResponse {

    private Long versionId;
    private Long documentId;
    private String documentTitle;
    private Integer versionNumber;
    private String status;
    private String createdBy;
    private String originalFileName;
    private String contentType;
    private Long fileSize;
    private LocalDateTime createdAt;
}
