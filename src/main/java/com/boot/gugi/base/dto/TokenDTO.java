package com.boot.gugi.base.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class TokenDTO {
    @Builder
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonNaming(value = PropertyNamingStrategies.SnakeCaseStrategy.class)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class TokenResponse {
        @JsonProperty("access_token")
        private String accessToken;

        public static TokenDTO.TokenResponse of(String accessToken) {
            return TokenResponse.builder()
                    .accessToken(accessToken)
                    .build();
        }
    }
}