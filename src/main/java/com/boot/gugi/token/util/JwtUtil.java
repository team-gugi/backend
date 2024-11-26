package com.boot.gugi.token.util;

import com.boot.gugi.model.User;
import com.boot.gugi.repository.UserRepository;
import com.boot.gugi.token.exception.TokenErrorResult;
import com.boot.gugi.token.exception.TokenException;
import com.boot.gugi.exception.UserErrorResult;
import com.boot.gugi.exception.UserException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import javax.crypto.SecretKey;
import java.util.Date;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtUtil {
    @Value("${jwt.secret}")
    private String SECRET_KEY;

    private final UserRepository userRepository;

    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(this.SECRET_KEY);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    // access-token 발급 메서드
    public String generateAccessToken(UUID userId, long expirationMillis) {
        log.info("액세스 토큰이 발행되었습니다.");

        return Jwts.builder()
                .claim("userId", userId.toString())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationMillis))
                .signWith(this.getSigningKey())
                .compact();
    }

    // register-token 발급 메서드
    public String generateRegisterToken(String provider, String providerId, String name, String email, Integer gender, Integer age, long expirationMillis) {
        log.info("레지스터 토큰이 발행되었습니다.");

        return Jwts.builder()
                .claim("provider", provider)
                .claim("providerId", providerId)
                .claim("name", name)
                .claim("email", email)
                .claim("gender", gender)
                .claim("age", age)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationMillis))
                .signWith(this.getSigningKey())
                .compact();
    }

    // refresh-token 발급 메서드
    public String generateRefreshToken(UUID userId, long expirationMillis) {
        log.info("리프레쉬 토큰이 발행되었습니다.");

        return Jwts.builder()
                .claim("userId", userId.toString())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationMillis))
                .signWith(this.getSigningKey())
                .compact();
    }

    private <T> T getClaimFromToken(String token, String claim, Class<T> clazz) {
        try {
            return Jwts.parser()
                    .verifyWith(this.getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload()
                    .get(claim, clazz);
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("유효하지 않은 토큰이거나 클레임을 가져오는 중 오류 발생: {}", e.getMessage());
            throw new TokenException(TokenErrorResult.INVALID_TOKEN);
        }
    }

    public String getProviderFromToken(String token) {
        return getClaimFromToken(token, "provider",String.class);
    }

    public String getProviderIdFromToken(String token) {
        return getClaimFromToken(token, "providerId", String.class);
    }

    public String getNameFromToken(String token) {
        return getClaimFromToken(token, "name", String.class);
    }

    public String getEmailFromToken(String token) {
        return getClaimFromToken(token, "email", String.class);
    }

    public Integer getGenderFromToken(String token) {
        return getClaimFromToken(token, "gender", Integer.class);
    }

    public Integer getAgeFromToken(String token) {
        return getClaimFromToken(token, "age", Integer.class);
    }

    // Jwt 토큰의 유효기간을 확인하는 메서드
    public boolean isTokenExpired(String token) {
        try {
            Date expirationDate = Jwts.parser()
                    .verifyWith(this.getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload()
                    .getExpiration();
            log.info("토큰의 유효기간을 확인합니다.");
            return expirationDate.before(new Date());
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("유효하지 않은 토큰입니다.");
            throw new TokenException(TokenErrorResult.INVALID_TOKEN);
        }
    }

    public String getTokenFromHeader(String authorizationHeader) {
        return authorizationHeader.substring(7);
    }

    public String getUserIdFromToken(String token) {
        return getClaimFromToken(token, "userId", String.class);
    }

    public User getUserFromHeader(String authorizationHeader) {
        String token = getTokenFromHeader(authorizationHeader);
        UUID userId = UUID.fromString(getUserIdFromToken(token));

        return userRepository.findByUserId(userId)
                .orElseThrow(() -> new UserException(UserErrorResult.NOT_FOUND_USER));
    }
}