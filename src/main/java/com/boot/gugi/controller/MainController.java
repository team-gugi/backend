package com.boot.gugi.controller;

import com.boot.gugi.base.ApiResponse;
import com.boot.gugi.base.dto.TeamDTO;
import com.boot.gugi.base.status.SuccessStatus;
import com.boot.gugi.model.TeamRank;
import com.boot.gugi.service.MainService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/main")
@RequiredArgsConstructor
public class MainController {

    private final MainService mainService;

    @GetMapping(value = "/kbo-ranking")
    public ResponseEntity<ApiResponse<List<TeamDTO.RankResponse>>> scrapeRank(
            HttpServletResponse response) {

        List<TeamDTO.RankResponse> teamRank = mainService.getRanks();

        return ApiResponse.onSuccess(SuccessStatus._GET, teamRank);
    }

    /*@PostMapping(value = "/kbo-ranking", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<TeamRank>> saveRank(
            HttpServletResponse response, @RequestPart @Valid TeamDTO.RankRequest rankRequest,
            @RequestPart(value = "teamLogo", required = false) MultipartFile teamLogo) {

        TeamRank teamRank = mainService.saveRank(rankRequest, teamLogo);

        return ApiResponse.onSuccess(SuccessStatus._OK, teamRank);
    }*/
}
