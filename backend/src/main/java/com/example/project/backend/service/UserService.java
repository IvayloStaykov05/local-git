package com.example.project.backend.service;

import com.example.project.backend.dto.request.user.ForgotPasswordRequest;
import com.example.project.backend.dto.request.user.UserRegisterRequest;
import com.example.project.backend.dto.response.user.AddMyInfoResponse;
import com.example.project.backend.dto.response.user.UpdateMyInfoResponse;
import com.example.project.backend.dto.response.user.UserProfileResponse;
import com.example.project.backend.dto.response.user.UserRegisterResponse;
import com.example.project.backend.dto.response.user.UserSearchResponse;
import com.example.project.backend.model.entity.User;
import com.example.project.backend.model.entity.VerificationToken;
import com.example.project.backend.repository.UserRepository;
import com.example.project.backend.repository.VerificationTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final EmailService emailService;
    private final VerificationTokenRepository verificationTokenRepository;
    private final PasswordEncoder passwordEncoder;

    public UserRegisterResponse register(UserRegisterRequest request) {
        validateRegistration(request);

        User user = buildUser(request);

        user.setEnabled(false);

        User savedUser = userRepository.save(user);

        String token = UUID.randomUUID().toString();

        VerificationToken verificationToken = VerificationToken.builder()
                .token(token)
                .user(savedUser)
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusHours(24))
                .build();

        verificationTokenRepository.save(verificationToken);

        emailService.sendVerificationEmail(savedUser.getEmail(), token);

        return new UserRegisterResponse(
                savedUser.getId(),
                savedUser.getUsername(),
                savedUser.getEmail(),
                "User registered successfully. Please check your email to activate your account."
        );
    }

    public String forgotPassword(ForgotPasswordRequest request) {
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("Passwords do not match");
        }

        User user = userRepository.findByUsername(request.getUsernameOrEmail())
                .or(() -> userRepository.findByEmail(request.getUsernameOrEmail()))
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        return "Password changed successfully";
    }

    private void validateRegistration(UserRegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Username already exists");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already exists");
        }
    }

    private User buildUser(UserRegisterRequest request) {
        return User.builder()
                .username(request.getUsername())
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .build();
    }

    @Transactional(readOnly = true)
    public List<UserSearchResponse> searchUsers(String search) {
        return userRepository.searchUsers(search)
                .stream()
                .map(user -> new UserSearchResponse(
                        user.getId(),
                        user.getUsername(),
                        user.getFirstName(),
                        user.getLastName(),
                        user.getEmail()
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public UserProfileResponse getUserProfile(Long userId, String loggedUsername) {
        userRepository.findByUsername(loggedUsername)
                .orElseThrow(() -> new IllegalArgumentException("Logged user not found"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        return new UserProfileResponse(
                user.getId(),
                user.getUsername(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getSystemRole(),
                user.getMyInfo()
        );
    }

    @Transactional
    public AddMyInfoResponse addMyInfo(String info, String username){
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Logged user not found"));

        user.setMyInfo(info);

        return new AddMyInfoResponse(
                user.getId(),
                user.getUsername(),
                "Personal information added successfully"
        );
    }

    public String getMyInfo(String username){
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Logged user not found"));

        String info = user.getMyInfo();
        if(info == null){
            throw new IllegalArgumentException("No personal information found");
        }

        return info;
    }

    @Transactional
    public UpdateMyInfoResponse updateMyInfo(String info, String username){
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Logged user not found"));

        String currentInfo = user.getMyInfo();
        if(currentInfo == null){
            throw new IllegalArgumentException("You must add personal information before you update it");
        }

        user.setMyInfo(info);

        return new UpdateMyInfoResponse(
                user.getId(),
                user.getUsername(),
                "Personal information updated successfully"
        );
    }
}