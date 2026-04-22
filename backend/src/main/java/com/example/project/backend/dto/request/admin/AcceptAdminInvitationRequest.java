package com.example.project.backend.dto.request.admin;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AcceptAdminInvitationRequest {
    private String adminUsername;
    private String adminEmail;
    private String adminPassword;
}