package com.example.project.backend.service;

import com.example.project.backend.dto.request.user.ForgotPasswordRequest;
import com.example.project.backend.dto.request.user.UserRegisterRequest;
import com.example.project.backend.dto.response.admin.CreateAdminProfileResponse;
import com.example.project.backend.dto.response.user.AddMyInfoResponse;
import com.example.project.backend.dto.response.user.UpdateMyInfoResponse;
import com.example.project.backend.dto.response.user.UserActivationResponse;
import com.example.project.backend.dto.response.user.UserDeactivationResponse;
import com.example.project.backend.dto.response.user.UserProfileResponse;
import com.example.project.backend.dto.response.user.UserRegisterResponse;
import com.example.project.backend.dto.response.user.UserSearchResponse;
import com.example.project.backend.model.entity.User;
import com.example.project.backend.model.enums.SystemRole;
import com.example.project.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    public UserRegisterResponse register(UserRegisterRequest request) {
        validateRegistration(request);

        User user = buildUser(request);
        user.setEnabled(true);

        User savedUser = userRepository.save(user);

        logger.info("Registered user with id {} and username {}", savedUser.getId(), savedUser.getUsername());

        return new UserRegisterResponse(
                savedUser.getId(),
                savedUser.getUsername(),
                savedUser.getEmail(),
                "User registered successfully."
        );
    }

    public String forgotPassword(ForgotPasswordRequest request) {
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            logger.error("Passwords do not match");
            throw new IllegalArgumentException("Passwords do not match");
        }

        User user = userRepository.findByUsername(request.getUsernameOrEmail())
                .or(() -> userRepository.findByEmail(request.getUsernameOrEmail()))
                .orElseThrow(() -> {
                    logger.error("User not found");
                    return new IllegalArgumentException("User not found");
                });

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        logger.info("Successful change of password of user with id {} and username {}", user.getId(), user.getUsername());

        return "Password changed successfully";
    }

    private void validateRegistration(UserRegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            logger.error("Unsuccessful registration - username {} already exists", request.getUsername());
            throw new IllegalArgumentException("Username already exists");
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            logger.error("Unsuccessful registration - email {} already exists", request.getEmail());
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
                        user.getEmail(),
                        user.getSystemRole(),
                        user.getMyInfo(),
                        user.isActive()
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public UserProfileResponse getUserProfile(Long userId, String loggedUsername) {
        userRepository.findByUsername(loggedUsername)
                .orElseThrow(() -> {
                    logger.error("Cannot get user profile - logged user with username {} not found", loggedUsername);
                    return new IllegalArgumentException("Logged user not found");
                });

        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    logger.error("Cannot get user profile - user with id {} not found", userId);
                    return new IllegalArgumentException("User not found");
                });

        logger.info("User with id {} found", userId);

        return new UserProfileResponse(
                user.getId(),
                user.getUsername(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getSystemRole(),
                user.getMyInfo(),
                user.isActive()
        );
    }

    @Transactional
    public AddMyInfoResponse addMyInfo(String info, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    logger.error("User with username {} not found", username);
                    return new IllegalArgumentException("Logged user not found");
                });

        user.setMyInfo(info);

        logger.info("User with id {} and username {} successfully updated personal information", user.getId(), user.getUsername());

        return new AddMyInfoResponse(
                user.getId(),
                user.getUsername(),
                "Personal information added successfully"
        );
    }

    @Transactional(readOnly = true)
    public String getMyInfo(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    logger.error("Cannot obtain personal information - user with username {} not found", username);
                    return new IllegalArgumentException("Logged user not found");
                });

        String info = user.getMyInfo();
        if (info == null) {
            logger.error("No personal information found for user with id {}", user.getId());
            throw new IllegalArgumentException("No personal information found");
        }

        logger.info("Successfully retrieved personal information for user with id {}", user.getId());

        return info;
    }

    @Transactional
    public UpdateMyInfoResponse updateMyInfo(String info, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    logger.error("Cannot update personal information - user with username {} not found", username);
                    return new IllegalArgumentException("Logged user not found");
                });

        String currentInfo = user.getMyInfo();
        if (currentInfo == null) {
            logger.error("Unsuccessful updating of personal information of user with id {} - current personal information not found", user.getId());
            throw new IllegalArgumentException("You must add personal information before you update it");
        }

        user.setMyInfo(info);

        logger.info("Successful updating of personal information of user with id {}", user.getId());

        return new UpdateMyInfoResponse(
                user.getId(),
                user.getUsername(),
                "Personal information updated successfully"
        );
    }

    @Transactional(readOnly = true)
    public List<UserSearchResponse> getAllUsers(String username) {
        User loggedUser = userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    logger.error("Unsuccessful retrieval of all users - user with username {} not found", username);
                    return new IllegalArgumentException("Logged user not found");
                });

        if (loggedUser.getSystemRole() != SystemRole.ADMIN) {
            logger.error("Unsuccessful retrieval of all users - user with id {} is not an admin", loggedUser.getId());
            throw new IllegalArgumentException("You don't have access to this information");
        }

        logger.info("Successful retrieval of all users by user with id {}", loggedUser.getId());

        return userRepository.findAll()
                .stream()
                .map(user -> new UserSearchResponse(
                        user.getId(),
                        user.getUsername(),
                        user.getFirstName(),
                        user.getLastName(),
                        user.getEmail(),
                        user.getSystemRole(),
                        user.getMyInfo(),
                        user.isActive()
                ))
                .toList();
    }

    public User getValidatedAdmin(String adminUsername, String errorMsgPrefix) {
        User admin = userRepository.findByUsername(adminUsername)
                .orElseThrow(() -> {
                    logger.error("{} admin with username {} not found", errorMsgPrefix, adminUsername);
                    return new IllegalArgumentException("Admin not found.");
                });

        if (admin.getSystemRole() != SystemRole.ADMIN) {
            logger.error("{} user with id {} is not an admin", errorMsgPrefix, admin.getId());
            throw new IllegalArgumentException("Only admins can access this resource.");
        }

        return admin;
    }

    @Transactional(readOnly = true)
    public List<UserSearchResponse> searchUsersForAdmin(String adminUsername, String search) {
        User admin = getValidatedAdmin(adminUsername, "Cannot search users for admin -");

        logger.info("Admin with id {} searched users by '{}'", admin.getId(), search);

        return userRepository.searchUsers(search)
                .stream()
                .map(user -> new UserSearchResponse(
                        user.getId(),
                        user.getUsername(),
                        user.getFirstName(),
                        user.getLastName(),
                        user.getEmail(),
                        user.getSystemRole(),
                        user.getMyInfo(),
                        user.isActive()
                ))
                .toList();
    }

    @Transactional
    public UserDeactivationResponse deactivateUser(Long userId, String adminUsername) {
        User admin = getValidatedAdmin(adminUsername, "Cannot deactivate user -");

        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    logger.error("Cannot deactivate user - user with id {} not found", userId);
                    return new IllegalArgumentException("User not found.");
                });

        user.setActive(false);

        logger.info("Admin with id {} successfully deactivated the account of user with id {}", admin.getId(), userId);

        return new UserDeactivationResponse(
                user.getId(),
                user.getUsername(),
                user.isActive(),
                "User account is deactivated successfully."
        );
    }

    @Transactional
    public UserActivationResponse activateUser(Long userId, String adminUsername) {
        User admin = getValidatedAdmin(adminUsername, "Cannot activate user -");

        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    logger.error("Cannot activate user - user with id {} not found", userId);
                    return new IllegalArgumentException("User not found.");
                });

        user.setActive(true);

        logger.info("Admin with id {} successfully activated the account of user with id {}", admin.getId(), userId);

        return new UserActivationResponse(
                user.getId(),
                user.getUsername(),
                user.isActive(),
                "User account is activated successfully."
        );
    }
}