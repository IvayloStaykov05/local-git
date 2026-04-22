package com.example.project.backend.dto.request.admin;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateAdminProfileRequest {

    @NotBlank(message = "Admin username is required.")
    private String adminUsername;

    @NotBlank(message = "First name is required.")
    private String firstName;

    @NotBlank(message = "Last name is required.")
    private String lastName;

    @NotBlank(message = "Admin email is required.")
    @Email(message = "Invalid email format.")
    private String adminEmail;

    @NotBlank(message = "Admin password is required.")
    private String adminPassword;
}