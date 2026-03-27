package com.example.project.backend.controller;

import com.example.project.backend.config.JwtService;
import com.example.project.backend.dto.request.user.ForgotPasswordRequest;
import com.example.project.backend.dto.request.user.UserLoginRequest;
import com.example.project.backend.dto.request.user.UserRegisterRequest;
import com.example.project.backend.dto.response.user.UserLoginResponse;
import com.example.project.backend.dto.response.user.UserRegisterResponse;
import com.example.project.backend.model.entity.User;
import com.example.project.backend.repository.UserRepository;
import com.example.project.backend.service.CustomUserDetailsService;
import com.example.project.backend.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final CustomUserDetailsService customUserDetailsService;

    @PostMapping("/register")
    public ResponseEntity<UserRegisterResponse> register(
            @RequestBody @Valid UserRegisterRequest request
    ) {
        UserRegisterResponse response = userService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<UserLoginResponse> login(
            @RequestBody @Valid UserLoginRequest request
    ) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUsernameOrEmail(),
                        request.getPassword()
                )
        );

        User user = userRepository.findByUsername(request.getUsernameOrEmail())
                .or(() -> userRepository.findByEmail(request.getUsernameOrEmail()))
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        UserDetails userDetails = customUserDetailsService.loadUserByUsername(user.getUsername());
        String jwtToken = jwtService.generateToken(userDetails);

        UserLoginResponse response = new UserLoginResponse(
                jwtToken,
                "Bearer",
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getSystemRole(),
                "Login successful"
        );

        return ResponseEntity.ok(response);
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<String> forgotPassword(
            @RequestBody @Valid ForgotPasswordRequest request
    ) {
        String message = userService.forgotPassword(request);
        return ResponseEntity.ok(message);
    }
}