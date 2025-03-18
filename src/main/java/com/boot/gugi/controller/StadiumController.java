package com.boot.gugi.controller;

import com.boot.gugi.base.ApiResponse;
import com.boot.gugi.base.dto.StadiumDTO;
import com.boot.gugi.base.status.SuccessStatus;
import com.boot.gugi.model.Food;
import com.boot.gugi.model.Stadium;
import com.boot.gugi.service.StadiumService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/stadium")
@RequiredArgsConstructor
public class StadiumController {

    private final StadiumService stadiumService;

    @GetMapping
    public ResponseEntity<ApiResponse<StadiumDTO.StadiumResponse>> getStadium(@RequestParam @Valid Integer stadiumCode) {

        StadiumDTO.StadiumResponse stadiumInfo = stadiumService.getStadiumInfo(stadiumCode);

        return ApiResponse.onSuccess(SuccessStatus._GET, stadiumInfo);
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Stadium>> saveStadium(@RequestBody @Valid StadiumDTO.StadiumRequest stadiumDetails) {

        stadiumService.saveStadiumInfo(stadiumDetails);

        return ApiResponse.onSuccess(SuccessStatus._OK);
    }

    @PostMapping(value = "/foods", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Food>> saveFood(@RequestPart @Valid StadiumDTO.FoodRequest foodDetails,
                                                      @RequestPart(value = "foodImg", required = false) MultipartFile foodImg) {

        stadiumService.saveFoodInfo(foodDetails,foodImg);

        return ApiResponse.onSuccess(SuccessStatus._OK);
    }
}
