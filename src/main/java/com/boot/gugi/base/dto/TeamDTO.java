package com.boot.gugi.base.dto;

import jakarta.persistence.Column;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

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
        private Integer difference;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RankResponse {
        private Integer teamRank;
        private String team;
        private String teamLogo;
        private Integer game;
        private Integer win;
        private Integer lose;
        private Integer draw;
        private BigDecimal winningRate;
        private Integer difference;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class teamDetailsDTO {
        private String teamCode;
        private String teamName;
        private String description;
        private String instagram;
        private String youtube;
        private String ticketShop;
        private String mdShop;
    }
}