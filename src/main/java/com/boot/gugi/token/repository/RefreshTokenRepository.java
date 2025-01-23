package com.boot.gugi.token.repository;

import com.boot.gugi.service.RedisService;
import com.boot.gugi.token.model.RefreshToken;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
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

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    private final RedisService redisService;

    private String getRedisKey(String userId) {
        return "Refresh-token:" + userId;
    }

    //redis - refreshToken
    public void save(final RefreshToken refreshToken) {

        String key = getRedisKey(refreshToken.getUserId().toString());
        redisService.setValues(key, refreshToken.getRefreshToken());
        redisTemplate.expire(key, REFRESH_TOKEN_EXPIRATION_TIME, TimeUnit.MILLISECONDS);
    }

    public Optional<RefreshToken> findByUserId(final UUID userId) {

        String key = getRedisKey(userId.toString());
        String refreshToken = redisService.getValues(key);

        if (Objects.isNull(refreshToken)) {
            return Optional.empty();
        }
        return Optional.of(new RefreshToken(userId, refreshToken));
    }

    public void deleteByUserId(UUID userId) {

        String key = getRedisKey(userId.toString());
        redisService.deleteValues(key);
    }
}