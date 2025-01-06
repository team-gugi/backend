package com.boot.gugi.service;

import com.boot.gugi.base.Enum.*;
import com.boot.gugi.base.dto.MateDTO;
import com.boot.gugi.exception.PostErrorResult;
import com.boot.gugi.exception.PostException;
import com.boot.gugi.exception.UserErrorResult;
import com.boot.gugi.exception.UserException;
import com.boot.gugi.model.MatePost;
import com.boot.gugi.model.MateRequest;
import com.boot.gugi.model.QMatePost;
import com.boot.gugi.model.User;
import com.boot.gugi.repository.MateRequestRepository;
import com.boot.gugi.repository.MatePostRepository;
import com.boot.gugi.repository.UserRepository;
import com.boot.gugi.token.service.TokenServiceImpl;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
    private final JPAQueryFactory queryFactory;

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

        Pageable pageable = PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "updatedAt"));
        List<MatePost> posts;

        if (cursor == null) {
            posts = matePostRepository.findFirst5ByOrderByUpdatedAtDesc(pageable);
        } else {
            posts = matePostRepository.findByUpdatedAtLessThan(cursor, pageable);
        }

        return posts.stream()
                .map(this::convertToLatestDTO)
                .toList();
    }

    public List<MateDTO.ResponseByRelevance> getAllPostsSortedByRelevance(String cursor, MateDTO.RequestOption matePostOptions) {

        long matchCountCursor = cursor != null ? Long.parseLong(cursor.split("_")[0]) : 6;
        LocalDateTime updatedAtCursor = cursor != null ? LocalDateTime.parse(cursor.split("_")[1]) : LocalDateTime.now();

        BooleanBuilder builder = buildConditions(matePostOptions);

        List<MatePost> posts = queryFactory.selectFrom(QMatePost.matePost)
                .where(builder)
                .fetch();

        Map<MatePost, Long> postCountMap = calculateMatchCounts(posts, matePostOptions);
        List<Map.Entry<MatePost, Long>> sortedEntries = sortAndFilterEntries(postCountMap, matchCountCursor, updatedAtCursor);
        return buildPagedResult(sortedEntries);
    }

    private BooleanBuilder buildConditions(MateDTO.RequestOption matePostOptions) {
        BooleanBuilder builder = new BooleanBuilder();
        QMatePost qmatePost = QMatePost.matePost;

        if (matePostOptions.getDate() != null) {
            builder.or(qmatePost.gameDate.eq(matePostOptions.getDate()));
        }
        if (matePostOptions.getGender() != null) {
            builder.or(qmatePost.gender.eq(GenderEnum.fromKorean(matePostOptions.getGender())));
        }
        if (matePostOptions.getAge() != null) {
            builder.or(qmatePost.age.eq(AgeRangeEnum.fromString(matePostOptions.getAge())));
        }
        if (matePostOptions.getTeam() != null) {
            builder.or(qmatePost.homeTeam.eq(TeamEnum.fromString(matePostOptions.getTeam())));
        }
        if (matePostOptions.getStadium() != null) {
            builder.or(qmatePost.gameStadium.eq(StadiumEnum.fromString(matePostOptions.getStadium())));
        }
        if (matePostOptions.getMember() != null) {
            builder.or(qmatePost.member.eq(matePostOptions.getMember()));
        }

        return builder;
    }

    private Map<MatePost, Long> calculateMatchCounts(List<MatePost> posts, MateDTO.RequestOption matePostOptions) {
        return posts.stream()
                .collect(Collectors.toMap(
                        post -> post,
                        post -> {
                            long count = 0;
                            if (matePostOptions.getDate() != null && post.getGameDate().equals(matePostOptions.getDate())) count++;
                            if (matePostOptions.getGender() != null && post.getGender().equals(GenderEnum.fromKorean(matePostOptions.getGender()))) count++;
                            if (matePostOptions.getAge() != null && post.getAge().equals(AgeRangeEnum.fromString(matePostOptions.getAge()))) count++;
                            if (matePostOptions.getTeam() != null && post.getHomeTeam().equals(TeamEnum.fromString(matePostOptions.getTeam()))) count++;
                            if (matePostOptions.getStadium() != null && post.getGameStadium().equals(StadiumEnum.fromString(matePostOptions.getStadium()))) count++;
                            if (matePostOptions.getMember() != null && post.getMember().equals(matePostOptions.getMember())) count++;
                            return count;
                        }
                ));
    }

    private List<Map.Entry<MatePost, Long>> sortAndFilterEntries(Map<MatePost, Long> postCountMap, long matchCountCursor, LocalDateTime updatedAtCursor) {
        return postCountMap.entrySet().stream()
                .filter(entry -> {
                    long matchCount = entry.getValue();
                    LocalDateTime updatedAt = entry.getKey().getUpdatedAt();
                    return (matchCount < matchCountCursor) ||
                            (matchCount == matchCountCursor && updatedAt.isBefore(updatedAtCursor));
                })
                .sorted(Comparator.comparingLong(Map.Entry<MatePost, Long>::getValue).reversed()
                        .thenComparing(entry -> entry.getKey().getUpdatedAt(), Comparator.reverseOrder()))
                .collect(Collectors.toList());
    }

    private List<MateDTO.ResponseByRelevance> buildPagedResult(List<Map.Entry<MatePost, Long>> sortedEntries) {
        List<MateDTO.ResponseByRelevance> result = new ArrayList<>();
        String nextCursor = null;

        for (int i = 0; i < sortedEntries.size(); i++) {
            Map.Entry<MatePost, Long> entry = sortedEntries.get(i);
            MatePost post = entry.getKey();

            long matchCount = entry.getValue();
            LocalDateTime updatedAt = post.getUpdatedAt();
            nextCursor = matchCount + "_" + updatedAt.toString();

            result.add(convertToRelevanceDTO(post, nextCursor));
            if (result.size() == 5) break;
        }

        return result;
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
        MatePost existingMatePost = getMatePostById(mateId);

        if (existingMatePost.getUser().getUserId().equals(applicant.getUserId())) {
            throw new PostException(PostErrorResult.FORBIDDEN_OWN_POST);
        }

        boolean alreadyApplied = mateRequestRepository.existsByApplicantAndMatePost(applicant, existingMatePost);
        if (alreadyApplied) {
            throw new PostException(PostErrorResult.ALREADY_APPLIED);
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