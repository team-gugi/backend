package com.boot.gugi.service;

import com.boot.gugi.base.dto.TeamDTO;
import com.boot.gugi.model.TeamRank;
import com.boot.gugi.repository.RedisRepository;
import com.boot.gugi.repository.TeamRankRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MainService {

    private final RedisRepository redisRepository;
    private final TeamRankRepository teamRankRepository;
    private final RedisTemplate<String, String> redisTemplate;

    private static final Logger logger = LoggerFactory.getLogger(MainService.class);

    public List<TeamDTO.RankResponse> getRanks() {
        long mongoCount = teamRankRepository.count();
        Set<String> redisKeys = redisTemplate.keys("team-rank:*");
        int redisKeyCount = (redisKeys != null) ? redisKeys.size() : 0;

        if (redisKeyCount == 0 || isSyncRequired(mongoCount, redisKeyCount)) {
            logger.info("Rank Redis 동기화 필요. MongoDB 데이터: {}개, Redis 키: {}개", mongoCount, redisKeyCount);
            List<TeamRank> dbRanks = teamRankRepository.findAll();
            redisRepository.syncRankToRedis(dbRanks, redisKeys);

            return dbRanks.stream()
                    .filter(team -> isValidRank(team.getTeamRank()))
                    .sorted(Comparator.comparingInt(TeamRank::getTeamRank))
                    .map(this::toRankResponse)
                    .collect(Collectors.toList());
        } else {
            List<TeamDTO.RankResponse> ranks = redisRepository.findRanks();
            logger.info("* Redis * 랭킹 정보 조회 성공.");

            return ranks.stream()
                    .filter(rank -> isValidRank(rank.getTeamRank()))
                    .sorted(Comparator.comparingInt(TeamDTO.RankResponse::getTeamRank))
                    .collect(Collectors.toList());
        }
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

    private boolean isSyncRequired(long mongoCount, int redisKeyCount) {
        return redisKeyCount != mongoCount;
    }
}