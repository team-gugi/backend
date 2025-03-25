package com.boot.gugi.repository;

import com.boot.gugi.model.MatePost;
import com.boot.gugi.model.User;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface MatePostRepository extends JpaRepository<MatePost, UUID>, QuerydslPredicateExecutor<MatePost>, MatePostRepositoryCustom {

    List<MatePost> findAllByUser(User user);
    @Query("SELECT m FROM MatePost m WHERE m.user = :user AND m.expired = false")
    List<MatePost> findAllByUserAndNotExpired(@Param("user") User user);
    List<MatePost> findByGameDateBefore(LocalDate date);
}