package com.boot.gugi.token.service;

import com.boot.gugi.token.exception.TokenErrorResult;
import com.boot.gugi.token.exception.TokenException;
import com.boot.gugi.token.model.RefreshToken;
import com.boot.gugi.token.repository.BlacklistTokenRepository;
import com.boot.gugi.token.repository.RefreshTokenRepository;
import com.boot.gugi.token.util.CookieUtil;
import com.boot.gugi.token.util.JwtUtil;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.Date;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class TokenServiceImpl implements TokenService {
    @Value("${jwt.access-token.expiration-time}")
    private long ACCESS_TOKEN_EXPIRATION_TIME;
    @Value("${jwt.refresh-token.expiration-time}")
    private long REFRESH_TOKEN_EXPIRATION_TIME;

    private final RefreshTokenRepository refreshTokenRepository;
    private final BlacklistTokenRepository blacklistTokenRepository;
    private final JwtUtil jwtUtil;
    private final CookieUtil cookieUtil;

    private static final Logger logger = LoggerFactory.getLogger(TokenServiceImpl.class);

    @Override
    public String reissueAccessToken(HttpServletRequest request, HttpServletResponse response) {
        Cookie cookie = cookieUtil.getRefreshCookie(request);
        String refreshToken = cookie.getValue();

        UUID userId = UUID.fromString(jwtUtil.getUserIdFromToken(refreshToken));
        RefreshToken existRefreshToken = refreshTokenRepository.findByUserId(userId)
                .orElseThrow(() -> new TokenException(TokenErrorResult.REFRESH_TOKEN_NOT_FOUND));

        if (!existRefreshToken.getRefreshToken().equals(refreshToken) || jwtUtil.isTokenExpired(refreshToken)) {
            throw new TokenException(TokenErrorResult.INVALID_REFRESH_TOKEN);
        }

        ResponseCookie newAccessCookie = cookieUtil.createAccessCookie(userId, ACCESS_TOKEN_EXPIRATION_TIME);
        response.addHeader("Set-Cookie", newAccessCookie.toString());

        return newAccessCookie.getValue();
    }

    private String getAccessToken(HttpServletRequest request, HttpServletResponse response) {
        Cookie cookie = cookieUtil.getAccessCookie(request);
        String accessToken = (cookie != null) ? cookie.getValue() : null;

        //로그아웃, 회원탈퇴된 액세스 토큰인지 확인
        if (isTokenBlacklisted(accessToken)) {
            logger.info("Access token is blacklisted: {}", accessToken);
            throw new TokenException(TokenErrorResult.BLACKLISTED_TOKEN);
        }

        if (accessToken == null || jwtUtil.isTokenExpired(accessToken)) {
            accessToken = reissueAccessToken(request, response);
        }

        return accessToken;
    }

    @Override
    public UUID getUserIdFromAccessToken(HttpServletRequest request, HttpServletResponse response) {
        String accessToken = getAccessToken(request, response);
        UUID userId = UUID.fromString(jwtUtil.getUserIdFromToken(accessToken));
        logger.info("Extracted userId: {}", userId);
        return userId;
    }

    public void addToBlacklist(String token, Date expirationDate) {
        long expirationMillis = expirationDate.getTime() - System.currentTimeMillis();
        if (expirationMillis > 0) {
            Duration expiration = Duration.ofMillis(expirationMillis);
            blacklistTokenRepository.save(token, expiration);
        } else {
            throw new IllegalArgumentException("토큰의 만료 시간이 이미 지났습니다.");
        }
    }

    public boolean isTokenBlacklisted(String token) {
        return blacklistTokenRepository.exists(token);
    }

}