package com.boot.gugi.base.dto;

import lombok.*;

import java.util.Set;

public class StadiumDTO {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StadiumRequest {
        private Integer stadiumCode;
        private String stadiumName;
        private String stadiumLocation;
        private String teamName;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StadiumResponse {
        private StadiumInfo stadiumInfo;
        private Set<FoodResponse> foodList;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StadiumInfo {
        private String stadiumName;
        private String stadiumLocation;
        private String teamName;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FoodRequest {
        private String foodName;
        private String foodLocation;
        private Integer stadiumCode;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FoodResponse {
        private String foodName;
        private String foodLocation;
        private String foodImg;
    }
}