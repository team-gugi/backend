package com.boot.gugi.repository;

import com.boot.gugi.base.dto.StadiumDTO;
import com.boot.gugi.base.dto.TeamDTO;
import com.boot.gugi.model.*;
import com.boot.gugi.service.RedisLockHelper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.*;
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

    private static final String RANK_REDIS_LOCK_KEY = "lock:rankSyncRedisWithMongo";
    private static final String SCHEDULE_REDIS_LOCK_KEY = "lock:scheduleSyncRedisWithMongo";

    private final TeamScheduleRepository teamScheduleRepository;
    private final RedisTemplate<String, String> redisTemplate;
    private final RedisLockHelper redisLockHelper;
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
    private String getRedisKeyRank(String rankKey) { return TEAM_RANK_PREFIX + rankKey;}

    public void saveRank(TeamRank rankInfo) {
        saveToRedis(getRedisKeyRank(rankInfo.getRankKey()) , rankInfo, TEAM_RANK_EXPIRATION_TIME);
    }
    public void deleteRank(TeamRank rank) {
        redisTemplate.delete(TEAM_RANK_PREFIX + rank.getRankKey());
    }

    public List<TeamDTO.RankResponse> findRanks() {
        List<TeamDTO.RankResponse> rankResponses = new ArrayList<>();
        ValueOperations<String, String> valueOperations = redisTemplate.opsForValue();

        String pattern = TEAM_RANK_PREFIX + "*";
        Cursor<byte[]> cursor = redisTemplate.getConnectionFactory()
                .getConnection()
                .scan(ScanOptions.scanOptions().match(pattern).build());

        while (cursor.hasNext()) {
            String key = new String(cursor.next());
            String json = valueOperations.get(key);

            if (json != null) {
                try {
                    TeamDTO.RankResponse rank = objectMapper.readValue(json, TeamDTO.RankResponse.class);
                    rankResponses.add(rank);
                } catch (JsonProcessingException e) {
                    logger.error("Redis JSON 파싱 실패: {}", e.getMessage());
                    return Collections.emptyList();
                }
            }
        }

        return rankResponses;
    }

    public void syncRankToRedis(List<TeamRank> dbRanks, Set<String> currentRedisKeys) {
        redisLockHelper.executeWithLock(
                RANK_REDIS_LOCK_KEY,
                5,
                10,
                () -> {
                    doSyncRankToRedis(dbRanks, currentRedisKeys);
                    return null;
                }
        );
    }

    public void doSyncRankToRedis(List<TeamRank> dbRanks, Set<String> currentRedisKeys) {
        logger.info("Rank Redis 동기화 시작. MongoDB 데이터 {}개.", dbRanks.size());

        if (currentRedisKeys != null && !currentRedisKeys.isEmpty()) {
            try {
                Long deletedCount = redisTemplate.delete(currentRedisKeys);
                logger.info("기존 Redis 데이터 {}개 삭제 완료.", deletedCount != null ? deletedCount : 0);
            } catch (Exception e) {
                logger.error("Redis 기존 키 삭제 중 오류 발생", e);
            }
        } else {
            logger.info("Redis에 기존 데이터 없음. 삭제 단계 건너뛰기.");
        }

        if (dbRanks != null && !dbRanks.isEmpty()) {
            Map<String, String> dataToSave = new HashMap<>();
            for (TeamRank rank : dbRanks) {
                try {
                    String rankJson = objectMapper.writeValueAsString(rank);
                    dataToSave.put(getRedisKeyRank(rank.getRankKey()), rankJson);
                } catch (JsonProcessingException e) {
                    logger.error("TeamRank 객체를 JSON으로 변환 중 오류 발생: {}", rank, e);
                }
            }

            if (!dataToSave.isEmpty()) {
                try {
                    redisTemplate.opsForValue().multiSet(dataToSave);
                    logger.info("Redis에 새로운 데이터 {}개 일괄 저장 완료.", dataToSave.size());
                } catch (Exception e) {
                    logger.error("Redis 새 데이터 일괄 저장 중 오류 발생", e);
                }
            } else {
                logger.info("JSON 변환에 성공한 데이터가 없어 Redis에 저장할 데이터가 없습니다.");
            }
        } else {
            logger.info("MongoDB에 데이터가 없어 Redis에 저장할 데이터가 없습니다.");
        }

        logger.info("Rank Redis 동기화 완료.");
    }

    /*** SCHEDULE ***/
    private String getRedisKeySchedule(String date, String teamName) { return SCHEDULE_PREFIX + date + ":" + teamName;}

    public void saveSchedule(TeamSchedule schedule) {
        String homeKey = getRedisKeySchedule(schedule.getDate(), schedule.getHomeTeam());
        String awayKey = getRedisKeySchedule(schedule.getDate(), schedule.getAwayTeam());
        saveForHash(homeKey, schedule);
        saveForHash(awayKey, schedule);
    }

    public void deleteSchedule(TeamSchedule schedule) {
        String homeKey = getRedisKeySchedule(schedule.getDate(), schedule.getHomeTeam());
        String awayKey = getRedisKeySchedule(schedule.getDate(), schedule.getAwayTeam());
        deleteFromHash(homeKey, schedule);
        deleteFromHash(awayKey, schedule);
    }

    public List<TeamDTO.ScheduleResponse> findTeamSchedule(String teamCode) {
        List<TeamDTO.ScheduleResponse> scheduleList = new ArrayList<>();

        for (int year = 2024; year <= 2025; year++) {
            for (int month = 1; month <= 12; month++) {
                String date = String.format("%04d.%02d", year, month);
                String redisKey = getRedisKeySchedule(date, teamCode);

                Long redisCount = redisTemplate.opsForHash().size(redisKey);
                Long mongoCount = teamScheduleRepository.countByDateAndTeam(date, teamCode);

                Set<TeamDTO.SpecificSchedule> specificSchedules;

                if (!redisCount.equals(mongoCount)) {
                    logger.info("Schedule Redis 동기화 필요: {} / {}. MongoDB: {}개, Redis: {}개", teamCode, date, mongoCount, redisCount);
                    List<TeamSchedule> dbSchedules = teamScheduleRepository.findByDateAndTeam(date, teamCode);
                    syncScheduleToRedis(dbSchedules, redisKey);

                    specificSchedules = dbSchedules.stream()
                            .map(schedule -> convertToSpecificSchedule(schedule, teamCode))
                            .collect(Collectors.toCollection(LinkedHashSet::new));
                } else {
                    specificSchedules = getSchedulesForMonth(date, teamCode);
                }

                TeamDTO.ScheduleResponse scheduleResponse = new TeamDTO.ScheduleResponse();
                scheduleResponse.setDate(date);
                scheduleResponse.setSpecificSchedule(specificSchedules);
                scheduleList.add(scheduleResponse);
            }
        }

        return scheduleList;
    }

    private Set<TeamDTO.SpecificSchedule> getSchedulesForMonth(String date, String teamCode) {
        String redisKey = getRedisKeySchedule(date, teamCode);
        Map<Object, Object> redisData = redisTemplate.opsForHash().entries(redisKey);

        Set<TeamDTO.SpecificSchedule> result = new LinkedHashSet<>();

        redisData.values().stream()
                .map(String.class::cast)
                .map(this::parseJsonToSchedule)
                .filter(Objects::nonNull)
                .map(schedule -> convertToSpecificSchedule(schedule, teamCode))
                .forEach(result::add);

        return result;
    }

    public void syncScheduleToRedis(List<TeamSchedule> dbSchedules, String currentRedisKey) {
        redisLockHelper.executeWithLock(
                SCHEDULE_REDIS_LOCK_KEY,
                10,
                60,
                () -> {
                    doSyncScheduleToRedis(dbSchedules, currentRedisKey);
                    return null;
                }
        );
    }

    public void doSyncScheduleToRedis(List<TeamSchedule> dbSchedules, String currentRedisKey) {
        logger.info("Schedule Redis 동기화 시작. MongoDB 데이터 {}개.", dbSchedules.size());

        if (Boolean.TRUE.equals(redisTemplate.hasKey(currentRedisKey))) {
            try {
                redisTemplate.delete(currentRedisKey);
                logger.info("기존 Redis schedule 데이터 삭제 완료. {}", currentRedisKey);
            } catch (Exception e) {
                logger.error("Redis 기존 키 삭제 중 오류 발생", e);
            }
        } else {
            logger.info("Redis에 기존 데이터 없음. 삭제 단계 건너뛰기.");
        }

        if (dbSchedules != null && !dbSchedules.isEmpty()) {
            try {
                Map<String, String> redisMap = dbSchedules.stream()
                        .collect(Collectors.toMap(
                                TeamSchedule::getScheduleKey,
                                schedule -> {
                                    try {
                                        return objectMapper.writeValueAsString(schedule);
                                    } catch (JsonProcessingException e) {
                                        logger.error("JSON 직렬화 오류: {}", schedule, e);
                                        return null;
                                    }
                                },
                                (existing, replacement) -> replacement
                        ));

                redisTemplate.opsForHash().putAll(currentRedisKey, redisMap);
                logger.info("Redis에 새 hash 데이터 {}개 저장 완료.", redisMap.size());
            } catch (Exception e) {
                logger.error("Redis 새 데이터 일괄 저장 중 오류 발생", e);
            }
        } else {
            logger.info("MongoDB에 데이터가 없어 Redis에 저장할 데이터가 없습니다.");
        }

        logger.info("Schedule Redis 동기화 완료.");
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

    private void saveForHash(String key, TeamSchedule schedule) {
        try {
            String json = objectMapper.writeValueAsString(schedule);
            redisTemplate.opsForHash().put(key, schedule.getScheduleKey(), json);
        } catch (JsonProcessingException e) {
            logError("TeamSchedule", key, e);
        }
    }

    private void deleteFromHash(String key, TeamSchedule schedule) {
        try {
            redisTemplate.opsForHash().delete(key, schedule.getScheduleKey());
        } catch (Exception e) {
            logError("DeleteFromHash", key + " - " + schedule.getScheduleKey(), e);
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