package com.example.project.backend.dto.request.documentVersion;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ApproveDocumentVersionRequest {

    @NotNull(message = "Document Id is required")
    private Long documentId;

    @NotNull(message = "Version Id is required")
    private Long versionId;

    @Size(max = 1000, message = "Comment must be at most 1000 characters")
    private String comment;

}
