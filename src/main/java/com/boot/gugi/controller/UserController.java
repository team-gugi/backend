package com.boot.gugi.controller;

import com.boot.gugi.base.dto.OnboardingInfoDTO;
import com.boot.gugi.exception.UserSuccessResult;
import com.boot.gugi.service.UserService;
import com.boot.gugi.base.ApiResponse;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping(value = "/onboarding", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<OnboardingInfoDTO.DefineUserResponse>> createUser(
            HttpServletResponse response,
            @RequestHeader(value = "Authorization") String authorizationHeader,
            @RequestPart("OnboardingInfoDTO") @Valid OnboardingInfoDTO.DefineUserRequest defineUserRequest,
            @RequestPart(value = "profileImg", required = false) MultipartFile profileImg) {

        OnboardingInfoDTO.DefineUserResponse defineUserResponse = userService.createUser(response, authorizationHeader,defineUserRequest,profileImg);

        return ApiResponse.onSuccess(UserSuccessResult.CREATED_DEFINE_USER, defineUserResponse);
    }
}