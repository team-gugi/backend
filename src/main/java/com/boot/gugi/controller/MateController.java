package com.boot.gugi.controller;

import com.boot.gugi.base.ApiResponse;
import com.boot.gugi.base.dto.MateDTO;
import com.boot.gugi.base.status.SuccessStatus;
import com.boot.gugi.model.MatePost;
import com.boot.gugi.service.MateService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/mate")
@RequiredArgsConstructor
public class MateController {

    private final MateService mateService;

    @PostMapping
    public ResponseEntity<ApiResponse<MatePost>> createMatePost(HttpServletRequest request, HttpServletResponse response,
                                                                @Validated @RequestBody MateDTO.MateRequest matePostDetails) {
        mateService.createMatePost(request, response, matePostDetails);

        return ApiResponse.onSuccess(SuccessStatus._CREATED);
    }

    @PutMapping
    public ResponseEntity<ApiResponse<MatePost>> updateMatePost(HttpServletRequest request, HttpServletResponse response,
                                                                @RequestParam UUID mateId,
                                                                @Validated @RequestBody MateDTO.MateRequest matePostDetails) {
        mateService.updateMatePost(request, response, mateId, matePostDetails);

        return ApiResponse.onSuccess(SuccessStatus._UPDATED);
    }
}
