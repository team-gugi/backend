package com.boot.gugi.repository;

import com.boot.gugi.base.dto.StadiumDTO;
import com.boot.gugi.base.dto.TeamDTO;
import com.boot.gugi.model.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.util.*;
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
    private static final String TEAM_CODE_PREFIX = "team-code:";
    private static final String STADIUM_CODE_PREFIX = "stadium-code:";
    private static final String FOOD_CODE_PREFIX = "food-code:";
    private static final String SCHEDULE_PREFIX = "schedule:";

    private final TeamRankRepository teamRankRepository;
    private final TeamScheduleRepository teamScheduleRepository;
    private final RedisTemplate<String, String> redisTemplate;
    private final RedissonClient redissonClient;
    private final ObjectMapper objectMapper;

    public void saveRank(TeamRank rankInfo) {
        saveToRedis(TEAM_RANK_PREFIX + rankInfo.getTeamRank() , rankInfo, TEAM_RANK_EXPIRATION_TIME);
    }

    public void saveTeam(Team teamInfo) {
        saveToRedis(TEAM_CODE_PREFIX + teamInfo.getTeamCode(), teamInfo, null);
    }

    public void saveStadium(Stadium stadiumInfo) {
        saveToRedis(STADIUM_CODE_PREFIX + stadiumInfo.getStadiumCode(), stadiumInfo, null);
    }

    private String getRedisKeySchedule(String date, String teamName) { return SCHEDULE_PREFIX + date + ":" + teamName;}

    public void saveSchedule(TeamSchedule teamSchedule) {

        String homeKey = getRedisKeySchedule(teamSchedule.getDate(), teamSchedule.getHomeTeam());
        saveForSet(homeKey, teamSchedule);

        String awayKey = getRedisKeySchedule(teamSchedule.getDate(), teamSchedule.getAwayTeam());
        saveForSet(awayKey, teamSchedule);
    }

    public void saveFood(Food foodDetails, Integer stadiumCode) {
        String foodInfoKey = FOOD_CODE_PREFIX + stadiumCode;
        try {
            String foodInfoJson = objectMapper.writeValueAsString(foodDetails);
            SetOperations<String, String> setOperations = redisTemplate.opsForSet();
            setOperations.add(foodInfoKey, foodInfoJson);
        } catch (JsonProcessingException e) {
            logError("FoodInfo", stadiumCode.toString(), e);
        }
    }

    private <T> void saveToRedis(String key, T object, Long expirationTime) {
        try {
            String json = objectMapper.writeValueAsString(object);
            ValueOperations<String, String> valueOperations = redisTemplate.opsForValue();
            valueOperations.set(key, json);
            if (expirationTime != null) {
                redisTemplate.expire(key, expirationTime, TimeUnit.MILLISECONDS);
            }
        } catch (JsonProcessingException e) {
            logError(object.getClass().getSimpleName(), key, e);
        }
    }

    private void saveForSet(String key, TeamSchedule teamSchedule) {
        try {
            String jsonValue = objectMapper.writeValueAsString(teamSchedule);
            redisTemplate.opsForSet().add(key, jsonValue);
        } catch (Exception e) {
            logError(teamSchedule.getClass().getSimpleName(), key, e);
        }
    }

    public void deleteSchedule(TeamSchedule teamSchedule) {
        String homeKey = getRedisKeySchedule(teamSchedule.getDate(), teamSchedule.getHomeTeam());
        deleteFromSet(homeKey, teamSchedule);

        String awayKey = getRedisKeySchedule(teamSchedule.getDate(), teamSchedule.getAwayTeam());
        deleteFromSet(awayKey, teamSchedule);
    }

    private void deleteFromSet(String key, TeamSchedule teamSchedule) {
        try {
            String jsonValue = objectMapper.writeValueAsString(teamSchedule);
            redisTemplate.opsForSet().remove(key, jsonValue);
        } catch (Exception e) {
            logger.error("Failed to process {} object in Redis. Key: {}. Error: {}", teamSchedule.getClass().getSimpleName(), key, e.getMessage());
        }
    }

    private void logError(String objectType, String key, Exception e) {
        logger.error("Failed to convert {} object to JSON. Key: {}, Error: {}", objectType, key, e.getMessage(), e);
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

    public TeamDTO.teamResponse findTeam(String teamCode) {
        ValueOperations<String, String> valueOperations = redisTemplate.opsForValue();
        String teamInfoKey = TEAM_CODE_PREFIX + teamCode;
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

    public StadiumDTO.StadiumResponse findStadium(Integer stadiumCode) {
        ValueOperations<String, String> valueOperations = redisTemplate.opsForValue();
        String stadiumInfoKey = STADIUM_CODE_PREFIX + stadiumCode;
        String stadiumInfoJson = valueOperations.get(stadiumInfoKey);

        SetOperations<String, String> setOperations = redisTemplate.opsForSet();
        String foodInfoKey = FOOD_CODE_PREFIX + stadiumCode;
        Set<String> foodInfoJsonSet = setOperations.members(foodInfoKey);

        if (stadiumInfoJson != null) {
            try {
                StadiumDTO.StadiumResponse stadiumResponse = objectMapper.readValue(stadiumInfoJson, StadiumDTO.StadiumResponse.class);

                if (foodInfoJsonSet != null && !foodInfoJsonSet.isEmpty()) {
                    Set<StadiumDTO.FoodResponse> foodList = new HashSet<>();
                    for (String foodInfoJson : foodInfoJsonSet) {
                        StadiumDTO.FoodResponse foodResponse = objectMapper.readValue(foodInfoJson, StadiumDTO.FoodResponse.class);
                        foodList.add(foodResponse);
                    }
                    stadiumResponse.setFoodList(foodList);
                } else {
                    return null;
                }
                return stadiumResponse;
            } catch (JsonProcessingException e) {
                logger.error("Failed to convert stadium JSON to StadiumResponse. Key: {}, Error: {}", stadiumInfoKey, e.getMessage(), e);
            }
        }
        return null;
    }

    public List<TeamDTO.ScheduleResponse> findTeamSchedule(String teamCode) {
        List<TeamDTO.ScheduleResponse> scheduleList = new ArrayList<>();

        for (int year = 2024; year <= 2025; year++) {
            for (int month = 1; month <= 12; month++) {
                String date = String.format("%04d.%02d", year, month);
                String key = getRedisKeySchedule(date, teamCode);

                TeamDTO.ScheduleResponse scheduleResponse = new TeamDTO.ScheduleResponse();
                scheduleResponse.setDate(date);
                Set<TeamDTO.SpecificSchedule> specificSchedules = new HashSet<>();

                Set<String> jsonSchedules = redisTemplate.opsForSet().members(key);
                if (jsonSchedules != null && !jsonSchedules.isEmpty()) {

                    for (String jsonSchedule : jsonSchedules) {
                        try {
                            TeamSchedule teamSchedule = objectMapper.readValue(jsonSchedule, TeamSchedule.class);
                            TeamDTO.SpecificSchedule specificSchedule = convertToSpecificSchedule(teamSchedule, teamCode);
                            specificSchedules.add(specificSchedule);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                } else {
                    List<TeamSchedule> teamSchedules = teamScheduleRepository.findByDateAndTeam(date, teamCode);
                    for (TeamSchedule teamSchedule : teamSchedules) {
                        TeamDTO.SpecificSchedule specificSchedule = convertToSpecificSchedule(teamSchedule, teamCode);
                        specificSchedules.add(specificSchedule);
                    }

                }
                scheduleResponse.setSpecificSchedule(specificSchedules);
                scheduleList.add(scheduleResponse);
            }
        }

        return scheduleList;
    }

    private TeamDTO.SpecificSchedule convertToSpecificSchedule(TeamSchedule teamSchedule, String teamCode) {
        TeamDTO.SpecificSchedule specificSchedule = new TeamDTO.SpecificSchedule();

        specificSchedule.setSpecificDate(teamSchedule.getSpecificDate());
        specificSchedule.setHomeTeam(teamSchedule.getHomeTeam());
        specificSchedule.setAwayTeam(teamSchedule.getAwayTeam());
        specificSchedule.setHomeScore(teamSchedule.getHomeScore());
        specificSchedule.setAwayScore(teamSchedule.getAwayScore());
        specificSchedule.setTime(teamSchedule.getGameTime());
        specificSchedule.setStadium(teamSchedule.getStadium());
        specificSchedule.setCancellationReason(teamSchedule.getCancellationReason());

        if (teamCode.equals(teamSchedule.getHomeTeam())) {
            specificSchedule.setLogoUrl(teamSchedule.getAwayImg());
        } else {
            specificSchedule.setLogoUrl(teamSchedule.getHomeImg());
        }

        return specificSchedule;
    }
}