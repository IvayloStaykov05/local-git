package com.example.project.backend.dto.response.documentMember;

import com.example.project.backend.model.enums.DocumentRole;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CreateDocumentMemberResponse {
    private Long id;
    private DocumentRole role;
    private String username;
    private String title;
    private String message;
}
