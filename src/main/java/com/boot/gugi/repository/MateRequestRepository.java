package com.boot.gugi.repository;

import com.boot.gugi.model.MatePost;
import com.boot.gugi.model.MateRequest;
import com.boot.gugi.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MateRequestRepository extends JpaRepository<MateRequest, UUID> {

    boolean existsByApplicantAndMatePost(User applicant, MatePost matePost);
    List<MateRequest> findAllByApplicant(User applicant);
}