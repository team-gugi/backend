package com.boot.gugi.controller;

import com.boot.gugi.base.ApiResponse;
import com.boot.gugi.base.dto.TeamDTO;
import com.boot.gugi.base.status.SuccessStatus;
import com.boot.gugi.model.Team;
import com.boot.gugi.service.TeamService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/team")
@RequiredArgsConstructor
public class TeamController {

    private final TeamService teamService;

    @GetMapping(value = "/details")
    public ResponseEntity<ApiResponse<TeamDTO.teamResponse>> getTeam(@RequestParam @Valid String teamCode) {

        TeamDTO.teamResponse teamInfo = teamService.getTeamInfo(teamCode);

        return ApiResponse.onSuccess(SuccessStatus._GET, teamInfo);
    }

    @PostMapping(value = "/details", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Team>> saveTeam(@RequestPart @Valid TeamDTO.teamRequest teamDetails,
                                                      @RequestPart(value = "teamLogo", required = false) MultipartFile teamLogo) {

        teamService.saveTeamInfo(teamDetails, teamLogo);

        return ApiResponse.onSuccess(SuccessStatus._OK);
    }

    @GetMapping(value = "/schedule")
    public ResponseEntity<ApiResponse<List<TeamDTO.ScheduleResponse>>> scrapeSchedule(@RequestParam @Valid String teamCode) {

        List<TeamDTO.ScheduleResponse> teamSchedule = teamService.getTeamSchedule(teamCode);

        return ApiResponse.onSuccess(SuccessStatus._GET, teamSchedule);
    }
}
