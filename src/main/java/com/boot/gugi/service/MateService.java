package com.boot.gugi.service;

import com.boot.gugi.base.Enum.*;
import com.boot.gugi.base.dto.MateDTO;
import com.boot.gugi.base.dto.MatePostWithScore;
import com.boot.gugi.exception.PostErrorResult;
import com.boot.gugi.exception.PostException;
import com.boot.gugi.exception.UserErrorResult;
import com.boot.gugi.exception.UserException;
import com.boot.gugi.model.MatePost;
import com.boot.gugi.model.MateRequest;
import com.boot.gugi.model.User;
import com.boot.gugi.model.UserOnboardingInfo;
import com.boot.gugi.repository.MateRequestRepository;
import com.boot.gugi.repository.MatePostRepository;
import com.boot.gugi.repository.UserRepository;
import com.boot.gugi.token.service.TokenServiceImpl;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.time.LocalDateTime;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MateService {

    private final TokenServiceImpl tokenServiceImpl;
    private final UserRepository userRepository;
    private final MatePostRepository matePostRepository;
    private final MateRequestRepository mateRequestRepository;

    private static final Logger logger = LoggerFactory.getLogger(MateService.class);

    @Transactional
    public void createMatePost(HttpServletRequest request, HttpServletResponse response, MateDTO.MateRequest matePostDetails) {
        User writer = validateUser(request, response);

        MatePost savedMate = createMateInfo(writer, matePostDetails);
        matePostRepository.save(savedMate);
    }

    @Transactional
    public void updateMatePost(HttpServletRequest request, HttpServletResponse response, UUID mateId, MateDTO.MateRequest matePostDetails) {
        User writer = validateUser(request, response);
        MatePost existingMatePost = getMatePostById(mateId);

        if (!existingMatePost.getUser().getUserId().equals(writer.getUserId())) {
            throw new PostException(PostErrorResult.UNAUTHORIZED_ACCESS);
        }

        updateMatePostInfo(existingMatePost, matePostDetails);
        matePostRepository.save(existingMatePost);
    }

    private User validateUser(HttpServletRequest request, HttpServletResponse response) {
        UUID userId = tokenServiceImpl.getUserIdFromAccessToken(request, response);
        return userRepository.findByUserId(userId)
                .orElseThrow(() -> new UserException(UserErrorResult.NOT_FOUND_USER));
    }

    private MatePost getMatePostById(UUID postId) {
        return matePostRepository.findById(postId)
                .orElseThrow(() -> new PostException(PostErrorResult.NOT_FOUND_MATE_POST));
    }

    private MatePost createMateInfo(User user, MateDTO.MateRequest matePostDetails) {
        GenderEnum gender = GenderEnum.fromKorean(matePostDetails.getOptions().getGender());
        AgeRangeEnum age = AgeRangeEnum.fromString(matePostDetails.getOptions().getAge());
        TeamEnum team = TeamEnum.fromString(matePostDetails.getOptions().getTeam());
        StadiumEnum stadium = StadiumEnum.fromString(matePostDetails.getOptions().getStadium());
        return MatePost.builder()
                .user(user)
                .title(matePostDetails.getTitle())
                .content(matePostDetails.getContent())
                .contact(matePostDetails.getContact())
                .gender(gender)
                .age(age)
                .gameDate(matePostDetails.getOptions().getDate())
                .homeTeam(team)
                .member(matePostDetails.getOptions().getMember())
                .gameStadium(stadium)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    private void updateMatePostInfo(MatePost existingMatePost, MateDTO.MateRequest matePostDetails) {
        GenderEnum gender = GenderEnum.fromKorean(matePostDetails.getOptions().getGender());
        AgeRangeEnum age = AgeRangeEnum.fromString(matePostDetails.getOptions().getAge());
        TeamEnum team = TeamEnum.fromString(matePostDetails.getOptions().getTeam());
        StadiumEnum stadium = StadiumEnum.fromString(matePostDetails.getOptions().getStadium());

        existingMatePost.setTitle(matePostDetails.getTitle());
        existingMatePost.setContent(matePostDetails.getContent());
        existingMatePost.setContact(matePostDetails.getContact());
        existingMatePost.setGender(gender);
        existingMatePost.setAge(age);
        existingMatePost.setGameDate(matePostDetails.getOptions().getDate());
        existingMatePost.setHomeTeam(team);
        existingMatePost.setMember(matePostDetails.getOptions().getMember());
        existingMatePost.setGameStadium(stadium);
        existingMatePost.setUpdatedAt(LocalDateTime.now());
    }

    public List<MateDTO.ResponseByDate> getAllPostsSortedByDate(LocalDateTime cursor) {

        List<MatePost> posts = matePostRepository.findPostsSortedByDate(cursor, 5);
        return posts.stream()
                .map(this::convertToLatestDTO)
                .toList();
    }

    public List<MateDTO.ResponseByRelevance> getAllPostsSortedByRelevance(String cursor, MateDTO.RequestOption matePostOptions) {

        List<MatePostWithScore> postsWithScores = matePostRepository.findPostsSortedByRelevance(cursor, 5, matePostOptions);

        if (postsWithScores.isEmpty()) {
            return Collections.emptyList();
        }

        return postsWithScores.stream()
                .map(entry -> convertToRelevanceDTO(entry.getMatePost(), generateNextCursor(entry)))
                .collect(Collectors.toList());
    }

    private String generateNextCursor(MatePostWithScore postWithScores) {
        return postWithScores.getMatchScore() + "_" + postWithScores.getMatePost().getUpdatedAt().toString();
    }

    private MateDTO.ResponseByDate convertToLatestDTO(MatePost post) {

        LocalDate gameDate = post.getGameDate();
        String formattedGameDate = String.format("%02d-%02d", gameDate.getMonthValue(), gameDate.getDayOfMonth());

        String homeTeam = post.getHomeTeam().toKorean();
        String firstWordOfHomeTeam = homeTeam.split(" ")[0];

        return new MateDTO.ResponseByDate(
                post.getMateId(),
                post.getTitle(),
                post.getContent(),
                post.getDaysSinceWritten(),
                post.getDaysUntilGame(),
                post.getConfirmedMembers(),
                post.getUpdatedAt(),
                new MateDTO.ResponseOption(
                        post.getGender().toKorean(),
                        post.getAge().toKorean(),
                        formattedGameDate,
                        firstWordOfHomeTeam,
                        post.getMember(),
                        post.getGameStadium().toKorean()
                )
        );
    }

    private MateDTO.ResponseByRelevance convertToRelevanceDTO(MatePost post, String nextCursor) {

        LocalDate gameDate = post.getGameDate();
        String formattedGameDate = String.format("%02d-%02d", gameDate.getMonthValue(), gameDate.getDayOfMonth());

        String homeTeam = post.getHomeTeam().toKorean();
        String firstWordOfHomeTeam = homeTeam.split(" ")[0];

        return new MateDTO.ResponseByRelevance(
                post.getMateId(),
                post.getTitle(),
                post.getContent(),
                post.getDaysSinceWritten(),
                post.getDaysUntilGame(),
                post.getConfirmedMembers(),
                nextCursor,
                new MateDTO.ResponseOption(
                        post.getGender().toKorean(),
                        post.getAge().toKorean(),
                        formattedGameDate,
                        firstWordOfHomeTeam,
                        post.getMember(),
                        post.getGameStadium().toKorean()
                )
        );
    }

    @Transactional
    public void applyToMatePost(HttpServletRequest request, HttpServletResponse response, UUID mateId){
        User applicant = validateUser(request, response);
        //UUID userId = applicant.getUserId();
        UserOnboardingInfo userInfo = applicant.getOnboardingInfo();
        MatePost existingMatePost = getMatePostById(mateId);

        if (existingMatePost.getUser().getUserId().equals(applicant.getUserId())) {
            throw new PostException(PostErrorResult.FORBIDDEN_OWN_POST);
        }

        boolean alreadyApplied = mateRequestRepository.existsByApplicantAndMatePost(applicant, existingMatePost);
        if (alreadyApplied) {
            throw new PostException(PostErrorResult.ALREADY_APPLIED);
        }

        if (existingMatePost.getConfirmedMembers().equals(existingMatePost.getMember())) {
            throw new PostException(PostErrorResult.RECRUITMENT_COMPLETED);
        }

        if (userInfo.getSex() == null) {
            throw new PostException(PostErrorResult.GENDER_REQUIRED);
        }
        if (userInfo.getAge() == null) {
            throw new PostException(PostErrorResult.AGE_REQUIRED);
        }

        GenderEnum genderOption = existingMatePost.getGender();
        if (genderOption != null) {
            if (genderOption.equals(GenderEnum.FEMALE_ONLY) && userInfo.getSex() != SexEnum.FEMALE) {
                throw new PostException(PostErrorResult.GENDER_MISMATCH);
            }
            else if (genderOption.equals(GenderEnum.MALE_ONLY) && userInfo.getSex() != SexEnum.MALE) {
                throw new PostException(PostErrorResult.GENDER_MISMATCH);
            }
        }
        AgeRangeEnum ageOption = existingMatePost.getAge();
        if (ageOption != null) {
            if (!userInfo.getAge().equals(ageOption)) {
                throw new PostException(PostErrorResult.AGE_MISMATCH);
            }
        }

        MateRequest savedRequest = registerRequest(applicant, existingMatePost);
        mateRequestRepository.save(savedRequest);
    }

    private MateRequest registerRequest(User applicant, MatePost matePost) {
        return MateRequest.builder()
                .applicant(applicant)
                .matePost(matePost)
                .status(ApplicationStatusEnum.PENDING)
                .appliedAt(LocalDateTime.now())
                .build();
    }
}