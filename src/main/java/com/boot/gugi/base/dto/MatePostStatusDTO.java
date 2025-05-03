package com.boot.gugi.base.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

public class MatePostStatusDTO {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MateRequestSummaryDTO {
        private List<AppliedRequestNotificationDTO> notification;
        private List<RequestedPostStatusDTO> pending;
        private List<AcceptedPostStatusDTO> accepted;
        private List<RequestedPostStatusDTO> rejected;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AppliedRequestNotificationDTO {
        private UUID requestId;
        private String title;
        private String nickName;
        private ApplicantInfo applicantInfo;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ApplicantInfo {
        private String age;
        private String gender;
        private String team;
        private String introduction;
        private String profileImg;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RequestedPostStatusDTO {
        private Boolean isOwner;
        private UUID mateId;
        private String title;
        private String content;
        private Integer daysSinceWritten;
        private Integer daysUntilGame;
        private Integer confirmedMembers;
        private MateDTO.ResponseOption options;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AcceptedPostStatusDTO {
        private Boolean isOwner;
        private UUID mateId;
        private String title;
        private String content;
        private Integer daysSinceWritten;
        private Integer daysUntilGame;
        private Integer confirmedMembers;
        private String contact;
        private MateDTO.ResponseOption options;
    }
}