package com.example.project.backend.dto.response.admin;

import com.example.project.backend.model.enums.SystemRole;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CreateAdminProfileResponse {

    private Long adminProfileId;
    private String adminUsername;
    private String firstName;
    private String lastName;
    private String adminEmail;
    private SystemRole systemRole;
    private String message;
}