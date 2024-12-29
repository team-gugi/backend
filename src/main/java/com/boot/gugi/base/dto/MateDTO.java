package com.boot.gugi.base.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public class MateDTO {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MateRequest {
        private String title;
        private String content;
        private String contact;
        private MateOption options;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MateOption {
        private String gender;
        private String age;
        private LocalDate date;
        private String team;
        private Integer member;
        private String stadium;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MateResponse {
        private UUID mateId;
        private String title;
        private String content;
        private Integer daysSinceWritten;
        private Integer daysUntilGame;
        private Integer confirmedMembers;
        private LocalDateTime updatedAt;
        private MateOption options;
    }
}