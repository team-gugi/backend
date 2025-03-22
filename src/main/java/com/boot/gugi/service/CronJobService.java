package com.boot.gugi.service;

import com.boot.gugi.model.MatePost;
import com.boot.gugi.repository.MatePostRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CronJobService {

    private final MatePostRepository matePostRepository;

    @Scheduled(cron = "0 50 23 * * 2-7")
    public void handleExpiredPostsCron() {
        handleExpiredPosts(LocalDate.now().plusDays(1));
    }

    @PostConstruct
    public void init() {
        handleExpiredPosts(LocalDate.now());
    }

    private void handleExpiredPosts(LocalDate date) {
        List<MatePost> expiredPosts = matePostRepository.findByGameDateBefore(date);

        for (MatePost post : expiredPosts) {
            if (!post.isExpired()) {
                post.setExpired(true);
                matePostRepository.save(post);
            }
        }
    }

}