package com.example.project.backend.dto.request.documentVersion;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateDocumentVersionRequest {

    @NotBlank(message = "Title is required")
    @Size(max = 200, message = "Title must no exceed 200 characters")
    private String title;

    /*@Size(max = 1000, message = "Description must not exceed 1000 characters")
    private String description;*/
    @NotBlank(message = "Please specify the owner of the document")
    private String username;

    @NotBlank(message = "Content is required")
    private String content;
}
