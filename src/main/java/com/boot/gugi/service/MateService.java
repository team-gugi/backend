package com.boot.gugi.service;

import com.boot.gugi.base.Enum.AgeRangeEnum;
import com.boot.gugi.base.Enum.GenderEnum;
import com.boot.gugi.base.Enum.StadiumEnum;
import com.boot.gugi.base.Enum.TeamEnum;
import com.boot.gugi.base.dto.MateDTO;
import com.boot.gugi.exception.PostErrorResult;
import com.boot.gugi.exception.PostException;
import com.boot.gugi.exception.UserErrorResult;
import com.boot.gugi.exception.UserException;
import com.boot.gugi.model.MatePost;
import com.boot.gugi.model.User;
import com.boot.gugi.repository.MatePostRepository;
import com.boot.gugi.repository.UserRepository;
import com.boot.gugi.token.service.TokenServiceImpl;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MateService {

    private final TokenServiceImpl tokenServiceImpl;
    private final UserRepository userRepository;
    private final MatePostRepository matePostRepository;

    @Transactional
    public void createMatePost(HttpServletRequest request, HttpServletResponse response, MateDTO.MateRequest matePostDetails) {
        UUID userId = tokenServiceImpl.getUserIdFromAccessToken(request, response);
        User writer = userRepository.findByUserId(userId)
                .orElseThrow(() -> new UserException(UserErrorResult.NOT_FOUND_USER));

        MatePost savedMate = createMateInfo(writer, matePostDetails);
        matePostRepository.save(savedMate);
    }

    @Transactional
    public void updateMatePost(HttpServletRequest request, HttpServletResponse response, UUID mateId, MateDTO.MateRequest matePostDetails) {
        UUID userId = tokenServiceImpl.getUserIdFromAccessToken(request, response);
        User writer = userRepository.findByUserId(userId)
                .orElseThrow(() -> new UserException(UserErrorResult.NOT_FOUND_USER));

        MatePost existingMatePost = matePostRepository.findById(mateId)
                .orElseThrow(() -> new PostException(PostErrorResult.NOT_FOUND_MATE_POST));

        if (!existingMatePost.getUser().getUserId().equals(writer.getUserId())) {
            throw new PostException(PostErrorResult.UNAUTHORIZED_ACCESS);
        }

        updateMatePostInfo(existingMatePost, matePostDetails);
        matePostRepository.save(existingMatePost);
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

    public List<MateDTO.MateResponse> getAllPostsSortedByDate(LocalDateTime cursor) {

        Pageable pageable = PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "updatedAt"));
        List<MatePost> posts;

        if (cursor == null) {
            posts = matePostRepository.findFirst5ByOrderByUpdatedAtDesc(pageable);
        } else {
            posts = matePostRepository.findByUpdatedAtLessThan(cursor, pageable);
        }

        return posts.stream()
                .map(this::convertToDto)
                .toList();
    }

    private MateDTO.MateResponse convertToDto(MatePost post) {
        return new MateDTO.MateResponse(
                post.getMateId(),
                post.getTitle(),
                post.getContent(),
                post.getDaysSinceWritten(),
                post.getDaysUntilGame(),
                post.getConfirmedMembers(),
                post.getUpdatedAt(),
                new MateDTO.MateOption(
                        post.getGender().toKorean(),
                        post.getAge().toKorean(),
                        post.getGameDate(),
                        post.getHomeTeam().toKorean(),
                        post.getMember(),
                        post.getGameStadium().toKorean()
                )
        );
    }
}