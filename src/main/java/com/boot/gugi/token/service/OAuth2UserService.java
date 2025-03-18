package com.boot.gugi.token.service;

import com.boot.gugi.token.auth.OAuth2UserInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import com.boot.gugi.token.auth.*;

import java.time.Duration;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class OAuth2UserService extends DefaultOAuth2UserService {

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    public void saveOauth2AccessToken(String provider, String providerId, String accessToken) {
        redisTemplate.opsForValue().set(provider + "-oauth:" + providerId, accessToken, Duration.ofHours(1));
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {

        OAuth2User oAuth2User = super.loadUser(userRequest);

        String oauthAccessToken = userRequest.getAccessToken().getTokenValue();
        String provider = userRequest.getClientRegistration().getRegistrationId();
        OAuth2UserInfo oAuth2UserInfo = null;

        switch (provider) {
            case "kakao" -> {
                log.info("카카오 로그인 요청");
                oAuth2UserInfo = new KakaoUserInfo(oAuth2User.getAttributes());
            }
            case "naver" -> {
                log.info("네이버 로그인 요청");
                oAuth2UserInfo = new NaverUserInfo((Map<String, Object>) oAuth2User.getAttributes().get("response"));
            }
            default -> throw new OAuth2AuthenticationException("지원하지 않는 OAuth2 provider: " + provider);
        }

        String providerId = oAuth2UserInfo.getProviderId();
        saveOauth2AccessToken(provider, providerId, oauthAccessToken);
        //log.info("oauth {},{},{}", provider, providerId, oauthAccessToken);

        return super.loadUser(userRequest);
    }
}
