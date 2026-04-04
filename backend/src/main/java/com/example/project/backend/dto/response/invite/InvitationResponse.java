package com.example.project.backend.dto.response.invite;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class InvitationResponse {
    private Long id;
    private Long documentId;
    private String documentTitle;
    private String senderUsername;
    private String recipientUsername;
    private String role;
    private String status;
    private String message;
}