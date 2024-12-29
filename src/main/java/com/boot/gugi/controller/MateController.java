package com.boot.gugi.controller;

import com.boot.gugi.base.ApiResponse;
import com.boot.gugi.base.dto.MateDTO;
import com.boot.gugi.base.status.SuccessStatus;
import com.boot.gugi.model.MatePost;
import com.boot.gugi.service.MateService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
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

    @GetMapping(value = "/latest")
    public ResponseEntity<ApiResponse<List<MateDTO.MateResponse>>> getMatePostsSortedByDate(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime  cursor) {
        List<MateDTO.MateResponse> matePostList = mateService.getAllPostsSortedByDate(cursor);

        return ApiResponse.onSuccess(SuccessStatus._GET, matePostList);
    }
}