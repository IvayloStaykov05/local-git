package com.example.project.backend.dto.response.document;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class DocumentDetailsResponse {
    private Long id;
    private String title;
    private String description;
    private String createdBy;
    private String currentUserRole;
    private Integer activeVersionNumber;
    private Long activeVersionId;
    private String activeFileName;
    private String activeContentType;
    private Long activeFileSize;
    private List<DocumentTeamMemberResponse> teamMembers;
}