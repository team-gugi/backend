package com.boot.gugi.repository;

import com.boot.gugi.model.User;
import com.boot.gugi.model.UserOnboardingInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserOnboardingInfoRepository extends JpaRepository<UserOnboardingInfo, Long> {
    UserOnboardingInfo findByUser(User user);

    @Query("SELECT u.nickName FROM UserOnboardingInfo u WHERE u.user = :user")
    String findNickNameByUser(@Param("user") User user);

    void deleteByUser(User user);
}