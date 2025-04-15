package com.boot.gugi.service;

import com.boot.gugi.base.dto.TeamDTO;
import com.boot.gugi.model.MatePost;
import com.boot.gugi.model.TeamRank;
import com.boot.gugi.model.TeamSchedule;
import com.boot.gugi.repository.MatePostRepository;
import com.boot.gugi.repository.RedisRepository;
import com.boot.gugi.repository.TeamRankRepository;
import com.boot.gugi.repository.TeamScheduleRepository;
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
            updateSchedule();
        } catch (Exception e) {
            logger.error("초기화 중 오류 발생", e);
        }
    }

    @Scheduled(cron = "0 50 23 * * 2-7")
    public void handleExpiredPostsCron() {
        handleExpiredPosts(LocalDate.now().plusDays(1));
    }

    @Scheduled(cron = "0 */1 15-23 * * 2-7")
    @Transactional
    public void handleRankCron() {
        updateRank();
    }

    @Scheduled(cron = "0 */10 * * * 2-7")
    @Transactional
    public void handleScheduleCron() {
        updateSchedule();
    }

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
                    redisRepository.saveRank(newRank);
                    logger.info("Updated rank: {}", existing);
                }
            } else {
                teamRankRepository.save(newRank);
                redisRepository.saveRank(newRank);
                logger.info("New rank saved: {}", newRank);
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
            existing.setTeamRank(updated.getTeamRank()); changed = true;
        }
        if (!Objects.equals(existing.getGame(), updated.getGame())) {
            existing.setGame(updated.getGame()); changed = true;
        }
        if (!Objects.equals(existing.getWin(), updated.getWin())) {
            existing.setWin(updated.getWin()); changed = true;
        }
        if (!Objects.equals(existing.getLose(), updated.getLose())) {
            existing.setLose(updated.getLose()); changed = true;
        }
        if (!Objects.equals(existing.getDraw(), updated.getDraw())) {
            existing.setDraw(updated.getDraw()); changed = true;
        }
        if (!Objects.equals(existing.getWinningRate(), updated.getWinningRate())) {
            existing.setWinningRate(updated.getWinningRate()); changed = true;
        }
        if (!Objects.equals(existing.getDifference(), updated.getDifference())) {
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
                    redisRepository.saveSchedule(existing);
                    logger.info("Updated schedule: {}", existing);
                }
            } else {
                teamScheduleRepository.save(newSchedule);
                redisRepository.saveSchedule(newSchedule);
                logger.info("New schedule saved: {}", newSchedule);
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
            existing.setAwayScore(updated.getAwayScore());
            updatedFlag = true;
        }
        if (!Objects.equals(existing.getHomeScore(), updated.getHomeScore())) {
            existing.setHomeScore(updated.getHomeScore());
            updatedFlag = true;
        }
        if (!Objects.equals(existing.getGameTime(), updated.getGameTime())) {
            existing.setGameTime(updated.getGameTime());
            updatedFlag = true;
        }
        if (!Objects.equals(existing.getStadium(), updated.getStadium())) {
            existing.setStadium(updated.getStadium());
            updatedFlag = true;
        }
        if (!Objects.equals(existing.getCancellationReason(), updated.getCancellationReason())) {
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