package com.boot.gugi.repository;

import com.boot.gugi.model.User;
import com.boot.gugi.model.UserOnboardingInfo;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserOnboardingInfoRepository extends JpaRepository<UserOnboardingInfo, Long> {
    UserOnboardingInfo findByUser(User user);
}