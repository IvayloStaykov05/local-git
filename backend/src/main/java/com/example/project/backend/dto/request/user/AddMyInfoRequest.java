package com.example.project.backend.dto.request.user;

import jakarta.persistence.Lob;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AddMyInfoRequest {

    @NotBlank(message = "You must specify your personal information")
    @Lob
    private String info;
}
