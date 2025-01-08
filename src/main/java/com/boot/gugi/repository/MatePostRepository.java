package com.boot.gugi.repository;

import com.boot.gugi.model.MatePost;
import com.boot.gugi.model.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface MatePostRepository extends JpaRepository<MatePost, UUID>, QuerydslPredicateExecutor<MatePost> {

    List<MatePost> findFirst5ByOrderByUpdatedAtDesc(Pageable pageable);
    List<MatePost> findByUpdatedAtLessThan(LocalDateTime cursor, Pageable pageable);
    List<MatePost> findAllByUser(User user);
}