package com.boot.gugi.token.service;

import com.boot.gugi.model.User;
import com.boot.gugi.repository.UserOnboardingInfoRepository;
import com.boot.gugi.repository.UserRepository;
import com.boot.gugi.token.auth.KakaoUserInfo;
import com.boot.gugi.token.auth.NaverUserInfo;
import com.boot.gugi.token.auth.OAuth2UserInfo;
import com.boot.gugi.token.model.RefreshToken;
import com.boot.gugi.token.repository.RefreshTokenRepository;
import com.boot.gugi.token.util.CookieUtil;
import com.boot.gugi.token.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuthLoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    @Value("${jwt.redirect.access}")
    private String ACCESS_TOKEN_REDIRECT_URI;
    @Value("${jwt.redirect.register}")
    private String REGISTER_TOKEN_REDIRECT_URI;
    @Value("${jwt.register-token.expiration-time}")
    private long REGISTER_TOKEN_EXPIRATION_TIME;
    @Value("${jwt.access-token.expiration-time}")
    private long ACCESS_TOKEN_EXPIRATION_TIME;
    @Value("${jwt.refresh-token.expiration-time}")
    private long REFRESH_TOKEN_EXPIRATION_TIME;

    private final JwtUtil jwtUtil;
    private final CookieUtil cookieUtil;
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserOnboardingInfoRepository userOnboardingInfoRepository;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException {
        OAuth2AuthenticationToken authToken = (OAuth2AuthenticationToken) authentication;
        String provider = authToken.getAuthorizedClientRegistrationId();
        OAuth2UserInfo oAuth2UserInfo = getUserInfo(provider, authToken);

        String providerId = oAuth2UserInfo.getProviderId();
        String name = oAuth2UserInfo.getName();
        String email = oAuth2UserInfo.getEmail();
        Integer gender = oAuth2UserInfo.getGender();
        Integer currentYear = java.time.Year.now().getValue();
        Integer age = currentYear - oAuth2UserInfo.getBirthyear();

        User existingUser = userRepository.findByProviderId(providerId);
        if (existingUser == null) {
            handleNewUser(request, response, provider, providerId, name, email, gender, age);
        } else {
            handleExistingUser(request, response, existingUser);
        }

        logUserInfo(oAuth2UserInfo);
    }

    private OAuth2UserInfo getUserInfo(String provider, OAuth2AuthenticationToken authToken) {
        if ("kakao".equals(provider)) {
            log.info("카카오 로그인 요청");
            return new KakaoUserInfo(authToken.getPrincipal().getAttributes());
        } else if ("naver".equals(provider)) {
            log.info("네이버 로그인 요청");
            return new NaverUserInfo((Map<String, Object>) authToken.getPrincipal().getAttributes().get("response"));
        } else {
            throw new UnsupportedOperationException("지원하지 않는 로그인 제공자입니다: " + provider);
        }
    }

    private void handleNewUser(HttpServletRequest request, HttpServletResponse response,
                               String provider, String providerId, String name, String email, Integer gender, Integer age) throws IOException {
        log.info("신규 유저입니다. 등록을 진행합니다.");

        //register token
        String registerToken = jwtUtil.generateRegisterToken(provider, providerId, name, email, gender, age, REGISTER_TOKEN_EXPIRATION_TIME);
        String redirectUri = String.format(REGISTER_TOKEN_REDIRECT_URI, registerToken);
        getRedirectStrategy().sendRedirect(request, response, redirectUri);
    }

    private void handleExistingUser(HttpServletRequest request, HttpServletResponse response, User user) throws IOException {
        log.info("기존 유저입니다.");

        //refresh token (cookie)
        ResponseCookie refreshCookie = cookieUtil.createRefreshCookie(user.getUserId(), REFRESH_TOKEN_EXPIRATION_TIME);
        response.addHeader("Set-Cookie", refreshCookie.toString());
        //refresh token (redis)
        RefreshToken newRefreshToken = new RefreshToken(user.getUserId(), refreshCookie.getValue());
        refreshTokenRepository.save(newRefreshToken);

        //access token (cookie)
        ResponseCookie accessCookie = cookieUtil.createAccessCookie(user.getUserId(), ACCESS_TOKEN_EXPIRATION_TIME);
        response.addHeader("Set-Cookie", accessCookie.toString());

        String redirectUri = String.format(ACCESS_TOKEN_REDIRECT_URI);
        getRedirectStrategy().sendRedirect(request, response, redirectUri);
    }

    private void logUserInfo(OAuth2UserInfo oAuth2UserInfo) {
        log.info("이메일 : {}", oAuth2UserInfo.getEmail());
        log.info("이름 : {}", oAuth2UserInfo.getName());
        log.info("성별 : {}", oAuth2UserInfo.getGender());
        log.info("출생년도 : {}", oAuth2UserInfo.getBirthyear());
        log.info("PROVIDER : {}", oAuth2UserInfo.getProvider());
        log.info("PROVIDER_ID : {}", oAuth2UserInfo.getProviderId());
    }
}
