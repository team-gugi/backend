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

    private static final String TEAM_REDIS_LOCK_KEY = "lock:teamSyncRedisWithMongo";
    private static final String STADIUM_REDIS_LOCK_KEY = "lock:stadiumSyncRedisWithMongo";
    private static final String FOOD_REDIS_LOCK_KEY = "lock:foodSyncRedisWithMongo";
    private static final String RANK_REDIS_LOCK_KEY = "lock:rankSyncRedisWithMongo";
    private static final String SCHEDULE_REDIS_LOCK_KEY = "lock:scheduleSyncRedisWithMongo";

    private final TeamScheduleRepository teamScheduleRepository;
    private final TeamRepository teamRepository;
    private final StadiumRepository stadiumRepository;
    private final StadiumFoodRepository stadiumFoodRepository;
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
                Team teamDetails = objectMapper.readValue(teamInfoJson, Team.class);
                logger.info("* Redis * 팀 정보 조회 성공. TeamCode: {}", teamCode);
                return TeamDTO.teamResponse.builder()
                        .teamCode(teamDetails.getTeamCode())
                        .teamLogo(teamDetails.getTeamLogo())
                        .teamName(teamDetails.getTeamName())
                        .description(teamDetails.getDescription())
                        .instagram(teamDetails.getInstagram())
                        .youtube(teamDetails.getYoutube())
                        .ticketShop(teamDetails.getTicketShop())
                        .mdShop(teamDetails.getMdShop())
                        .build();
            } catch (JsonProcessingException e) {
                logger.error("Redis JSON 파싱 실패. TeamCode: {}, Error: {}", teamCode, e.getMessage(), e);
            }
        }
        return null;
    }

    public void syncTeamToRedis(Team dbTeam) {
        redisLockHelper.executeWithLock(
                TEAM_REDIS_LOCK_KEY,
                5,
                10,
                () -> {
                    doSyncTeamToRedis(dbTeam);
                    return null;
                }
        );
    }

    public void doSyncTeamToRedis(Team dbTeam) {
        logger.info("Team Redis 동기화 시작. {}", dbTeam.getTeamCode());
        saveTeam(dbTeam);
        logger.info("Team Redis 동기화 완료. {}", dbTeam.getTeamCode());
    }

    /*** Stadium ***/
    public void saveStadium(Stadium stadiumInfo) {
        saveToRedis(STADIUM_CODE_PREFIX + stadiumInfo.getStadiumCode(), stadiumInfo, null);
    }

    public StadiumDTO.StadiumInfo findStadium(Integer stadiumCode) {
        ValueOperations<String, String> valueOperations = redisTemplate.opsForValue();
        String stadiumInfoKey = STADIUM_CODE_PREFIX + stadiumCode;
        String stadiumInfoJson = valueOperations.get(stadiumInfoKey);

        if (stadiumInfoJson != null) {
            try {
                Stadium stadiumResponse = objectMapper.readValue(stadiumInfoJson, Stadium.class);
                logger.info("* Redis * 구장 정보 조회 성공. StadiumCode: {}", stadiumCode);
                return StadiumDTO.StadiumInfo.builder()
                        .stadiumName(stadiumResponse.getStadiumName())
                        .stadiumLocation(stadiumResponse.getStadiumLocation())
                        .teamName(stadiumResponse.getTeamName())
                        .build();
            } catch (JsonProcessingException e) {
                logger.error("Redis JSON 파싱 실패. StadiumCode: {}, Error: {}", stadiumCode, e.getMessage(), e);
            }
        }
        return null;
    }

    public void syncStadiumToRedis(Stadium dbTeam) {
        redisLockHelper.executeWithLock(
                STADIUM_REDIS_LOCK_KEY,
                5,
                10,
                () -> {
                    doSyncStadiumToRedis(dbTeam);
                    return null;
                }
        );
    }

    public void doSyncStadiumToRedis(Stadium dbStadium) {
        logger.info("Stadium Redis 동기화 시작. {}", dbStadium.getStadiumName());
        saveStadium(dbStadium);
        logger.info("Stadium Redis 동기화 완료. {}", dbStadium.getStadiumName());
    }

    /*** Food ***/
    public void saveFood(Food foodDetails, Integer stadiumCode) {
        String foodInfoKey = FOOD_CODE_PREFIX + stadiumCode;
        String hashKey = String.valueOf(foodDetails.getId());
        try {
            String foodInfoJson = objectMapper.writeValueAsString(foodDetails);
            redisTemplate.opsForHash().put(foodInfoKey, hashKey, foodInfoJson);
        } catch (JsonProcessingException e) {
            logError("FoodInfo", foodInfoKey, e);
        }
    }

    public Set<StadiumDTO.FoodResponse> findFood(Integer stadiumCode) {
        String foodInfoKey = FOOD_CODE_PREFIX + stadiumCode;
        Map<Object, Object> redisData = redisTemplate.opsForHash().entries(foodInfoKey);

        Set<StadiumDTO.FoodResponse> foodList = new LinkedHashSet<>();
        redisData.values().stream()
                .map(String.class::cast)
                .map(this::parseJsonToFood)
                .filter(Objects::nonNull)
                .map(food -> convertToSpecificFood(food))
                .forEach(foodList::add);

        logger.info("* Redis * 식음료 정보 조회 성공. StadiumCode: {}", stadiumCode);
        return foodList;
    }

    private Food parseJsonToFood(String json) {
        try {
            return objectMapper.readValue(json, Food.class);
        } catch (JsonProcessingException e) {
            logger.error("Redis JSON 파싱 실패. Food: {}, Error: {}", json, e.getMessage(), e);
            return null;
        }
    }

    private StadiumDTO.FoodResponse convertToSpecificFood(Food food) {
        return StadiumDTO.FoodResponse.builder()
                .foodName(food.getFoodName())
                .foodLocation(food.getFoodLocation())
                .foodImg(food.getFoodImg())
                .build();
    }

    public void syncFoodToRedis(List<Food> dbFoods, String currentRedisKey) {
        redisLockHelper.executeWithLock(
                FOOD_REDIS_LOCK_KEY,
                10,
                60,
                () -> {
                    doSyncFoodToRedis(dbFoods, currentRedisKey);
                    return null;
                }
        );
    }

    public void doSyncFoodToRedis(List<Food> dbFoods, String currentRedisKey) {
        logger.info("<동기화 시작> Redis Food. MongoDB 데이터 {}개.", dbFoods.size());

        if (Boolean.TRUE.equals(redisTemplate.hasKey(currentRedisKey))) {
            try {
                redisTemplate.delete(currentRedisKey);
                logger.info("<동기화 중> Redis Food 데이터 삭제 완료. {}", currentRedisKey);
            } catch (Exception e) {
                logger.error("<동기화 오류> Redis Food 키 삭제 중 오류 발생", e);
            }
        } else {
            logger.info("<동기화 중> Redis에 Food 데이터 없음. 삭제 단계 건너뛰기.");
        }

        if (dbFoods != null && !dbFoods.isEmpty()) {
            Map<String, String> redisMap = new HashMap<>();
            for (Food food : dbFoods) {
                try {
                    String key = food.getId().toString();
                    String value = objectMapper.writeValueAsString(food);
                    redisMap.put(key, value);
                } catch (JsonProcessingException e) {
                    logger.error("<동기화 오류> JSON 직렬화 오류: foodId={}, name={}", food.getId(), food.getFoodName(), e);
                }
            }

            if (!redisMap.isEmpty()) {
                redisTemplate.opsForHash().putAll(currentRedisKey, redisMap);
                logger.info("<동기화 중> Redis Food 데이터 저장 완료. {}개", redisMap.size());
            }
        } else {
            logger.info("<동기화 중> MongoDB에 Food 데이터 없음. Redis 저장 단계 건너뛰기.");
        }
        logger.info("<동기화 완료> Redis Food.");
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
                    TeamRank rank = objectMapper.readValue(json, TeamRank.class);
                    TeamDTO.RankResponse rankInfo = TeamDTO.RankResponse.builder()
                            .teamRank(rank.getTeamRank())
                            .team(rank.getTeam())
                            .game(rank.getGame())
                            .win(rank.getWin())
                            .lose(rank.getLose())
                            .draw(rank.getDraw())
                            .winningRate(rank.getWinningRate())
                            .difference(rank.getDifference())
                            .build();

                    rankResponses.add(rankInfo);
                } catch (JsonProcessingException e) {
                    logger.error("Redis JSON 파싱 실패: Rank: {}, Error: {}", json, e.getMessage(), e);
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
        logger.info("<동기화 시작> Redis Rank. MongoDB 데이터 {}개.", dbRanks.size());

        if (currentRedisKeys != null && !currentRedisKeys.isEmpty()) {
            try {
                Long deletedCount = redisTemplate.delete(currentRedisKeys);
                logger.info("<동기화 중> Redis Rank 데이터 삭제 완료. {}", deletedCount != null ? deletedCount : 0);
            } catch (Exception e) {
                logger.error("<동기화 오류> Redis Rank 키 삭제 중 오류 발생", e);
            }
        } else {
            logger.info("<동기화 중> Redis에 Rank 데이터 없음. 삭제 단계 건너뛰기.");
        }

        if (dbRanks != null && !dbRanks.isEmpty()) {
            Map<String, String> dataToSave = new HashMap<>();
            for (TeamRank rank : dbRanks) {
                try {
                    String rankJson = objectMapper.writeValueAsString(rank);
                    dataToSave.put(getRedisKeyRank(rank.getRankKey()), rankJson);
                } catch (JsonProcessingException e) {
                    logger.error("<동기화 오류> JSON 직렬화 오류: TeamRank={}", rank, e);
                }
            }

            if (!dataToSave.isEmpty()) {
                redisTemplate.opsForValue().multiSet(dataToSave);
                logger.info("<동기화 중> Redis Rank 데이터 저장 완료. {}개", dataToSave.size());
            }
        } else {
            logger.info("<동기화 중> MongoDB에 Rank 데이터 없음. Redis 저장 단계 건너뛰기.");
        }
        logger.info("<동기화 완료> Redis Rank.");
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
                    logger.info("Schedule Redis 동기화 필요. {} / {}. MongoDB: {}개, Redis: {}개", teamCode, date, mongoCount, redisCount);
                    List<TeamSchedule> dbSchedules = teamScheduleRepository.findByDateAndTeam(date, teamCode);
                    syncScheduleToRedis(dbSchedules, redisKey);

                    specificSchedules = dbSchedules.stream()
                            .map(schedule -> convertToSpecificSchedule(schedule, teamCode))
                            .collect(Collectors.toCollection(LinkedHashSet::new));
                } else {
                    specificSchedules = getSchedulesForMonth(date, teamCode);
                    logger.info("* Redis * 스케줄 정보 조회 성공 {}.", date);
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
        logger.info("<동기화 시작> Redis Schedule. MongoDB 데이터 {}개.", dbSchedules.size());

        if (Boolean.TRUE.equals(redisTemplate.hasKey(currentRedisKey))) {
            try {
                redisTemplate.delete(currentRedisKey);
                logger.info("<동기화 중> Redis Schedule 데이터 삭제 완료. {}", currentRedisKey);
            } catch (Exception e) {
                logger.error("<동기화 오류> Redis Schedule 키 삭제 중 오류 발생", e);
            }
        } else {
            logger.info("<동기화 중> Redis에 Schedule 데이터 없음. 삭제 단계 건너뛰기.");
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
                                        logger.error("<동기화 오류> JSON 직렬화 오류: Schedule={}", schedule, e);
                                        return null;
                                    }
                                },
                                (existing, replacement) -> replacement
                        ));

                redisTemplate.opsForHash().putAll(currentRedisKey, redisMap);
                logger.info("<동기화 중> Redis Schedule 데이터 저장 완료. {}개", redisMap.size());
            } catch (Exception e) {
                logger.error("<동기화 오류> Redis Schedule 데이터 일괄 저장 중 오류 발생", e);
            }
        } else {
            logger.info("<동기화 중> MongoDB에 Schedule 데이터 없음. Redis 저장 단계 건너뛰기.");
        }
        logger.info("<동기화 완료> Redis Schedule.");
    }

    private TeamSchedule parseJsonToSchedule(String json) {
        try {
            return objectMapper.readValue(json, TeamSchedule.class);
        } catch (IOException e) {
            logger.error("Redis JSON 파싱 실패. Schedule: {}", json, e);
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

    /*** isSyncRequired ***/
    public void cronSyncAllTeamsToRedis() {
        List<Team> dbTeams = teamRepository.findAll();

        for (Team dbTeam : dbTeams) {
            try {
                String redisKey = TEAM_CODE_PREFIX + dbTeam.getTeamCode();
                String redisValue = redisTemplate.opsForValue().get(redisKey);

                String dbJson = objectMapper.writeValueAsString(dbTeam);
                if (redisValue == null || !redisValue.equals(dbJson)) {
                    redisTemplate.opsForValue().set(redisKey, dbJson);
                    logger.info("[Team] Redis 갱신: {}", dbTeam.getTeamCode());
                }
            } catch (JsonProcessingException e) {
                logger.error("[Team] JSON 직렬화 실패. TeamCode: {}", dbTeam.getTeamCode(), e);
            }
        }
    }

    public void cronSyncAllStadiumsToRedis() {
        List<Stadium> dbStadiums = stadiumRepository.findAll();

        for (Stadium dbStadium : dbStadiums) {
            try {
                String redisKey = STADIUM_CODE_PREFIX + dbStadium.getStadiumCode();
                String redisValue = redisTemplate.opsForValue().get(redisKey);

                StadiumDTO.StadiumRequest dto = StadiumDTO.StadiumRequest.builder()
                        .stadiumCode(dbStadium.getStadiumCode())
                        .stadiumLocation(dbStadium.getStadiumLocation())
                        .stadiumName(dbStadium.getStadiumName())
                        .teamName(dbStadium.getTeamName())
                        .build();

                String dbJson = objectMapper.writeValueAsString(dto);
                if (redisValue == null || !redisValue.equals(dbJson)) {
                    redisTemplate.opsForValue().set(redisKey, dbJson);
                    logger.info("[Stadium] Redis 갱신: {}", dbStadium.getStadiumName());
                }
            } catch (JsonProcessingException e) {
                logger.error("[Stadium] JSON 직렬화 실패. Stadium: {}", dbStadium.getStadiumName(), e);
            }
        }
    }

    public void cronSyncAllFoodsToRedis() {
        List<Stadium> dbStadiums = stadiumRepository.findAll();

        for (Stadium dbStadium : dbStadiums) {
            Integer stadiumCode = dbStadium.getStadiumCode();
            String redisKey = FOOD_CODE_PREFIX + stadiumCode;

            List<StadiumFood> stadiumFoods = stadiumFoodRepository.findByStadiumCodeWithFood(stadiumCode);

            for (StadiumFood stadiumFood : stadiumFoods) {
                Food food = stadiumFood.getFood();
                Long foodId = food.getId();
                String hashKey = foodId.toString();

                try {
                    Object redisObj = redisTemplate.opsForHash().get(redisKey, hashKey);
                    String redisJson = redisObj != null ? redisObj.toString() : null;

                    String foodJson = objectMapper.writeValueAsString(food);
                    if (redisJson == null || !redisJson.equals(foodJson)) {
                        redisTemplate.opsForHash().put(redisKey, foodId.toString(), foodJson);
                        logger.info("[Food] Redis 갱신: stadium={}, foodId={}, foodName={}", dbStadium.getStadiumName(), foodId, food.getFoodName());
                    }
                } catch (JsonProcessingException e) {
                    logger.error("[Food] JSON 직렬화 실패: foodId={}, foodName={}", foodId, food.getFoodName(), e);
                }
            }
        }
    }

    public boolean isSyncRequiredRank(TeamRank current, String redisKey) {
        try {
            String key = getRedisKeyRank(redisKey);
            String redisJson = redisTemplate.opsForValue().get(key);
            if (redisJson == null) return true;

            String currentJson = objectMapper.writeValueAsString(current);
            return !currentJson.equals(redisJson);
        } catch (JsonProcessingException e) {
            logger.error("[TeamRank] JSON 직렬화 실패. TeamRank: {}", redisKey, e);
            return true;
        }
    }

    public boolean isSyncRequiredSchedule(TeamSchedule current, TeamSchedule newData, String hashKey) {
        try {
            String homeKey = getRedisKeySchedule(newData.getDate(), newData.getHomeTeam());
            String awayKey = getRedisKeySchedule(newData.getDate(), newData.getAwayTeam());

            Object homeObj = redisTemplate.opsForHash().get(homeKey, hashKey);
            Object awayObj = redisTemplate.opsForHash().get(awayKey, hashKey);
            if (homeObj == null || awayObj == null) return true;

            String homeJson = homeObj.toString();
            String awayJson = awayObj.toString();
            String currentJson = objectMapper.writeValueAsString(current);

            return !currentJson.equals(homeJson) && !currentJson.equals(awayJson);
        } catch (JsonProcessingException e) {
            logger.error("[Schedule] JSON 직렬화 실패. Schedule: {}", hashKey, e);
            return true;
        }
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

    private void logError(String type, String key, Exception e) {
        logger.error("Failed to process Redis operation. Type: {}, Key: {}, Error: {}", type, key, e.getMessage(), e);
    }
}