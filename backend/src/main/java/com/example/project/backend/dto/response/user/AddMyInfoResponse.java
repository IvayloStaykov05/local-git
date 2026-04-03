package com.example.project.backend.dto.response.user;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AddMyInfoResponse {
    private Long id;
    private String username;
    private String message;
}
