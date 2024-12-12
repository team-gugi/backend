package com.boot.gugi.base.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

public class StadiumDTO {

    @Data
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
        private String stadiumName;
        private String stadiumLocation;
        private String teamName;
        private Set<FoodResponse> foodList;
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
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FoodResponse {
        private String foodName;
        private String foodLocation;
        private String foodImg;
    }
}