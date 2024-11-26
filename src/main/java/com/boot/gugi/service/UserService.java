package com.boot.gugi.service;

import com.boot.gugi.base.dto.OnboardingInfoDTO;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public interface UserService {

    OnboardingInfoDTO.DefineUserResponse createUser(HttpServletResponse response, String authorizationHeader, OnboardingInfoDTO.DefineUserRequest defineUserRequest, MultipartFile profileImg);
}