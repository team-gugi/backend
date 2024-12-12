package com.boot.gugi.repository;

import com.boot.gugi.base.dto.TeamDTO;
import com.boot.gugi.model.Team;
import com.boot.gugi.model.TeamRank;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Repository
@RequiredArgsConstructor
public class RedisRepository {

    @Value("${team.rank.expiration-time}")
    private long TEAM_RANK_EXPIRATION_TIME;

    private static final Logger logger = LoggerFactory.getLogger(RedisRepository.class);
    private static final String TEAM_RANK_PREFIX = "team-rank:";
    private static final String TEAM_CODE = "team-code:";

    private final TeamRankRepository teamRankRepository;
    private final RedisTemplate<String, String> redisTemplate;
    private final RedissonClient redissonClient;
    private final ObjectMapper objectMapper;

    public void saveRank(TeamRank savedRank) {
        ValueOperations<String, String> valueOperations = redisTemplate.opsForValue();
        String teamRankKey = TEAM_RANK_PREFIX + savedRank.getTeamRank();

        try {
            String teamRankJson = objectMapper.writeValueAsString(savedRank);
            valueOperations.set(teamRankKey, teamRankJson);
            redisTemplate.expire(teamRankKey, TEAM_RANK_EXPIRATION_TIME, TimeUnit.MILLISECONDS);
        } catch (JsonProcessingException e) {
            logger.error("Failed to convert TeamRank object to JSON. TeamRank: {}, Error: {}", savedRank, e.getMessage(), e);
        }
    }

    public void updateRank(List<TeamRank> newScrapedData) {
        RLock lock = redissonClient.getLock("teamRankLock");
        try {
            if (lock.tryLock(5, TimeUnit.SECONDS)) {
                newScrapedData.forEach(data -> updateSingleRank(data));
            } else {
                logger.warn("Could not acquire lock for updating ranks.");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("Thread was interrupted while trying to acquire lock.", e);
        } finally {
            lock.unlock();
        }
    }

    private void updateSingleRank(TeamRank newData) {

        int rank = newData.getTeamRank();
        String teamRankKey = TEAM_RANK_PREFIX + rank;

        String newTeamRankJson = convertObjectToJson(newData);
        redisTemplate.opsForValue().set(teamRankKey, newTeamRankJson);
    }

    private String convertObjectToJson(TeamRank data) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            return objectMapper.writeValueAsString(data);
        } catch (JsonProcessingException e) {
            logger.error("Error converting object to JSON", e);
            return null;
        }
    }

    public List<TeamDTO.RankRequest> findRank() {
        List<TeamDTO.RankRequest> rankRequests = new ArrayList<>();
        ValueOperations<String, String> valueOperations = redisTemplate.opsForValue();

        for (int i = 1; i <= 10; i++) {
            String teamRankKey = TEAM_RANK_PREFIX + i;
            String teamRankJson = valueOperations.get(teamRankKey);
            if (teamRankJson != null) {
                try {
                    TeamDTO.RankRequest rankRequest = objectMapper.readValue(teamRankJson, TeamDTO.RankRequest.class);
                    rankRequests.add(rankRequest);
                } catch (JsonProcessingException e) {
                    logger.error("Failed to convert JSON to TeamRank object for rank {}. Error: {}", i, e.getMessage(), e);
                }
            }
        }
        return rankRequests;
    }

    public List<TeamDTO.RankResponse> findRanks() {
        List<TeamDTO.RankResponse> rankResponses = new ArrayList<>();
        ValueOperations<String, String> valueOperations = redisTemplate.opsForValue();

        for (int i = 1; i <= 10; i++) {
            String teamRankKey = TEAM_RANK_PREFIX + i;
            String teamRankJson = valueOperations.get(teamRankKey);
            if (teamRankJson != null) {
                try {
                    TeamDTO.RankResponse rankResponse = objectMapper.readValue(teamRankJson, TeamDTO.RankResponse.class);
                    rankResponses.add(rankResponse);
                } catch (JsonProcessingException e) {
                    logger.error("Failed to convert JSON to TeamRank object for rank {}. Error: {}", i, e.getMessage(), e);
                }
            }
        }
        return rankResponses;
    }

    public void saveTeam(Team teamDetails) {
        ValueOperations<String, String> valueOperations = redisTemplate.opsForValue();
        String teamInfoKey = TEAM_CODE + teamDetails.getTeamCode();

        try {
            String teamInfoJson = objectMapper.writeValueAsString(teamDetails);
            valueOperations.set(teamInfoKey, teamInfoJson);
        } catch (JsonProcessingException e) {
            logger.error("Failed to convert TeamInfo object to Json. TeamCode: {}, Error: {}", teamDetails.getTeamCode(), e.getMessage(), e);
        }
    }

    public TeamDTO.teamResponse findTeam(String teamCode) {
        ValueOperations<String, String> valueOperations = redisTemplate.opsForValue();
        String teamInfoKey = TEAM_CODE + teamCode;
        String teamInfoJson = valueOperations.get(teamInfoKey);

        if (teamInfoJson != null) {
            try {
                TeamDTO.teamResponse teamDetails = objectMapper.readValue(teamInfoJson, TeamDTO.teamResponse.class);
                logger.info("I found it. TeamCode: {}", teamCode);
                return teamDetails;
            } catch (JsonProcessingException e) {
                logger.error("Failed to convert JSON to TeamInfo object. TeamCode: {}, Error: {}", teamCode, e.getMessage(), e);
            }
        }
        return null;
    }
}