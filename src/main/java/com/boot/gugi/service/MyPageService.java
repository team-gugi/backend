package com.boot.gugi.service;

import com.boot.gugi.base.Enum.ApplicationStatusEnum;
import com.boot.gugi.exception.PostErrorResult;
import com.boot.gugi.exception.PostException;
import com.boot.gugi.exception.UserErrorResult;
import com.boot.gugi.exception.UserException;
import com.boot.gugi.model.MatePost;
import com.boot.gugi.model.MateRequest;
import com.boot.gugi.model.User;
import com.boot.gugi.repository.MateRequestRepository;
import com.boot.gugi.repository.UserRepository;
import com.boot.gugi.token.service.TokenServiceImpl;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MyPageService {

    private final TokenServiceImpl tokenServiceImpl;
    private final UserRepository userRepository;
    private final MateRequestRepository mateRequestRepository;

    @Transactional
    public void respondToMateRequest(HttpServletRequest request, HttpServletResponse response, UUID requestId, String status) {
        UUID userId = tokenServiceImpl.getUserIdFromAccessToken(request, response);
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new UserException(UserErrorResult.NOT_FOUND_USER));

        MateRequest mateRequest = mateRequestRepository.findById(requestId)
                .orElseThrow(() -> new PostException(PostErrorResult.NOT_FOUND_REQUEST));

        if (!mateRequest.getMatePost().getUser().getUserId().equals(userId)) {
            throw new PostException(PostErrorResult.UNAUTHORIZED_ACCESS);
        }
        if (mateRequest.getStatus() != ApplicationStatusEnum.PENDING) {
            throw new PostException(PostErrorResult.ALREADY_RESPONDED);
        }

        ApplicationStatusEnum newStatus = ApplicationStatusEnum.fromKorean(status);
        if (newStatus == ApplicationStatusEnum.ACCEPTED) {
            MatePost matePost = mateRequest.getMatePost();
            Integer confirmedMembers = matePost.getConfirmedMembers();
            Integer totalMembers = matePost.getMember();

            if (confirmedMembers >= totalMembers) {
                throw new PostException(PostErrorResult.MAX_MEMBERS_REACHED);
            }
            matePost.setConfirmedMembers(confirmedMembers + 1);
        }

        mateRequest.setStatus(ApplicationStatusEnum.fromKorean(status));
        mateRequestRepository.save(mateRequest);
    }
}