package com.boot.gugi.service;

import com.boot.gugi.base.Enum.AgeRangeEnum;
import com.boot.gugi.base.Enum.SexEnum;
import com.boot.gugi.base.dto.OnboardingInfoDTO;
import com.boot.gugi.base.dto.UserDTO;
import com.boot.gugi.exception.UserErrorResult;
import com.boot.gugi.exception.UserException;
import com.boot.gugi.model.MatePost;
import com.boot.gugi.model.User;
import com.boot.gugi.model.UserOnboardingInfo;
import com.boot.gugi.repository.*;
import com.boot.gugi.token.exception.TokenErrorResult;
import com.boot.gugi.token.exception.TokenException;
import com.boot.gugi.token.model.RefreshToken;
import com.boot.gugi.token.repository.RefreshTokenRepository;
import com.boot.gugi.token.service.OAuth2UnlinkService;
import com.boot.gugi.token.service.TokenServiceImpl;
import com.boot.gugi.token.util.CookieUtil;
import com.boot.gugi.token.util.JwtUtil;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;
import jakarta.transaction.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.Date;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class UserService {

    private final JwtUtil jwtUtil;
    private final CookieUtil cookieUtil;
    private final TokenServiceImpl tokenServiceImpl;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;
    private final UserOnboardingInfoRepository userOnboardingInfoRepository;
    private final S3Service s3Service;
    private final OAuth2UnlinkService oAuth2UnlinkService;
    private final RedisService redisService;
    private final DiaryRepository diaryRepository;
    private final MatePostRepository matePostRepository;
    private final MateRequestRepository mateRequestRepository;

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    @Value("${jwt.access-token.expiration-time}")
    private long ACCESS_TOKEN_EXPIRATION_TIME;
    @Value("${jwt.refresh-token.expiration-time}")
    private long REFRESH_TOKEN_EXPIRATION_TIME;
    @Value("${default.profile.img.url}")
    private String DEFAULT_USER_IMAGE;

    public OnboardingInfoDTO.DefineUserResponse createUser(HttpServletResponse response, String authorizationHeader, OnboardingInfoDTO.DefineUserRequest defineUserRequest, MultipartFile profileImg){

        String registerToken = jwtUtil.getTokenFromHeader(authorizationHeader);

        // 레지스터 토큰 서명 및 만료 확인
        if (jwtUtil.isTokenExpired(registerToken)) {
            throw new TokenException(TokenErrorResult.INVALID_REGISTER_TOKEN);
        }

        String provider = jwtUtil.getProviderFromToken(registerToken);
        String providerId = jwtUtil.getProviderIdFromToken(registerToken);
        String email = jwtUtil.getEmailFromToken(registerToken);

        User user = User.builder()
                .provider(provider)
                .providerId(providerId)
                .email(email)
                .build();
        userRepository.save(user);

        String uploadedImageUrl = s3Service.uploadImg(profileImg,DEFAULT_USER_IMAGE);

        SexEnum sex = SexEnum.fromKorean(defineUserRequest.getSex());

        AgeRangeEnum age = AgeRangeEnum.fromString(defineUserRequest.getAge());
        UserOnboardingInfo onboardingInfo = UserOnboardingInfo.builder()
                .user(user)
                .nickName(defineUserRequest.getNickName())
                .introduction(defineUserRequest.getIntroduction())
                .team(defineUserRequest.getTeam())
                .sex(sex)
                .age(age)
                .profileImg(uploadedImageUrl)
                .build();
        userOnboardingInfoRepository.save(onboardingInfo);

        ResponseCookie refreshCookie = cookieUtil.createRefreshCookie(user.getUserId(), REFRESH_TOKEN_EXPIRATION_TIME);
        response.addHeader("Set-Cookie", refreshCookie.toString());

        RefreshToken newRefreshToken = new RefreshToken(user.getUserId(), refreshCookie.getValue());
        refreshTokenRepository.save(newRefreshToken);

        ResponseCookie accessCookie = cookieUtil.createAccessCookie(user.getUserId(), ACCESS_TOKEN_EXPIRATION_TIME);
        response.addHeader("Set-Cookie", accessCookie.toString());

        logger.info("userId : {}", user.getUserId());
        logger.info("user-email : {}", user.getEmail());
        logger.info("access-token : {}", accessCookie.toString());
        logger.info("refresh-token : {}", refreshCookie.toString());

        String accessToken = extractAccessToken(accessCookie.toString());
        String refreshToken = extractAccessToken(refreshCookie.toString());

        return new OnboardingInfoDTO.DefineUserResponse(onboardingInfo.getNickName(), onboardingInfo.getIntroduction(), onboardingInfo.getTeam(), onboardingInfo.getProfileImg(), accessToken, refreshToken);
    }

    private String extractAccessToken(String cookieString) {
        String[] cookies = cookieString.split(";");
        for (String cookie : cookies) {
            if (cookie.trim().startsWith("access_token=")) {
                return cookie.substring("access_token=".length()).trim();
            }
            else if (cookie.trim().startsWith("refresh_token=")) {
                return cookie.substring("refresh_token=".length()).trim();
            }
        }
        return null;
    }

    public UserDTO.UserResponse getUser(HttpServletRequest request, HttpServletResponse response) {
        User user = validateUser(request, response);

        UserOnboardingInfo userDetails = userOnboardingInfoRepository.findByUser(user);
        return convertToUserDTO(userDetails);
    }

    public UserDTO.UserResponse updateUser(HttpServletRequest request, HttpServletResponse response,
                                           UserDTO.UserRequest userDTO, MultipartFile profileImg) {
        User user = validateUser(request, response);
        String uploadedImageUrl = s3Service.uploadImg(profileImg,DEFAULT_USER_IMAGE);

        UserOnboardingInfo updatedUser = updateUserDTO(user, userDTO, uploadedImageUrl);
        userOnboardingInfoRepository.save(updatedUser);
        return convertToUserDTO(updatedUser);
    }

    private User validateUser(HttpServletRequest request, HttpServletResponse response) {
        UUID userId = tokenServiceImpl.getUserIdFromAccessToken(request, response);
        return userRepository.findByUserId(userId)
                .orElseThrow(() -> new UserException(UserErrorResult.NOT_FOUND_USER));
    }

    private UserDTO.UserResponse convertToUserDTO(UserOnboardingInfo userDetails) {
        return new UserDTO.UserResponse(
                userDetails.getNickName(),
                userDetails.getProfileImg(),
                userDetails.getTeam(),
                userDetails.getIntroduction()

        );
    }

    private UserOnboardingInfo updateUserDTO(User user, UserDTO.UserRequest userDTO, String newProfileImg) {
        UserOnboardingInfo userDetails = userOnboardingInfoRepository.findByUser(user);

        userDetails.setNickName(userDTO.getNickName());
        userDetails.setProfileImg(newProfileImg);
        userDetails.setTeam(userDTO.getTeam());
        userDetails.setIntroduction(userDTO.getIntroduction());

        return userDetails;
    }

    @Transactional
    public void withdraw(HttpServletRequest request, HttpServletResponse response) {

        User user = validateUser(request, response);
        Cookie cookie = cookieUtil.getAccessCookie(request);
        String accessToken = cookie.getValue();

        //Oauth 토큰 처리
        unlinkOAuthAccount(user);

        //사용자 게시물 및 매칭 요청 삭제
        deleteUserPostsAndRequests(user, user.getUserId());

        //JWT 토큰 처리
        handleTokenCleanup(accessToken, user.getUserId());
        handleCookieCleanup(response);

        //사용자 삭제
        userOnboardingInfoRepository.deleteByUser(user);
        userRepository.delete(user);
    }

    private void unlinkOAuthAccount(User user) {
        String provider = user.getProvider();
        String providerId = user.getProviderId();

        oAuth2UnlinkService.unlink(provider, providerId);
        redisService.deleteValues(provider + "-oauth:" + providerId);
    }

    private void deleteUserPostsAndRequests(User user, UUID userId) {

        // post 삭제
        List<MatePost> matePostList = matePostRepository.findAllByUser(user);
        for (MatePost post : matePostList) {
            mateRequestRepository.deleteAllByMatePost(post);
            matePostRepository.delete(post);
        }

        // request 삭제
        mateRequestRepository.deleteAllByApplicant(user);
        // diary 삭제
        diaryRepository.deleteAllByUserId(userId);
    }

    private void handleTokenCleanup(String accessToken, UUID userId) {
        refreshTokenRepository.deleteByUserId(userId); // 리프레쉬 토큰 삭제
        Date expirationDate = jwtUtil.getExpirationDateFromToken(accessToken);
        tokenServiceImpl.addToBlacklist(accessToken, expirationDate); // 액세스 토큰 블랙리스트 등록
    }

    private void handleCookieCleanup(HttpServletResponse response) {
        List<String> cookiesToClear = List.of("access_token", "refresh_token");
        clearCookies(cookiesToClear, response);
    }

    private void clearCookies(List<String> cookieNames, HttpServletResponse response) {
        for (String cookieName : cookieNames) {
            ResponseCookie responseCookie = ResponseCookie.from(cookieName, null)
                    .maxAge(0)
                    .path("/")
                    .httpOnly(true)
                    .secure(true)
                    .build();
            response.addHeader("Set-Cookie", responseCookie.toString());
        }
    }

    public void logout(HttpServletRequest request, HttpServletResponse response) {

        User user = validateUser(request, response);
        Cookie cookie = cookieUtil.getAccessCookie(request);
        String accessToken = cookie.getValue();

        //JWT 토큰 처리
        handleTokenCleanup(accessToken, user.getUserId());
        handleCookieCleanup(response);
    }
}