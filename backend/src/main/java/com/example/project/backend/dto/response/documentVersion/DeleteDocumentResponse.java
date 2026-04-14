package com.example.project.backend.dto.response.documentVersion;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class DeleteDocumentResponse {

    private Long documentId;
    private String title;
    private String message;
}
