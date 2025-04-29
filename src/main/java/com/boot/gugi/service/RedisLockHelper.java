package com.boot.gugi.service;

import lombok.RequiredArgsConstructor;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@Service
@RequiredArgsConstructor
public class RedisLockHelper {

    private final RedissonClient redissonClient;

    private static final Logger logger = LoggerFactory.getLogger(RedisLockHelper.class);

    public <T> T executeWithLock(String lockKey, long waitTimeSec, long leaseTimeSec, Supplier<T> task) {
        RLock lock = redissonClient.getLock(lockKey);
        boolean isLocked = false;

        try {
            isLocked = lock.tryLock(waitTimeSec, leaseTimeSec, TimeUnit.SECONDS);

            if (isLocked) {
                logger.info("Lock 획득 성공. 동기화 시작.");
                return task.get();
            } else {
                throw new IllegalStateException("Lock 획득 실패: " + lockKey);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Lock 대기 중 인터럽트 발생", e);
        } catch (Exception e) {
            throw new RuntimeException("Lock 처리 중 오류 발생", e);
        } finally {
            if (isLocked && lock.isHeldByCurrentThread()) {
                lock.unlock();
                logger.info("Lock 해제 완료.");
            }
        }
    }
}
