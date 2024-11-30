package com.boot.gugi.service;

import com.amazonaws.AmazonServiceException;
import com.boot.gugi.base.dto.OnboardingInfoDTO;
import com.boot.gugi.model.User;
import com.boot.gugi.model.UserOnboardingInfo;
import com.boot.gugi.repository.UserOnboardingInfoRepository;
import com.boot.gugi.repository.UserRepository;
import com.boot.gugi.token.exception.TokenErrorResult;
import com.boot.gugi.token.exception.TokenException;
import com.boot.gugi.token.model.RefreshToken;
import com.boot.gugi.token.repository.RefreshTokenRepository;
import com.boot.gugi.token.util.CookieUtil;
import com.boot.gugi.token.util.JwtUtil;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;
import jakarta.transaction.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class DefineUserImpl implements UserService {

    private final JwtUtil jwtUtil;
    private final CookieUtil cookieUtil;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;
    private final UserOnboardingInfoRepository userOnboardingInfoRepository;
    private final S3Service s3Service;

    @Value("${jwt.access-token.expiration-time}")
    private long ACCESS_TOKEN_EXPIRATION_TIME;
    @Value("${jwt.refresh-token.expiration-time}")
    private long REFRESH_TOKEN_EXPIRATION_TIME;
    @Value("${default.profile.img.url}")
    private String DEFAULT_USER_IMAGE;

    @Override
    public OnboardingInfoDTO.DefineUserResponse createUser(HttpServletResponse response, String authorizationHeader, OnboardingInfoDTO.DefineUserRequest defineUserRequest, MultipartFile profileImg){

        String registerToken = jwtUtil.getTokenFromHeader(authorizationHeader);

        // 레지스터 토큰 서명 및 만료 확인
        if (jwtUtil.isTokenExpired(registerToken)) {
            throw new TokenException(TokenErrorResult.INVALID_REGISTER_TOKEN);
        }

        String provider = jwtUtil.getProviderFromToken(registerToken);
        String providerId = jwtUtil.getProviderIdFromToken(registerToken);
        String name = jwtUtil.getNameFromToken(registerToken);
        String email = jwtUtil.getEmailFromToken(registerToken);
        Integer gender = jwtUtil.getGenderFromToken(registerToken);
        Integer age = jwtUtil.getAgeFromToken(registerToken);

        User user = User.builder()
                .provider(provider)
                .providerId(providerId)
                .name(name)
                .email(email)
                .gender(gender)
                .age(age)
                .build();
        userRepository.save(user);

        String uploadedImageUrl = uploadProfileImage(profileImg);

        UserOnboardingInfo onboardingInfo = UserOnboardingInfo.builder()
                .user(user)
                .nickName(defineUserRequest.getNickName())
                .introduction(defineUserRequest.getIntroduction())
                .team(defineUserRequest.getTeam())
                .profileImg(uploadedImageUrl)
                .build();
        userOnboardingInfoRepository.save(onboardingInfo);

        ResponseCookie refreshCookie = cookieUtil.createRefreshCookie(user.getUserId(), REFRESH_TOKEN_EXPIRATION_TIME);
        response.addHeader("Set-Cookie", refreshCookie.toString());

        RefreshToken newRefreshToken = new RefreshToken(user.getUserId(), refreshCookie.getValue());
        refreshTokenRepository.save(newRefreshToken);

        ResponseCookie accessCookie = cookieUtil.createAccessCookie(user.getUserId(), ACCESS_TOKEN_EXPIRATION_TIME);
        response.addHeader("Set-Cookie", accessCookie.toString());


        return new OnboardingInfoDTO.DefineUserResponse(onboardingInfo.getNickName(), onboardingInfo.getIntroduction(), onboardingInfo.getTeam(), onboardingInfo.getProfileImg());

    }

    private String uploadProfileImage(MultipartFile profileImg) {
        if (profileImg != null && !profileImg.isEmpty()) {
            try {
                String fileName = UUID.randomUUID().toString() + "_" + profileImg.getOriginalFilename();
                return s3Service.uploadFile(profileImg.getInputStream(), fileName);
            } catch (IOException e) {
                throw new RuntimeException("프로필 이미지 업로드 실패: " + e.getMessage(), e);
            } catch (AmazonServiceException e) {
                throw new RuntimeException("S3 서비스 오류: " + e.getMessage(), e);
            }
        } else {
            return DEFAULT_USER_IMAGE;
        }
    }
}