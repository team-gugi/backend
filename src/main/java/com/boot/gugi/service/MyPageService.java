package com.boot.gugi.service;

import com.boot.gugi.base.Enum.AgeRangeEnum;
import com.boot.gugi.base.Enum.ApplicationStatusEnum;
import com.boot.gugi.base.Enum.SexEnum;
import com.boot.gugi.base.dto.MateDTO;
import com.boot.gugi.base.dto.MatePostStatusDTO;
import com.boot.gugi.exception.PostErrorResult;
import com.boot.gugi.exception.PostException;
import com.boot.gugi.exception.UserErrorResult;
import com.boot.gugi.exception.UserException;
import com.boot.gugi.model.MatePost;
import com.boot.gugi.model.MateRequest;
import com.boot.gugi.model.User;
import com.boot.gugi.model.UserOnboardingInfo;
import com.boot.gugi.repository.MatePostRepository;
import com.boot.gugi.repository.MateRequestRepository;
import com.boot.gugi.repository.UserOnboardingInfoRepository;
import com.boot.gugi.repository.UserRepository;
import com.boot.gugi.token.service.TokenServiceImpl;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MyPageService {

    private final TokenServiceImpl tokenServiceImpl;
    private final UserRepository userRepository;
    private final UserOnboardingInfoRepository userOnboardingInfoRepository;
    private final MateRequestRepository mateRequestRepository;
    private final MatePostRepository matePostRepository;

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

    @Transactional(readOnly = true)
    public MatePostStatusDTO.MateRequestSummaryDTO getMateRequestSummary(HttpServletRequest request, HttpServletResponse response) {
        UUID userId = tokenServiceImpl.getUserIdFromAccessToken(request, response);
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new UserException(UserErrorResult.NOT_FOUND_USER));

        List<MatePostStatusDTO.AppliedRequestNotificationDTO> notificationList = new ArrayList<>();
        List<MatePostStatusDTO.RequestedPostStatusDTO> pendingList = new ArrayList<>();
        List<MatePostStatusDTO.AcceptedPostStatusDTO> acceptedList = new ArrayList<>();
        List<MatePostStatusDTO.RequestedPostStatusDTO> rejectedList = new ArrayList<>();

        List<MateRequest> mateRequests = mateRequestRepository.findAllByApplicantAndNotExpired(user);
        processMateRequests(mateRequests, pendingList, acceptedList, rejectedList);

        List<MatePost> matePosts = matePostRepository.findAllByUserAndNotExpired(user);
        processMatePosts(matePosts, pendingList, acceptedList, notificationList);

        return buildSummaryDTO(notificationList, acceptedList, pendingList, rejectedList);
    }

    private void processMateRequests(List<MateRequest> mateRequests,
                                     List<MatePostStatusDTO.RequestedPostStatusDTO> pendingList,
                                     List<MatePostStatusDTO.AcceptedPostStatusDTO> acceptedList,
                                     List<MatePostStatusDTO.RequestedPostStatusDTO> rejectedList) {
        for (MateRequest mateRequest : mateRequests) {
            MatePost matePost = mateRequest.getMatePost();

            switch (mateRequest.getStatus()) {
                case PENDING -> pendingList.add(convertToMatePostStatusDTO(matePost, false));
                case ACCEPTED -> acceptedList.add(convertToAcceptedPostStatusDTO(matePost, false));
                case REJECTED -> rejectedList.add(convertToMatePostStatusDTO(matePost, false));
            }
        }
    }

    private void processMatePosts(List<MatePost> matePosts,
                                  List<MatePostStatusDTO.RequestedPostStatusDTO> pendingList,
                                  List<MatePostStatusDTO.AcceptedPostStatusDTO> acceptedList,
                                  List<MatePostStatusDTO.AppliedRequestNotificationDTO> notificationList) {
        for (MatePost matePost : matePosts) {
            long confirmedMemberCount = matePost.getConfirmedMembers();

            if (confirmedMemberCount == 1) {
                pendingList.add(convertToMatePostStatusDTO(matePost, true));
            } else if (confirmedMemberCount >= 2) {
                acceptedList.add(convertToAcceptedPostStatusDTO(matePost, true));
            }

            List<MatePostStatusDTO.AppliedRequestNotificationDTO> pendingNotifications = matePost.getMateRequestList().stream()
                    .filter(mateRequest -> mateRequest.getStatus() == ApplicationStatusEnum.PENDING)
                    .map(this::convertToNotificationDTO)
                    .collect(Collectors.toList());

            notificationList.addAll(pendingNotifications);
        }
    }

    private MatePostStatusDTO.MateRequestSummaryDTO buildSummaryDTO(
            List<MatePostStatusDTO.AppliedRequestNotificationDTO> notificationList,
            List<MatePostStatusDTO.AcceptedPostStatusDTO> acceptedList,
            List<MatePostStatusDTO.RequestedPostStatusDTO> pendingList,
            List<MatePostStatusDTO.RequestedPostStatusDTO> rejectedList) {

        MatePostStatusDTO.MateRequestSummaryDTO summaryDTO = new MatePostStatusDTO.MateRequestSummaryDTO();
        summaryDTO.setNotification(notificationList);
        summaryDTO.setPending(pendingList);
        summaryDTO.setAccepted(acceptedList);
        summaryDTO.setRejected(rejectedList);

        return summaryDTO;
    }

    private MatePostStatusDTO.AppliedRequestNotificationDTO convertToNotificationDTO(MateRequest mateRequest) {
        MatePost matePost = mateRequest.getMatePost();
        User applicant = mateRequest.getApplicant();
        UserOnboardingInfo applicantInfo = userOnboardingInfoRepository.findByUser(applicant);

        AgeRangeEnum age = applicantInfo.getAge();
        SexEnum sex = applicantInfo.getSex();

        return new MatePostStatusDTO.AppliedRequestNotificationDTO(
                mateRequest.getRequestId(),
                matePost.getTitle(),
                applicantInfo.getNickName(),
                new MatePostStatusDTO.ApplicantInfo(
                        age.toKorean(),
                        sex.toKorean(),
                        applicantInfo.getTeam(),
                        applicantInfo.getIntroduction(),
                        applicantInfo.getProfileImg()
                )
        );
    }

    private MatePostStatusDTO.RequestedPostStatusDTO convertToMatePostStatusDTO(MatePost matePost, Boolean isOwner) {
        LocalDate gameDate = matePost.getGameDate();
        String formattedGameDate = String.format("%02d-%02d", gameDate.getMonthValue(), gameDate.getDayOfMonth());

        String homeTeam = matePost.getHomeTeam().toKorean();
        String firstWordOfHomeTeam = homeTeam.split(" ")[0];

        return new MatePostStatusDTO.RequestedPostStatusDTO(
                isOwner,
                matePost.getMateId(),
                matePost.getTitle(),
                matePost.getContent(),
                matePost.getDaysSinceWritten(),
                matePost.getDaysUntilGame(),
                matePost.getConfirmedMembers(),
                new MateDTO.ResponseOption(
                        matePost.getGender().toKorean(),
                        matePost.getAge().toKorean(),
                        formattedGameDate,
                        firstWordOfHomeTeam,
                        matePost.getMember(),
                        matePost.getGameStadium().toKorean()
                )
        );
    }

    private MatePostStatusDTO.AcceptedPostStatusDTO convertToAcceptedPostStatusDTO(MatePost matePost, Boolean isOwner) {
        LocalDate gameDate = matePost.getGameDate();
        String formattedGameDate = String.format("%02d-%02d", gameDate.getMonthValue(), gameDate.getDayOfMonth());

        String homeTeam = matePost.getHomeTeam().toKorean();
        String firstWordOfHomeTeam = homeTeam.split(" ")[0];

        return new MatePostStatusDTO.AcceptedPostStatusDTO(
                isOwner,
                matePost.getMateId(),
                matePost.getTitle(),
                matePost.getContent(),
                matePost.getDaysSinceWritten(),
                matePost.getDaysUntilGame(),
                matePost.getConfirmedMembers(),
                matePost.getContact(),
                new MateDTO.ResponseOption(
                        matePost.getGender().toKorean(),
                        matePost.getAge().toKorean(),
                        formattedGameDate,
                        firstWordOfHomeTeam,
                        matePost.getMember(),
                        matePost.getGameStadium().toKorean()
                )
        );
    }
}