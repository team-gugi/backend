package com.boot.gugi.service;

import com.boot.gugi.base.Enum.AgeRangeEnum;
import com.boot.gugi.base.Enum.GenderEnum;
import com.boot.gugi.base.Enum.StadiumEnum;
import com.boot.gugi.base.Enum.TeamEnum;
import com.boot.gugi.base.dto.MateDTO;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
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
}