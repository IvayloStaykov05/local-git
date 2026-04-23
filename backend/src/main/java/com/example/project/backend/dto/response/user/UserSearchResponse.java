package com.example.project.backend.dto.response.user;

import com.example.project.backend.model.enums.SystemRole;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class UserSearchResponse {
    private Long id;
    private String username;
    private String firstName;
    private String lastName;
    private String email;
    private SystemRole systemRole;
    private String myInfo;
    private boolean active;
}