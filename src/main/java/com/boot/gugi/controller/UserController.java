package com.boot.gugi.controller;

import com.boot.gugi.base.dto.MatePostStatusDTO;
import com.boot.gugi.base.dto.OnboardingInfoDTO;
import com.boot.gugi.base.dto.UserDTO;
import com.boot.gugi.base.status.SuccessStatus;
import com.boot.gugi.base.status.UserSuccessStatus;
import com.boot.gugi.model.MateRequest;
import com.boot.gugi.service.MyPageService;
import com.boot.gugi.base.ApiResponse;
import com.boot.gugi.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final MyPageService myPageService;

    @PostMapping(value = "/onboarding", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<OnboardingInfoDTO.DefineUserResponse>> createUser(
            HttpServletResponse response,
            @RequestHeader(value = "Authorization") String authorizationHeader,
            @RequestPart("OnboardingInfoDTO") @Valid OnboardingInfoDTO.DefineUserRequest defineUserRequest,
            @RequestPart(value = "profileImg", required = false) MultipartFile profileImg) {

        OnboardingInfoDTO.DefineUserResponse defineUserResponse = userService.createUser(response, authorizationHeader,defineUserRequest,profileImg);

        return ApiResponse.onSuccess(UserSuccessStatus.CREATED_DEFINE_USER, defineUserResponse);
    }

    @GetMapping(value = "/info")
    public ResponseEntity<ApiResponse<UserDTO.UserResponse>> getUserInfo(HttpServletRequest request, HttpServletResponse response) {

        UserDTO.UserResponse user = userService.getCurrentUser(request, response);
        return ApiResponse.onSuccess(UserSuccessStatus.GET_USER, user);
    }

    @PostMapping(value = "/mate-requests/{requestId}/status")
    public ResponseEntity<ApiResponse<MateRequest>> updateRequestStatus(HttpServletRequest request, HttpServletResponse response,
                                                                        @Valid @PathVariable UUID requestId,
                                                                        @RequestParam String status) {

        myPageService.respondToMateRequest(request, response, requestId, status);

        return ApiResponse.onSuccess(UserSuccessStatus.UPDATE_MATE_REQUEST);
    }

    @GetMapping(value = "/notifications/all")
    public ResponseEntity<ApiResponse<MatePostStatusDTO.MateRequestSummaryDTO>> getMatePostStatus(HttpServletRequest request, HttpServletResponse response) {

        MatePostStatusDTO.MateRequestSummaryDTO  mateRequestStatusList = myPageService.getMateRequestSummary(request, response);

        return ApiResponse.onSuccess(SuccessStatus._GET, mateRequestStatusList);
    }
}