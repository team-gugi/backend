package com.boot.gugi.controller;

import com.boot.gugi.base.ApiResponse;
import com.boot.gugi.base.dto.DiaryDTO;
import com.boot.gugi.base.status.SuccessStatus;
import com.boot.gugi.model.Diary;
import com.boot.gugi.service.DiaryService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/diary")
@RequiredArgsConstructor
public class DiaryController {

    private final DiaryService diaryService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Diary>> createDiary(HttpServletRequest request, HttpServletResponse response,
                                                          @RequestPart @Valid DiaryDTO.DiaryRequest diaryInfo,
                                                          @RequestPart(value = "gameImg", required = false) MultipartFile gameImg) {

        diaryService.createDiaryPost(request, response, diaryInfo, gameImg);

        return ApiResponse.onSuccess(SuccessStatus._CREATED);
    }

    @PutMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Diary>> updateDiary(HttpServletRequest request, HttpServletResponse response,
                                                          @RequestPart @Valid UUID diaryId,
                                                          @RequestPart @Valid DiaryDTO.DiaryRequest diaryInfo,
                                                          @RequestPart(value = "gameImg", required = false) MultipartFile gameImg) {

        diaryService.updateDiaryPost(request, response, diaryId, diaryInfo, gameImg);

        return ApiResponse.onSuccess(SuccessStatus._UPDATED);
    }

    @GetMapping(value = "/details")
    public ResponseEntity<ApiResponse<DiaryDTO.DiaryDetailDto>> getDiaryDetails(HttpServletRequest request, HttpServletResponse response,
                                                                                @RequestParam @Valid UUID diaryId) {

        DiaryDTO.DiaryDetailDto diaryInfo = diaryService.getDiaryDetails(request, response, diaryId);

        return ApiResponse.onSuccess(SuccessStatus._GET, diaryInfo);
    }

    @GetMapping(value = "/all")
    public ResponseEntity<ApiResponse<List<DiaryDTO.DiarySingleDto>>> getAllDiaries(HttpServletRequest request, HttpServletResponse response) {

        List<DiaryDTO.DiarySingleDto> diaryList = diaryService.getAllDiary(request, response);

        return ApiResponse.onSuccess(SuccessStatus._GET, diaryList);
    }

    @GetMapping(value = "/win-rate")
    public ResponseEntity<ApiResponse<DiaryDTO.WinRateResponse>> getMyWinRate(HttpServletRequest request, HttpServletResponse response) {

        DiaryDTO.WinRateResponse myWinRate = diaryService.getMyWinRate(request, response);

        return ApiResponse.onSuccess(SuccessStatus._GET, myWinRate);
    }
}