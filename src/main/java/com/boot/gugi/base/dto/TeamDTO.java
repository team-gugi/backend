package com.boot.gugi.base.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Set;

public class TeamDTO {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RankRequest {
        private Integer teamRank;
        private String team;
        private Integer game;
        private Integer win;
        private Integer lose;
        private Integer draw;
        private BigDecimal winningRate;
        private BigDecimal difference;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RankResponse {
        private Integer teamRank;
        private String team;
        private Integer game;
        private Integer win;
        private Integer lose;
        private Integer draw;
        private BigDecimal winningRate;
        private BigDecimal difference;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ScheduleRequest {
        private String date;
        private String specificDate;
        private String homeTeam;
        private String awayTeam;
        private String homeImg;
        private String awayImg;
        private Integer homeScore;
        private Integer awayScore;
        private String time;
        private String stadium;
        private String cancellationReason;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ScheduleResponse {
        private String date;
        private Set<SpecificSchedule> specificSchedule;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SpecificSchedule {
        private String specificDate;
        private String homeTeam;
        private String awayTeam;
        private String logoUrl;
        private Integer homeScore;
        private Integer awayScore;
        private String time;
        private String stadium;
        private String cancellationReason;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class teamRequest {
        private String teamCode;
        private String teamName;
        private String description;
        private String instagram;
        private String youtube;
        private String ticketShop;
        private String mdShop;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class teamResponse {
        private String teamCode;
        private String teamLogo;
        private String teamName;
        private String description;
        private String instagram;
        private String youtube;
        private String ticketShop;
        private String mdShop;
    }
}