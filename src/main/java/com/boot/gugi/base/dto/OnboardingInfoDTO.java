package com.boot.gugi.base.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

public class OnboardingInfoDTO {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DefineUserRequest {
        private String nickName;
        private String introduction;
        private String team;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DefineUserResponse {
        private String nickName;
        private String introduction;
        private String team;
        private String profileImg;
    }
}
