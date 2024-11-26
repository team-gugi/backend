package com.boot.gugi.token.controller;

import com.boot.gugi.base.ApiResponse;
import com.boot.gugi.base.status.TokenSuccessStatus;
import com.boot.gugi.token.service.TokenService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class TokenController {
    private final TokenService authService;

    @GetMapping("/reissue/access-token")
    public ResponseEntity<ApiResponse<Object>> reissueAccessToken(
            HttpServletRequest request,
            HttpServletResponse response) {

        authService.reissueAccessToken(request, response);
        return ApiResponse.onSuccess(TokenSuccessStatus.CREATED_ACCESS_TOKEN);
    }
}