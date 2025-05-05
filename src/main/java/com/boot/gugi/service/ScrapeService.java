package com.boot.gugi.service;

import com.boot.gugi.base.Enum.StadiumEnum;
import com.boot.gugi.base.Enum.TeamEnum;
import com.boot.gugi.base.dto.TeamDTO;
import io.github.bonigarcia.wdm.WebDriverManager;
import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ScrapeService {

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

    private static final Logger logger = LoggerFactory.getLogger(ScrapeService.class);

    public List<TeamDTO.RankRequest> scrapeRank() {
        List<TeamDTO.RankRequest> rankRequestList = new ArrayList<>();
        try {
            Document doc = Jsoup.connect("https://www.koreabaseball.com/Record/TeamRank/TeamRankDaily.aspx")
                    .timeout(10000)
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
                            new BigDecimal(winningRate.text().trim()),
                            new BigDecimal(difference.text().trim())
                    );
                    rankRequestList.add(teamData);
                }
            }
        } catch (IOException e) {
            logger.error("Error while scraping KBO rankings: ", e);
        }
        return rankRequestList;
    }

    public List<TeamDTO.ScheduleRequest> scrapeSchedule() {

        WebDriverManager.chromedriver().setup();

        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--disable-gpu");

        WebDriver driver = new ChromeDriver(options);
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(5));

        List<TeamDTO.ScheduleRequest> scheduleResponseList = new ArrayList<>();
        try {
            driver.get("https://www.koreabaseball.com/Schedule/Schedule.aspx");

            Select seriesSelect = new Select(driver.findElement(By.id("ddlSeries")));
            Select yearSelect = new Select(driver.findElement(By.id("ddlYear")));
            Select monthSelect = new Select(driver.findElement(By.id("ddlMonth")));

            seriesSelect.selectByValue("0,9,6");
            wait.until(ExpectedConditions.presenceOfElementLocated(By.id("boxList")));

            for (int year = 2024; year <= 2025; year++) {
                yearSelect.selectByValue(String.valueOf(year));
                wait.until(ExpectedConditions.presenceOfElementLocated(By.className("tbl-type06")));

                for (int month = 3; month <= 10; month++) {
                    WebElement oldTableBody = driver.findElement(By.cssSelector(".tbl-type06 tbody"));
                    monthSelect.selectByValue(String.format("%02d", month));
                    wait.until(ExpectedConditions.stalenessOf(oldTableBody));

                    wait.until(ExpectedConditions.numberOfElementsToBeMoreThan(
                            By.cssSelector(".tbl-type06 tbody tr"), 0
                    ));

                    WebElement table = driver.findElement(By.className("tbl-type06"));
                    WebElement tbody = table.findElement(By.tagName("tbody"));
                    List<WebElement> rows = tbody.findElements(By.tagName("tr"));
                    List<WebElement> tds = tbody.findElements(By.tagName("td"));

                    if (rows.size() == 1 && tds.size() == 1) {
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
                        String cancellationReasonText = (cancellationReason != null && !cancellationReason.getText().equals("-"))
                                ? cancellationReason.getText()
                                : null;

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
