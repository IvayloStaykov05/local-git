package com.example.project.backend.controller;

import com.example.project.backend.dto.request.user.AddMyInfoRequest;
import com.example.project.backend.dto.request.user.UpdateMyInfoRequest;
import com.example.project.backend.dto.response.user.AddMyInfoResponse;
import com.example.project.backend.dto.response.user.UpdateMyInfoResponse;
import com.example.project.backend.dto.response.user.UserProfileResponse;
import com.example.project.backend.dto.response.user.UserSearchResponse;
import com.example.project.backend.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/search")
    public ResponseEntity<List<UserSearchResponse>> searchUsers(
            @RequestParam(required = false) String search
    ) {
        return ResponseEntity.ok(userService.searchUsers(search));
    }

    @GetMapping("/{userId}")
    public ResponseEntity<UserProfileResponse> getUserProfile(
            @PathVariable Long userId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(userService.getUserProfile(userId, authentication.getName()));
    }

    @GetMapping("/getMyInfo")
    public ResponseEntity<String> getMyInfo(Authentication authentication){
        String info = userService.getMyInfo(authentication.getName());
        return ResponseEntity.ok(info);
    }

    @PostMapping("/addMyInfo")
    public ResponseEntity<AddMyInfoResponse> addMyInfo(
            @RequestBody @Valid AddMyInfoRequest request,
            Authentication authentication
    ){
        AddMyInfoResponse response = userService.addMyInfo(request.getInfo(), authentication.getName());
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/updateMyInfo")
    public ResponseEntity<UpdateMyInfoResponse> updateMyInfo(
            @RequestBody @Valid UpdateMyInfoRequest request,
            Authentication authentication
    ){
        UpdateMyInfoResponse response = userService.updateMyInfo(request.getInfo(), authentication.getName());
        return ResponseEntity.ok(response);
    }
}