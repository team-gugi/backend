package com.boot.gugi.service;

import com.boot.gugi.base.dto.TeamDTO;
import com.boot.gugi.model.TeamRank;
import com.boot.gugi.repository.RedisRepository;
import com.boot.gugi.repository.TeamRankRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TeamServiceImpl implements MainService{

    private static final Logger logger = LoggerFactory.getLogger(TeamServiceImpl.class);

    private final RedisTemplate<String, String> redisTemplate;
    private final TeamRankRepository teamRankRepository;
    private final RedisRepository redisRepository;
    private final S3Service s3Service;

    private boolean isDbUpdated = false;

    @Scheduled(cron = "0 0 15 * * ?")
    public void resetUpdateFlag() {
        isDbUpdated = false;
    }

    @Scheduled(cron = "0 */1 15-23 * * 2-7")
    public void checkAndUpdateGameData() {
        if (!isDbUpdated) {
            List<TeamDTO.RankRequest> newScrapedData = scrapeRank();
            List<TeamDTO.RankRequest> currentDbData = getRank();

            if (isDataChanged(newScrapedData, currentDbData)) {
                updateDatabase(newScrapedData);
                List<TeamRank> newData = teamRankRepository.findAll();
                redisRepository.updateRank(newData);
                logger.info("KBO rankings updated successfully after change detection.");
            }
        }
    }

    private boolean isDataChanged(List<TeamDTO.RankRequest> scrapedData, List<TeamDTO.RankRequest> dbData) {
        return !scrapedData.equals(dbData);
    }

    @Transactional
    private void updateDatabase(List<TeamDTO.RankRequest> scrapedData) {
        for (TeamDTO.RankRequest teamRequest : scrapedData) {
            TeamRank existingTeam = teamRankRepository.findByTeam(teamRequest.getTeam());

            if (existingTeam != null) {
                existingTeam.setTeamRank(teamRequest.getTeamRank());
                existingTeam.setGame(teamRequest.getGame());
                existingTeam.setWin(teamRequest.getWin());
                existingTeam.setLose(teamRequest.getLose());
                existingTeam.setDraw(teamRequest.getDraw());
                existingTeam.setWinningRate(teamRequest.getWinningRate());
                existingTeam.setDifference(teamRequest.getDifference());

                teamRankRepository.save(existingTeam);
            }
        }
    }

    public List<TeamDTO.RankRequest> scrapeRank() {
        List<TeamDTO.RankRequest> rankRequestList = new ArrayList<>();
        try {
            Document doc = Jsoup.connect("https://www.koreabaseball.com/Record/TeamRank/TeamRankDaily.aspx")
                    .timeout(10000) // 10초 타임아웃 설정
                    .get();

            Elements baseballTeams = doc.select("#cphContents_cphContents_cphContents_udpRecord > table > tbody > tr");

            for (Element baseballTeam : baseballTeams) {
                Element rank = baseballTeam.selectFirst("td:nth-child(1)"); //순위
                Element team = baseballTeam.selectFirst("td:nth-child(2)"); // 팀명
                Element game = baseballTeam.selectFirst("td:nth-child(3)"); // 경기 수
                Element win = baseballTeam.selectFirst("td:nth-child(4)"); // 승
                Element lose = baseballTeam.selectFirst("td:nth-child(5)"); // 패
                Element draw = baseballTeam.selectFirst("td:nth-child(6)"); // 무
                Element winningRate = baseballTeam.selectFirst("td:nth-child(7)"); // 승률
                Element difference = baseballTeam.selectFirst("td:nth-child(8)"); // 게임차

                if (team != null) {
                    String teamName = team.text();

                    TeamDTO.RankRequest teamData = new TeamDTO.RankRequest(
                            Integer.parseInt(rank.text()),
                            teamName,
                            Integer.parseInt(game.text()),
                            Integer.parseInt(win.text()),
                            Integer.parseInt(lose.text()),
                            Integer.parseInt(draw.text()),
                            new BigDecimal(winningRate.text().replace("%", "").trim()),
                            Integer.parseInt(difference.text())
                    );
                    rankRequestList.add(teamData);
                }
            }
        } catch (IOException e) {
            logger.error("Error while scraping KBO rankings: ", e);
        }
        return rankRequestList;
    }

    private List<TeamDTO.RankRequest> getRank() {

        List<TeamDTO.RankRequest> ranks = redisRepository.findRank();
        if (ranks == null || ranks.isEmpty()) {
            List<TeamRank> teams = teamRankRepository.findAll();

            ranks = teams.stream()
                    .filter(team -> team.getTeamRank() >= 1 && team.getTeamRank() <= 10)
                    .sorted(Comparator.comparingInt(TeamRank::getTeamRank))
                    .map(team -> new TeamDTO.RankRequest(
                            team.getTeamRank(),
                            team.getTeam(),
                            team.getGame(),
                            team.getWin(),
                            team.getLose(),
                            team.getDraw(),
                            team.getWinningRate(),
                            team.getDifference()
                    ))
                    .collect(Collectors.toList());
        }
        return ranks;
    }

    public List<TeamDTO.RankResponse> getRanks() {

        List<TeamDTO.RankResponse> ranks = redisRepository.findRanks();
        if (ranks == null || ranks.isEmpty()) {
            List<TeamRank> teams = teamRankRepository.findAll();

            ranks = teams.stream()
                    .filter(team -> team.getTeamRank() >= 1 && team.getTeamRank() <= 10)
                    .sorted(Comparator.comparingInt(TeamRank::getTeamRank))
                    .map(team -> new TeamDTO.RankResponse(
                            team.getTeamRank(),
                            team.getTeam(),
                            team.getTeamLogo(),
                            team.getGame(),
                            team.getWin(),
                            team.getLose(),
                            team.getDraw(),
                            team.getWinningRate(),
                            team.getDifference()
                    ))
                    .collect(Collectors.toList());
        }
        return ranks;
    }

    public TeamRank saveRank(TeamDTO.RankRequest rankRequest, MultipartFile teamLogo) {

        String uploadedLogoUrl = s3Service.uploadImg(teamLogo, null);
        TeamRank savedRank = createTeam(rankRequest, uploadedLogoUrl);
        redisRepository.saveRank(savedRank);
        return teamRankRepository.save(savedRank);
    }

    private TeamRank createTeam(TeamDTO.RankRequest rankRequest, String logoURL) {
        return TeamRank.builder()
                .team(rankRequest.getTeam())
                .teamLogo(logoURL)
                .teamRank(rankRequest.getTeamRank())
                .game(rankRequest.getGame())
                .win(rankRequest.getWin())
                .lose(rankRequest.getLose())
                .draw(rankRequest.getDraw())
                .winningRate(rankRequest.getWinningRate())
                .difference(rankRequest.getDifference())
                .build();
    }
}