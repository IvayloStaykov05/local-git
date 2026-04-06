package com.example.project.backend.dto.request.documentVersion;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class HistoryVersionRequest {

    @NotBlank(message = "Content cannot be empty.")
    private String content;
}
