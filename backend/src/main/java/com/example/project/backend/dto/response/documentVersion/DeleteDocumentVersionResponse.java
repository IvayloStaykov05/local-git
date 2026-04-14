package com.example.project.backend.dto.response.documentVersion;



import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class DeleteDocumentVersionResponse {

    private Long versionId;
    private Long documentId;
    private Integer versionNumber;
    private String message;
}
