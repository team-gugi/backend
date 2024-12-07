package com.boot.gugi.controller;

import com.boot.gugi.base.ApiResponse;
import com.boot.gugi.base.dto.TeamDTO;
import com.boot.gugi.base.status.SuccessStatus;
import com.boot.gugi.model.Team;
import com.boot.gugi.service.TeamService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/team")
@RequiredArgsConstructor
public class TeamController {

    private final TeamService teamService;

    @GetMapping(value = "/details")
    public ResponseEntity<ApiResponse<TeamDTO.teamDetailsDTO>> getTeam(@RequestParam @Valid String teamCode) {

        TeamDTO.teamDetailsDTO teamInfo = teamService.getTeamInfo(teamCode);

        return ApiResponse.onSuccess(SuccessStatus._GET, teamInfo);
    }

    @PostMapping(value = "/details")
    public ResponseEntity<ApiResponse<Team>> saveTeam(@RequestBody @Valid TeamDTO.teamDetailsDTO teamDetails) {

        teamService.saveTeamInfo(teamDetails);

        return ApiResponse.onSuccess(SuccessStatus._OK);
    }
}
