package com.boot.gugi.token.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class BlacklistTokenRepository {

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    private static final String BLACKLIST_PREFIX = "Black-list:";

    public void save(String token, Duration expiration) {
        redisTemplate.opsForValue().set(BLACKLIST_PREFIX + token, "true", expiration);
    }

    public boolean exists(String token) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(BLACKLIST_PREFIX + token));
    }
}
