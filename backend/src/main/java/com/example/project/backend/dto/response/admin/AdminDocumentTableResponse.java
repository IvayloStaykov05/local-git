package com.example.project.backend.dto.response.admin;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class AdminDocumentTableResponse {
    private Long id;
    private String title;
    private String ownerUsername;
    private Long versionsCount;
    private LocalDateTime createdAt;
}