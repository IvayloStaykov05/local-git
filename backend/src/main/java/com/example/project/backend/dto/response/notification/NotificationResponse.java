package com.example.project.backend.dto.response.notification;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class NotificationResponse {
    private Long id;
    private String message;
    private String type;
    private boolean isRead;
    private String sender;
    private LocalDateTime createdAt;
    private boolean actionable;
    private Long invitationId;
    private Long documentId;
    private String documentTitle;
    private String role;
}