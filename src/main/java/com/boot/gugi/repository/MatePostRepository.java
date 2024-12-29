package com.boot.gugi.repository;

import com.boot.gugi.model.MatePost;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface MatePostRepository extends JpaRepository<MatePost, UUID> {

    List<MatePost> findFirst5ByOrderByUpdatedAtDesc(Pageable pageable);
    List<MatePost> findByUpdatedAtLessThan(LocalDateTime cursor, Pageable pageable);
}