package com.boot.gugi.service;

import com.boot.gugi.base.Enum.TeamEnum;
import com.boot.gugi.base.dto.TeamDTO;
import com.boot.gugi.model.TeamRank;
import com.boot.gugi.repository.RedisRepository;
import com.boot.gugi.repository.TeamRankRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MainService {

    private final RedisRepository redisRepository;
    private final TeamRankRepository teamRankRepository;

    public List<TeamDTO.RankResponse> getRanks() {

        List<TeamDTO.RankResponse> ranks = redisRepository.findRanks();
        if (!ranks.isEmpty()) {
            return ranks;
        }

        return teamRankRepository.findAll().stream()
                .filter(team -> isValidRank(team.getTeamRank()))
                .sorted(Comparator.comparingInt(TeamRank::getTeamRank))
                .map(this::toRankResponse)
                .collect(Collectors.toList());
    }

    private boolean isValidRank(int rank) {
        return rank >= 1 && rank <= 10;
    }

    private TeamDTO.RankResponse toRankResponse(TeamRank team) {
        return new TeamDTO.RankResponse(
                team.getTeamRank(),
                team.getTeam(),
                team.getGame(),
                team.getWin(),
                team.getLose(),
                team.getDraw(),
                team.getWinningRate(),
                team.getDifference()
        );
    }
}