package com.boot.gugi.controller;

import com.boot.gugi.base.ApiResponse;
import com.boot.gugi.base.dto.MateDTO;
import com.boot.gugi.base.status.SuccessStatus;
import com.boot.gugi.model.MateRequest;
import com.boot.gugi.model.MatePost;
import com.boot.gugi.service.MateService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
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
    public ResponseEntity<ApiResponse<List<MateDTO.ResponseByDate>>> getMatePostsSortedByDate(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime  cursor) {

        List<MateDTO.ResponseByDate> matePostList = mateService.getAllPostsSortedByDate(cursor);

        return ApiResponse.onSuccess(SuccessStatus._GET, matePostList);
    }

    @GetMapping(value = "/relevant")
    public ResponseEntity<ApiResponse<List<MateDTO.ResponseByRelevance>>> getMatePostsSortedByRelevance(
            @RequestParam(required = false) String cursor,
            @RequestParam(required = false) LocalDate date,
            @RequestParam(required = false) String gender,
            @RequestParam(required = false) String age,
            @RequestParam(required = false) String team,
            @RequestParam(required = false) String stadium,
            @RequestParam(required = false) Integer member) {


        MateDTO.RequestOption matePostOptions = new MateDTO.RequestOption(gender, age, date, team, member, stadium);
        List<MateDTO.ResponseByRelevance> matePostList = mateService.getAllPostsSortedByRelevance(cursor, matePostOptions);

        return ApiResponse.onSuccess(SuccessStatus._GET, matePostList);
    }

    @PostMapping(value = "/{mateId}/apply")
    public ResponseEntity<ApiResponse<MateRequest>> applyToMatePost(HttpServletRequest request, HttpServletResponse response,
                                                                    @Valid @PathVariable UUID mateId) {

        mateService.applyToMatePost(request, response, mateId);

        return ApiResponse.onSuccess(SuccessStatus._APPLY);
    }
}