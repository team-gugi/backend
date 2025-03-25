package com.boot.gugi.repository;

import com.boot.gugi.model.MatePost;
import com.boot.gugi.model.MateRequest;
import com.boot.gugi.model.User;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

public interface MateRequestRepository extends JpaRepository<MateRequest, UUID> {

    boolean existsByApplicantAndMatePost(User applicant, MatePost matePost);
    @Query("SELECT mr FROM MateRequest mr WHERE mr.applicant = :user AND mr.matePost.expired = false")
    List<MateRequest> findAllByApplicantAndNotExpired(@Param("user") User user);
    @Transactional
    void deleteAllByApplicant(User applicant);
    @Transactional
    void deleteAllByMatePost(MatePost matePost);
    List<MateRequest> findByMatePost(MatePost matePost);
}