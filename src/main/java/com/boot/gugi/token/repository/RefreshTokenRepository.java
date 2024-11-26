package com.boot.gugi.token.repository;

import com.boot.gugi.token.model.RefreshToken;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Repository;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Repository
@RequiredArgsConstructor
public class RefreshTokenRepository {

    @Value("${jwt.refresh-token.expiration-time}")
    private long REFRESH_TOKEN_EXPIRATION_TIME;

    private final RedisTemplate redisTemplate;

    //redis - refreshToken
    public void save(final RefreshToken refreshToken) {
        ValueOperations<String, String> valueOperations = redisTemplate.opsForValue();
        String refreshTokenKey = "Refresh-token:" + refreshToken.getUserId().toString();
        valueOperations.set(refreshTokenKey, refreshToken.getRefreshToken());
        redisTemplate.expire(refreshTokenKey, REFRESH_TOKEN_EXPIRATION_TIME, TimeUnit.MILLISECONDS);
    }

    public Optional<RefreshToken> findByUserId(final UUID userId) {
        ValueOperations<String, String> valueOperations = redisTemplate.opsForValue();

        String refreshTokenKey = "Refresh-token:" + userId.toString();
        String refreshToken = valueOperations.get(refreshTokenKey);

        if (Objects.isNull(refreshToken)) {
            return Optional.empty();
        }
        return Optional.of(new RefreshToken(userId, refreshToken));
    }
}