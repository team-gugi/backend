package com.boot.gugi.token.service;

import com.boot.gugi.service.RedisService;
import com.boot.gugi.token.exception.TokenErrorResult;
import com.boot.gugi.token.exception.TokenException;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@RequiredArgsConstructor
public class OAuth2UnlinkService {
    @Value("${naver.unlink}")
    private String NAVER_URL;
    @Value("${kakao.unlink}")
    private String KAKAO_URL;

    @Value("${spring.security.oauth2.client.registration.naver.client-id}")
    private String NAVER_CLIENT_ID;
    @Value("${spring.security.oauth2.client.registration.naver.client-secret}")
    private String NAVER_CLIENT_SECRET;

    private final RestTemplate restTemplate;
    private final RedisService redisService;

    public void unlink(String provider, String providerId) {
        if (provider.startsWith("kakao")) {
            kakaoUnlink(provider+"-oauth:"+providerId);
        } else if (provider.startsWith("naver")) {
            naverUnlink(provider+"-oauth:"+providerId);
        } else {
            throw new TokenException(TokenErrorResult.INVALID_REQUEST);
        }
    }

    public void naverUnlink(String key) {

        String accessToken = redisService.getValues(key);

        if (accessToken == null) {
            throw new TokenException(TokenErrorResult.EXPIRED_AUTH_TOKEN);
        }

        String url = NAVER_URL +
                "?service_provider=NAVER" +
                "&grant_type=delete" +
                "&client_id=" +
                NAVER_CLIENT_ID +
                "&client_secret=" +
                NAVER_CLIENT_SECRET +
                "&access_token=" +
                accessToken;

        UnlinkResponse response = restTemplate.getForObject(url, UnlinkResponse.class);

        if (response != null && !"success".equalsIgnoreCase(response.getResult())) {
            throw new TokenException(TokenErrorResult.INTERNAL_SERVER_ERROR);
        }
    }

    //네이버 응답 데이터
    @Getter
    @RequiredArgsConstructor
    public static class UnlinkResponse {
        @JsonProperty("access_token")
        private final String accessToken;
        private final String result;
    }

    public void kakaoUnlink(String key) {
        String accessToken = redisService.getValues(key);

        if (accessToken == null) {
            throw new TokenException(TokenErrorResult.EXPIRED_AUTH_TOKEN);
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        HttpEntity<Object> entity = new HttpEntity<>("", headers);
        restTemplate.exchange(KAKAO_URL, HttpMethod.POST, entity, String.class);
    }
}
