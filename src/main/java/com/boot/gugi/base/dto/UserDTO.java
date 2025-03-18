package com.boot.gugi.base.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

public class UserDTO {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserRequest {
        private String nickName;
        private String team;
        private String introduction;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserResponse {
        private String nickName;
        private String profileImg;
        private String team;
        private String introduction;
    }
}
