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
import java.util.stream.Collectors;

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

    /*** Team ***/
    public void saveTeam(Team teamInfo) {
        saveToRedis(TEAM_CODE_PREFIX + teamInfo.getTeamCode(), teamInfo, null);
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

    /*** Stadium ***/
    public void saveStadium(Stadium stadiumInfo) {
        saveToRedis(STADIUM_CODE_PREFIX + stadiumInfo.getStadiumCode(), stadiumInfo, null);
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

    /*** Food ***/
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

    /*** RANK ***/
    public void saveRank(TeamRank rankInfo) {
        saveToRedis(TEAM_RANK_PREFIX + rankInfo.getRankKey() , rankInfo, TEAM_RANK_EXPIRATION_TIME);
    }
    public void deleteRank(TeamRank rank) {
        redisTemplate.delete(TEAM_RANK_PREFIX + rank.getRankKey());
    }

    public List<TeamDTO.RankResponse> findRanks() {
        List<TeamDTO.RankResponse> rankResponses = new ArrayList<>();
        ValueOperations<String, String> valueOperations = redisTemplate.opsForValue();

        for (int i = 1; i <= 10; i++) {
            String key = TEAM_RANK_PREFIX + i;
            String json = valueOperations.get(key);
            if (json == null) {
                return Collections.emptyList();
            }

            try {
                TeamDTO.RankResponse rank = objectMapper.readValue(json, TeamDTO.RankResponse.class);
                rankResponses.add(rank);
            } catch (JsonProcessingException e) {
                logger.error("Redis JSON 파싱 실패: {}", e.getMessage());
                return Collections.emptyList();
            }
        }

        return rankResponses;
    }

    /*** SCHEDULE ***/
    private String getRedisKeySchedule(String date, String teamName) { return SCHEDULE_PREFIX + date + ":" + teamName;}

    public void saveSchedule(TeamSchedule schedule) {
        String homeKey = getRedisKeySchedule(schedule.getDate(), schedule.getHomeTeam());
        String awayKey = getRedisKeySchedule(schedule.getDate(), schedule.getAwayTeam());
        saveForSet(homeKey, schedule);
        saveForSet(awayKey, schedule);
    }

    public void deleteSchedule(TeamSchedule schedule) {
        String homeKey = getRedisKeySchedule(schedule.getDate(), schedule.getHomeTeam());
        String awayKey = getRedisKeySchedule(schedule.getDate(), schedule.getAwayTeam());
        deleteFromSet(homeKey, schedule);
        deleteFromSet(awayKey, schedule);
    }

    public List<TeamDTO.ScheduleResponse> findTeamSchedule(String teamCode) {
        List<TeamDTO.ScheduleResponse> scheduleList = new ArrayList<>();

        for (int year = 2024; year <= 2025; year++) {
            for (int month = 1; month <= 12; month++) {
                String date = String.format("%04d.%02d", year, month);
                Set<TeamDTO.SpecificSchedule> schedules = getSchedulesForMonth(date, teamCode);

                TeamDTO.ScheduleResponse scheduleResponse = new TeamDTO.ScheduleResponse();
                scheduleResponse.setDate(date);
                scheduleResponse.setSpecificSchedule(schedules);

                scheduleList.add(scheduleResponse);
            }
        }

        return scheduleList;
    }

    private Set<TeamDTO.SpecificSchedule> getSchedulesForMonth(String date, String teamCode) {
        String redisKey = getRedisKeySchedule(date, teamCode);
        Set<String> redisData = redisTemplate.opsForSet().members(redisKey);

        Set<TeamDTO.SpecificSchedule> result = new LinkedHashSet<>();

        if (redisData != null && !redisData.isEmpty()) {
            redisData.stream()
                    .map(this::parseJsonToSchedule)
                    .filter(Objects::nonNull)
                    .map(schedule -> convertToSpecificSchedule(schedule, teamCode))
                    .forEach(result::add);
        } else {
            List<TeamSchedule> dbSchedules = teamScheduleRepository.findByDateAndTeam(date, teamCode);
            dbSchedules.stream()
                    .map(schedule -> convertToSpecificSchedule(schedule, teamCode))
                    .forEach(result::add);
        }

        return result;
    }

    private TeamSchedule parseJsonToSchedule(String json) {
        try {
            return objectMapper.readValue(json, TeamSchedule.class);
        } catch (IOException e) {
            logger.error("Failed to parse JSON to TeamSchedule: {}", json, e);
            return null;
        }
    }

    private TeamDTO.SpecificSchedule convertToSpecificSchedule(TeamSchedule schedule, String teamCode) {
        return TeamDTO.SpecificSchedule.builder()
                .specificDate(schedule.getSpecificDate())
                .homeTeam(schedule.getHomeTeam())
                .awayTeam(schedule.getAwayTeam())
                .homeScore(schedule.getHomeScore())
                .awayScore(schedule.getAwayScore())
                .time(schedule.getGameTime())
                .stadium(schedule.getStadium())
                .cancellationReason(schedule.getCancellationReason())
                .logoUrl(teamCode.equals(schedule.getHomeTeam()) ? schedule.getAwayImg() : schedule.getHomeImg())
                .build();
    }

    /*** HELPER METHODS ***/
    private <T> void saveToRedis(String key, T data, Long expirationTime) {
        try {
            String json = objectMapper.writeValueAsString(data);
            redisTemplate.opsForValue().set(key, json);
            if (expirationTime != null) {
                redisTemplate.expire(key, expirationTime, TimeUnit.MILLISECONDS);
            }
        } catch (JsonProcessingException e) {
            logError(data.getClass().getSimpleName(), key, e);
        }
    }

    private void saveForSet(String key, TeamSchedule schedule) {
        try {
            String json = objectMapper.writeValueAsString(schedule);
            redisTemplate.opsForSet().add(key, json);
        } catch (JsonProcessingException e) {
            logError("TeamSchedule", key, e);
        }
    }

    private void deleteFromSet(String key, TeamSchedule schedule) {
        try {
            String json = objectMapper.writeValueAsString(schedule);
            redisTemplate.opsForSet().remove(key, json);
        } catch (JsonProcessingException e) {
            logError("TeamSchedule", key, e);
        }
    }

    private void logError(String type, String key, Exception e) {
        logger.error("Failed to process Redis operation. Type: {}, Key: {}, Error: {}", type, key, e.getMessage(), e);
    }
}