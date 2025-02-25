package com.boot.gugi.service;

import com.boot.gugi.base.Enum.StadiumEnum;
import com.boot.gugi.base.Enum.TeamEnum;
import com.boot.gugi.base.dto.TeamDTO;
import com.boot.gugi.model.TeamRank;
import com.boot.gugi.model.TeamSchedule;
import com.boot.gugi.repository.RedisRepository;
import com.boot.gugi.repository.TeamRankRepository;
import com.boot.gugi.repository.TeamScheduleRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.Select;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TeamServiceImpl implements MainService{

    @Value("${team.kia.image.url}")
    private String kiaImageUrl;

    @Value("${team.kt.image.url}")
    private String ktImageUrl;

    @Value("${team.lg.image.url}")
    private String lgImageUrl;

    @Value("${team.nc.image.url}")
    private String ncImageUrl;

    @Value("${team.ssg.image.url}")
    private String ssgImageUrl;

    @Value("${team.doosan.image.url}")
    private String doosanImageUrl;

    @Value("${team.lotte.image.url}")
    private String lotteImageUrl;

    @Value("${team.samsung.image.url}")
    private String samsungImageUrl;

    @Value("${team.kiwoom.image.url}")
    private String kiwoomImageUrl;

    @Value("${team.hanwha.image.url}")
    private String hanwhaImageUrl;

    private static final Logger logger = LoggerFactory.getLogger(TeamServiceImpl.class);

    private final RedisTemplate<String, String> redisTemplate;
    private final TeamRankRepository teamRankRepository;
    private final TeamScheduleRepository teamScheduleRepository;
    private final RedisRepository redisRepository;
    private final S3Service s3Service;
    private final ChromeDriver chromeDriver;

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

    private String getScheduleKey(TeamSchedule teamSchedule) {
        return teamSchedule.getDate() + "_" + teamSchedule.getSpecificDate() + "_" + teamSchedule.getHomeTeam() + "_" + teamSchedule.getAwayTeam();
    }

    @Transactional
    @Scheduled(cron = "0 */10 * * * 2-7")
    public void saveSchedule() {

        /*List<TeamDTO.ScheduleRequest> newScrapedData = scrapeSchedule();
        for (TeamDTO.ScheduleRequest scheduleResponse : newScrapedData) {
            TeamSchedule teamSchedule = convertToTeamSchedule(scheduleResponse);
            redisRepository.saveSchedule(teamSchedule);
        }*/
        List<TeamDTO.ScheduleRequest> newScrapedData = scrapeSchedule();
        Set<String> newScheduleKeys = new HashSet<>();

        for (TeamDTO.ScheduleRequest scheduleResponse : newScrapedData) {
            TeamSchedule teamSchedule = convertToTeamSchedule(scheduleResponse);
            String scheduleKey = getScheduleKey(teamSchedule);
            newScheduleKeys.add(scheduleKey);

            Optional<TeamSchedule> existingSchedule = teamScheduleRepository.findByDateAndSpecificDateAndHomeTeamAndAwayTeam(
                    teamSchedule.getDate(),
                    teamSchedule.getSpecificDate(),
                    teamSchedule.getHomeTeam(),
                    teamSchedule.getAwayTeam()
            );

            if (existingSchedule.isPresent()) {
                TeamSchedule existing = existingSchedule.get();
                boolean isUpdated = updateIfChanged(existing, teamSchedule);

                if (isUpdated) {
                    teamScheduleRepository.save(existing);
                    logger.info("Updated schedule: {}", existing);
                }
            } else {
                teamScheduleRepository.save(teamSchedule);
            }

            redisRepository.saveSchedule(teamSchedule);
        }
        removeDeletedSchedules(newScheduleKeys);
    }

    private boolean updateIfChanged(TeamSchedule existing, TeamSchedule newSchedule) {
        boolean isUpdated = false;

        if (!Objects.equals(existing.getAwayScore(), newSchedule.getAwayScore())) {
            existing.setAwayScore(newSchedule.getAwayScore());
            isUpdated = true;
        }
        if (!Objects.equals(existing.getHomeScore(), newSchedule.getHomeScore())) {
            existing.setHomeScore(newSchedule.getHomeScore());
            isUpdated = true;
        }
        if (!Objects.equals(existing.getGameTime(), newSchedule.getGameTime())) {
            existing.setGameTime(newSchedule.getGameTime());
            isUpdated = true;
        }
        if (!Objects.equals(existing.getStadium(), newSchedule.getStadium())) {
            existing.setStadium(newSchedule.getStadium());
            isUpdated = true;
        }
        if (!Objects.equals(existing.getCancellationReason(), newSchedule.getCancellationReason())) {
            existing.setCancellationReason(newSchedule.getCancellationReason());
            isUpdated = true;
        }
        return isUpdated;
    }

    private void removeDeletedSchedules(Set<String> newScheduleKeys) {
        List<TeamSchedule> existingSchedules = teamScheduleRepository.findAll();

        for (TeamSchedule existingSchedule : existingSchedules) {
            String existingKey = getScheduleKey(existingSchedule);

            if (!newScheduleKeys.contains(existingKey)) {
                teamScheduleRepository.delete(existingSchedule);
                redisRepository.deleteSchedule(existingSchedule);
                logger.info("Deleted schedule from MySQL & Redis: {}", existingSchedule);
            }
        }
    }

    private TeamSchedule convertToTeamSchedule(TeamDTO.ScheduleRequest scheduleResponse) {

        return TeamSchedule.builder()
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
                .build();
    }

    public List<TeamDTO.ScheduleRequest> scrapeSchedule() {

        WebDriverManager.chromedriver().setup();

        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--disable-gpu");

        WebDriver driver = new ChromeDriver(options);

        List<TeamDTO.ScheduleRequest> scheduleResponseList = new ArrayList<>();
        try {
            driver.get("https://www.koreabaseball.com/Schedule/Schedule.aspx");

            Select yearSelect = new Select(driver.findElement(By.id("ddlYear")));
            Select monthSelect = new Select(driver.findElement(By.id("ddlMonth")));

            for (int year = 2024; year <= 2025; year++) {
                yearSelect.selectByValue(String.valueOf(year));

                for (int month = 1; month <= 12; month++) {
                    monthSelect.selectByValue(String.format("%02d", month));

                    WebElement table = driver.findElement(By.className("tbl-type06"));
                    WebElement tbody = table.findElement(By.tagName("tbody"));
                    List<WebElement> rows = tbody.findElements(By.tagName("tr"));
                    List<WebElement> tds = tbody.findElements(By.tagName("td"));

                    if (rows.size() == 1 && tds.size() == 1) {
                        logger.info("No game schedule found for year: {} month: {}", year, month);
                        continue;
                    }

                    WebElement formattedGameDay = null;

                    for (var row : rows) {
                        WebElement stadium = null;
                        WebElement awayScore = null;
                        WebElement homeScore = null;
                        WebElement cancellationReason = null;

                        //경기날짜
                        List<WebElement> dayElements = row.findElements(By.cssSelector("td.day"));
                        if (!dayElements.isEmpty()) {
                            formattedGameDay = dayElements.get(0);
                            stadium = row.findElement(By.cssSelector("td:nth-child(8)"));
                            cancellationReason = row.findElement(By.cssSelector("td:nth-child(9)"));
                        } else {
                            stadium = row.findElement(By.cssSelector("td:nth-child(7)"));
                            cancellationReason = row.findElement(By.cssSelector("td:nth-child(8)"));
                        }

                        var time = row.findElement(By.cssSelector("td.time"));
                        var awayTeam = row.findElement(By.cssSelector("td.play > span:nth-child(1)"));
                        var homeTeam = row.findElement(By.cssSelector("td.play > span:nth-child(3)"));

                        //경기여부
                        List<WebElement> spans = row.findElements(By.cssSelector("td.play > em > span"));
                        if (spans.size() > 1) {
                            awayScore = spans.get(0);
                            homeScore = spans.get(2);
                        }

                        String date = String.format("%d.%02d", year, month);
                        //logger.info("day : {}", date);
                        //logger.info("specificDay : {}", formattedGameDay.getText());
                        //logger.info("time : {}", time.getText());
                        //logger.info("away : {}-{}", awayTeam.getText(), awayScore != null ? awayScore.getText() : "N/A");
                        //logger.info("home : {}-{}", homeTeam.getText(), homeScore != null ? homeScore.getText() : "N/A");
                        //logger.info("stadium : {}", stadium.getText());
                        String cancellationReasonText = (cancellationReason != null && !cancellationReason.getText().equals("-"))
                                ? cancellationReason.getText()
                                : null;
                        //logger.info("cancellationReason : {}", cancellationReasonText);

                        //String date = String.format("%d.%02d", year, month);
                        TeamEnum english_away = TeamEnum.fromString(awayTeam.getText());
                        TeamEnum english_home = TeamEnum.fromString(homeTeam.getText());
                        String long_stadium = StadiumEnum.getDisplayNameKoreanByShortName(stadium.getText());

                        TeamDTO.ScheduleRequest teamSchedule = new TeamDTO.ScheduleRequest(
                                date,
                                formattedGameDay.getText(),
                                homeTeam.getText(),
                                awayTeam.getText(),
                                getImageUrl(english_home),
                                getImageUrl(english_away),
                                homeScore != null ? Integer.parseInt(homeScore.getText()) : -1,
                                awayScore != null ? Integer.parseInt(awayScore.getText()) : -1,
                                time.getText(),
                                long_stadium,
                                cancellationReasonText
                        );

                        scheduleResponseList.add(teamSchedule);
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Error while scraping KBO schedule: ", e);
        } finally {
            driver.quit();
        }
        return scheduleResponseList;
    }

    public String getImageUrl(TeamEnum team) {
        switch (team) {
            case KIA: return kiaImageUrl;
            case KT: return ktImageUrl;
            case LG: return lgImageUrl;
            case NC: return ncImageUrl;
            case SSG: return ssgImageUrl;
            case DOOSAN: return doosanImageUrl;
            case LOTTE: return lotteImageUrl;
            case SAMSUNG: return samsungImageUrl;
            case KIWOOM: return kiwoomImageUrl;
            case HANWHA: return hanwhaImageUrl;
            default: throw new IllegalArgumentException("No image URL for team: " + team);
        }
    }
}