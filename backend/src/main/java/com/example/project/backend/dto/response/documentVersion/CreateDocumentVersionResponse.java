package com.example.project.backend.dto.response.documentVersion;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CreateDocumentVersionResponse {
    private Long id;
    private String title;
    private String createdByUsername;
    private Integer version;
    private String message;
}
