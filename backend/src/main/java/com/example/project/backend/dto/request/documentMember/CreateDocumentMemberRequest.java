package com.example.project.backend.dto.request.documentMember;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateDocumentMemberRequest {

    @NotBlank(message = "You must specify the title of the document")
    private String title;

    @NotBlank(message = "You must specify the role")
    private String role;

    @NotBlank(message = "You must specify the author of the document")
    private String author;

    @NotBlank(message = "You must specify the user")
    private String username;

}
