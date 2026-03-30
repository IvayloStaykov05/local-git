package com.example.project.backend.dto.response.documentVersion;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class DocumentVersionHistoryResponse {
    private Long versionId;
    private Integer versionNumber;
    private String status;
    private String createdBy;
    private String approvedBy;
    private String rejectedBy;
    private LocalDateTime createdAt;
    private LocalDateTime approvedAt;
    private LocalDateTime rejectedAt;
}
