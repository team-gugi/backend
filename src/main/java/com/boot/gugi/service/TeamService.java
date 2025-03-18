package com.boot.gugi.service;

import com.boot.gugi.base.Enum.TeamEnum;
import com.boot.gugi.base.dto.TeamDTO;
import com.boot.gugi.model.Team;
import com.boot.gugi.repository.RedisRepository;
import com.boot.gugi.repository.TeamRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TeamService {

    private final TeamRepository teamRepository;
    private final RedisRepository redisRepository;
    private final S3Service s3Service;

    public TeamDTO.teamResponse getTeamInfo(String teamCode) {

        TeamDTO.teamResponse teamInfo = redisRepository.findTeam(teamCode);
        if (teamInfo == null) {
            Team team = teamRepository.findByTeamCode(teamCode);

            if (team == null) {
                throw new EntityNotFoundException("Team not found for code: " + teamCode);
            }
            teamInfo = convertToTeamDetailsDTO(team);
        }
        return teamInfo;
    }

    private TeamDTO.teamResponse convertToTeamDetailsDTO(Team team) {
        TeamDTO.teamResponse teamDTO = new TeamDTO.teamResponse();
        teamDTO.setTeamCode(team.getTeamCode());
        teamDTO.setTeamLogo(team.getTeamLogo());
        teamDTO.setTeamName(team.getTeamName());
        teamDTO.setDescription(team.getDescription());
        teamDTO.setInstagram(team.getInstagram());
        teamDTO.setYoutube(team.getYoutube());
        teamDTO.setTicketShop(team.getTicketShop());
        teamDTO.setMdShop(team.getMdShop());
        return teamDTO;
    }

    public void saveTeamInfo(TeamDTO.teamRequest teamDetails, MultipartFile teamLogo) {

        String uploadedLogoUrl = s3Service.uploadImg(teamLogo, null);
        Team savedTeam = createTeamDetails(teamDetails, uploadedLogoUrl);
        redisRepository.saveTeam(savedTeam);
        teamRepository.save(savedTeam);
    }

    private Team createTeamDetails(TeamDTO.teamRequest teamDetails, String logoURL) {
        return Team.builder()
                .teamCode(teamDetails.getTeamCode())
                .teamLogo(logoURL)
                .teamName(teamDetails.getTeamName())
                .description(teamDetails.getDescription())
                .instagram(teamDetails.getInstagram())
                .youtube(teamDetails.getYoutube())
                .ticketShop(teamDetails.getTicketShop())
                .mdShop(teamDetails.getMdShop())
                .build();
    }

    public List<TeamDTO.ScheduleResponse> getTeamSchedule(String teamCode) {

        String teamName = TeamEnum.getShortNameByLowerCase(teamCode);
        List<TeamDTO.ScheduleResponse> scheduleList = redisRepository.findTeamSchedule(teamName);

        return scheduleList;
    }
}
