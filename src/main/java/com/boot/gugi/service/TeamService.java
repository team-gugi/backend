package com.boot.gugi.service;

import com.boot.gugi.base.dto.TeamDTO;
import com.boot.gugi.model.Team;
import com.boot.gugi.repository.RedisRepository;
import com.boot.gugi.repository.TeamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TeamService {

    private final TeamRepository teamRepository;
    private final RedisRepository redisRepository;

    public TeamDTO.teamDetailsDTO getTeamInfo(String teamCode) {

        TeamDTO.teamDetailsDTO teamInfo = redisRepository.findTeam(teamCode);
        if (teamInfo == null) {
            TeamDTO.teamDetailsDTO team = teamRepository.findByTeamCode(teamCode);
            return team;
        }
        return teamInfo;
    }

    public void saveTeamInfo(TeamDTO.teamDetailsDTO teamDetails) {

        Team savedTeam = createTeamDetails(teamDetails);
        redisRepository.saveTeam(savedTeam);
        teamRepository.save(savedTeam);
    }

    private Team createTeamDetails(TeamDTO.teamDetailsDTO teamDetails) {
        return Team.builder()
                .teamCode(teamDetails.getTeamCode())
                .teamName(teamDetails.getTeamName())
                .description(teamDetails.getDescription())
                .instagram(teamDetails.getInstagram())
                .youtube(teamDetails.getYoutube())
                .ticketShop(teamDetails.getTicketShop())
                .mdShop(teamDetails.getMdShop())
                .build();
    }
}
