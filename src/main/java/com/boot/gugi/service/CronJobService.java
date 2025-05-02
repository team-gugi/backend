package com.boot.gugi.service;

import com.boot.gugi.base.dto.TeamDTO;
import com.boot.gugi.model.MatePost;
import com.boot.gugi.model.TeamRank;
import com.boot.gugi.model.TeamSchedule;
import com.boot.gugi.repository.*;
import jakarta.annotation.PostConstruct;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;

@Service
@RequiredArgsConstructor
public class CronJobService {

    private final MatePostRepository matePostRepository;
    private final TeamRankRepository teamRankRepository;
    private final TeamScheduleRepository teamScheduleRepository;
    private final RedisRepository redisRepository;
    private final ScrapeService scrapeService;

    private static final Logger logger = LoggerFactory.getLogger(CronJobService.class);

    @PostConstruct
    public void init() {
        asyncInit();
    }

    @Async
    public void asyncInit() {
        try {
            handleExpiredPosts(LocalDate.now());
            updateRank();
            handleTeamCron();
            handleStadiumCron();
            handleFoodCron();
            updateSchedule();
        } catch (Exception e) {
            logger.error("초기화 중 오류 발생", e);
        }
    }

    @Scheduled(cron = "0 50 23 * * *")
    @Transactional
    public void handleExpiredPostsCron() {
        handleExpiredPosts(LocalDate.now().plusDays(1));
    }

    @Scheduled(cron = "0 */1 15-23 * * *")
    @Transactional
    public void handleRankCron() {
        updateRank();
    }

    @Scheduled(cron = "0 */15 15-23 * * *")
    @Transactional
    public void handleScheduleCron() {
        updateSchedule();
    }

    @Scheduled(cron = "0 0 15 * * *")
    @Transactional
    public void handleTeamCron() { redisRepository.cronSyncAllTeamsToRedis(); }

    @Scheduled(cron = "0 0 15 * * *")
    @Transactional
    public void handleStadiumCron() { redisRepository.cronSyncAllStadiumsToRedis(); }

    @Scheduled(cron = "0 0 15 * * *")
    @Transactional
    public void handleFoodCron() { redisRepository.cronSyncAllFoodsToRedis(); }

    private void handleExpiredPosts(LocalDate date) {
        List<MatePost> expiredPosts = matePostRepository.findByGameDateBefore(date);

        for (MatePost post : expiredPosts) {
            if (!post.isExpired()) {
                post.setExpired(true);
                matePostRepository.save(post);
            }
        }
    }

    public void updateRank() {
        List<TeamDTO.RankRequest> scrapedData = scrapeService.scrapeRank();
        Set<String> newRankKeys = new HashSet<>();

        for (TeamDTO.RankRequest dto : scrapedData) {
            TeamRank newRank = convertToTeamRank(dto);
            String key = newRank.getRankKey();
            newRankKeys.add(key);

            Optional<TeamRank> existingOpt = teamRankRepository.findByRankKey(key);

            if (existingOpt.isPresent()) {
                TeamRank existing = existingOpt.get();

                if (updateIfChanged(existing, newRank)) {
                    teamRankRepository.save(existing);
                    logger.info("[Rank] MongoDB 갱신: {}", key);
                }

                if (redisRepository.isSyncRequiredRank(existing, key)) {
                    redisRepository.saveRank(existing);
                    logger.info("[Rank] Redis 갱신: {}", key);
                }
            } else {
                teamRankRepository.save(newRank);
                redisRepository.saveRank(newRank);
                logger.info("[Rank] 새로 저장 in MongoDB, Redis: {}", newRank);
            }
        }

        removeDeletedRanks(newRankKeys);
    }

    private String getRankKey(TeamDTO.RankRequest dto) {
        return dto.getTeam();
    }

    private boolean updateIfChanged(TeamRank existing, TeamRank updated) {
        boolean changed = false;

        if (!Objects.equals(existing.getTeamRank(), updated.getTeamRank())) {
            logger.info("TeamRank changed from {} to {}", existing.getTeamRank(), updated.getTeamRank());
            existing.setTeamRank(updated.getTeamRank()); changed = true;
        }
        if (!Objects.equals(existing.getGame(), updated.getGame())) {
            logger.info("Game changed from {} to {}", existing.getGame(), updated.getGame());
            existing.setGame(updated.getGame()); changed = true;
        }
        if (!Objects.equals(existing.getWin(), updated.getWin())) {
            logger.info("Win changed from {} to {}", existing.getWin(), updated.getWin());
            existing.setWin(updated.getWin()); changed = true;
        }
        if (!Objects.equals(existing.getLose(), updated.getLose())) {
            logger.info("Lose changed from {} to {}", existing.getLose(), updated.getLose());
            existing.setLose(updated.getLose()); changed = true;
        }
        if (!Objects.equals(existing.getDraw(), updated.getDraw())) {
            logger.info("Draw changed from {} to {}", existing.getDraw(), updated.getDraw());
            existing.setDraw(updated.getDraw()); changed = true;
        }
        if (existing.getWinningRate().compareTo(updated.getWinningRate()) != 0) {
            logger.info("WinningRate changed from {} to {}", existing.getWinningRate(), updated.getWinningRate());
            existing.setWinningRate(updated.getWinningRate()); changed = true;
        }

        if (existing.getDifference().compareTo(updated.getDifference()) != 0) {
            logger.info("Difference changed from {} to {}", existing.getDifference(), updated.getDifference().doubleValue());
            existing.setDifference(updated.getDifference()); changed = true;
        }

        return changed;
    }

    private void removeDeletedRanks(Set<String> newKeys) {
        List<TeamRank> toDelete = teamRankRepository.findAllExceptKeys(newKeys);

        for (TeamRank rank : toDelete) {
            teamRankRepository.delete(rank);
            redisRepository.deleteRank(rank);
            logger.info("Deleted old rank: {}", rank);
        }
    }

    private TeamRank convertToTeamRank(TeamDTO.RankRequest dto) {
        String key = getRankKey(dto);

        return TeamRank.builder()
                .team(dto.getTeam())
                .teamRank(dto.getTeamRank())
                .game(dto.getGame())
                .win(dto.getWin())
                .lose(dto.getLose())
                .draw(dto.getDraw())
                .winningRate(dto.getWinningRate())
                .difference(dto.getDifference())
                .rankKey(key)
                .build();
    }

    public void updateSchedule() {
        List<TeamDTO.ScheduleRequest> scrapedData = scrapeService.scrapeSchedule();
        Set<String> newScheduleKeys = new HashSet<>();

        for (TeamDTO.ScheduleRequest dto : scrapedData) {
            TeamSchedule newSchedule = convertToTeamSchedule(dto);
            String key = newSchedule.getScheduleKey();
            newScheduleKeys.add(key);

            Optional<TeamSchedule> existingOpt = teamScheduleRepository.findByScheduleKey(key);

            if (existingOpt.isPresent()) {
                TeamSchedule existing = existingOpt.get();

                if (updateIfChanged(existing, newSchedule)) {
                    teamScheduleRepository.save(existing);
                    logger.info("[Schedule] MongoDB 갱신: {}", existing.getScheduleKey());
                }

                if (redisRepository.isSyncRequiredSchedule(existing, newSchedule, key)) {
                    redisRepository.saveSchedule(existing);
                    logger.info("[Schedule] Redis 갱신: {}", existing.getScheduleKey());
                }
            } else {
                teamScheduleRepository.save(newSchedule);
                redisRepository.saveSchedule(newSchedule);
                logger.info("[Schedule] 새로 저장 in MongoDB, Redis: {}", newSchedule);
            }
        }

        removeDeletedSchedules(newScheduleKeys);
    }

    private String getScheduleKey(TeamDTO.ScheduleRequest dto) {
        return dto.getDate() + "_" + dto.getSpecificDate() + "_" + dto.getTime() + "_" + dto.getHomeTeam() + "_" + dto.getAwayTeam();
    }

    private boolean updateIfChanged(TeamSchedule existing, TeamSchedule updated) {
        boolean updatedFlag = false;

        if (!Objects.equals(existing.getAwayScore(), updated.getAwayScore())) {
            logger.info("AwayScore changed from {} to {}", existing.getAwayScore(), updated.getAwayScore());
            existing.setAwayScore(updated.getAwayScore());
            updatedFlag = true;
        }
        if (!Objects.equals(existing.getHomeScore(), updated.getHomeScore())) {
            logger.info("HomeScore changed from {} to {}", existing.getHomeScore(), updated.getHomeScore());
            existing.setHomeScore(updated.getHomeScore());
            updatedFlag = true;
        }
        if (!Objects.equals(existing.getGameTime(), updated.getGameTime())) {
            logger.info("GameTime changed from {} to {}", existing.getGameTime(), updated.getGameTime());
            existing.setGameTime(updated.getGameTime());
            updatedFlag = true;
        }
        if (!Objects.equals(existing.getStadium(), updated.getStadium())) {
            logger.info("Stadium changed from {} to {}", existing.getStadium(), updated.getStadium());
            existing.setStadium(updated.getStadium());
            updatedFlag = true;
        }
        if (!Objects.equals(existing.getCancellationReason(), updated.getCancellationReason())) {
            logger.info("CancellationReason changed from {} to {}", existing.getCancellationReason(), updated.getCancellationReason());
            existing.setCancellationReason(updated.getCancellationReason());
            updatedFlag = true;
        }

        return updatedFlag;
    }

    private void removeDeletedSchedules(Set<String> newKeys) {
        List<TeamSchedule> toDelete = teamScheduleRepository.findAllExceptKeys(newKeys);

        for (TeamSchedule schedule : toDelete) {
            teamScheduleRepository.delete(schedule);
            redisRepository.deleteSchedule(schedule);
            logger.info("Deleted old schedule: {}", schedule);
        }
    }

    private TeamSchedule convertToTeamSchedule(TeamDTO.ScheduleRequest scheduleResponse) {

        String key = getScheduleKey(scheduleResponse);

        return  TeamSchedule.builder()
                .date(scheduleResponse.getDate())
                .specificDate(scheduleResponse.getSpecificDate())
                .homeTeam(scheduleResponse.getHomeTeam())
                .awayTeam(scheduleResponse.getAwayTeam())
                .homeImg(scheduleResponse.getHomeImg())
                .awayImg(scheduleResponse.getAwayImg())
                .homeScore(scheduleResponse.getHomeScore())
                .awayScore(scheduleResponse.getAwayScore())
                .gameTime(scheduleResponse.getTime())
                .stadium(scheduleResponse.getStadium())
                .cancellationReason(scheduleResponse.getCancellationReason())
                .scheduleKey(key)
                .build();
    }
}