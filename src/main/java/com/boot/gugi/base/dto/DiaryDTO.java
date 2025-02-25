package com.boot.gugi.base.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public class DiaryDTO {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DiaryRequest {
        private LocalDate gameDate;
        private String gameStadium;
        private String homeTeam;
        private String awayTeam;
        private Integer homeScore;
        private Integer awayScore;
        private String content;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DiarySingleDto {
        private UUID diaryId;
        private LocalDate gameDate;
        private String gameStadium;
        private String homeTeam;
        private String awayTeam;
        private String gameResult;
        private String gameImg;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DiaryDetailDto {
        private UUID diaryId;
        private LocalDate gameDate;
        private String gameStadium;
        private String homeTeam;
        private String awayTeam;
        private Integer homeScore;
        private Integer awayScore;
        private String gameImg;
        private String content;
        private String gameResult;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WinRateResponse {
        private String nickName;
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "0.00")
        private BigDecimal winRate;
        private Integer totalDiaryCount;
        private Integer totalWins;
    }
}