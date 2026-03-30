package com.example.project.backend.dto.response.document;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class DocumentListResponse {

    private Long id;
    private String title;
    private String description;
    private String role;
    private String createdBy;
    private Integer activeVersionNumber;
    private String content;
}
