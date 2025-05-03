package com.boot.gugi.service;

import com.boot.gugi.base.Enum.*;
import com.boot.gugi.base.dto.DiaryDTO;
import com.boot.gugi.exception.PostErrorResult;
import com.boot.gugi.exception.PostException;
import com.boot.gugi.exception.UserErrorResult;
import com.boot.gugi.exception.UserException;
import com.boot.gugi.model.Diary;
import com.boot.gugi.model.User;
import com.boot.gugi.repository.DiaryRepository;
import com.boot.gugi.repository.UserOnboardingInfoRepository;
import com.boot.gugi.repository.UserRepository;
import com.boot.gugi.token.service.TokenServiceImpl;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DiaryService {

    private final S3Service s3Service;
    private final DiaryRepository diaryRepository;
    private final UserRepository userRepository;
    private final UserOnboardingInfoRepository userOnboardingInfoRepository;
    private final TokenServiceImpl tokenServiceImpl;

    @Transactional
    public void createDiaryPost(HttpServletRequest request, HttpServletResponse response, DiaryDTO.DiaryRequest postInfo, MultipartFile gameImg) {

        UUID userId = tokenServiceImpl.getUserIdFromAccessToken(request, response);
        String uploadedDiaryUrl = s3Service.uploadImg(gameImg, null);
        GameResultEnum gameResult = determineGameResult(postInfo.getHomeScore(), postInfo.getAwayScore());

        updateUserStatistics(userId, gameResult, null, true);
        Diary savedDiary = createDiaryInfo(userId, postInfo, uploadedDiaryUrl, gameResult);
        diaryRepository.save(savedDiary);
    }

    @Transactional
    public void updateDiaryPost(HttpServletRequest request, HttpServletResponse response, UUID diaryId, DiaryDTO.DiaryRequest postInfo, MultipartFile gameImg) {

        UUID userId = tokenServiceImpl.getUserIdFromAccessToken(request, response);
        Diary existingDiary = diaryRepository.findByDiaryId(diaryId)
                .orElseThrow(() -> new PostException(PostErrorResult.NOT_FOUND_DIARY));

        if (!existingDiary.getUserId().equals(userId)) {
            throw new PostException(PostErrorResult.UNAUTHORIZED_ACCESS);
        }

        String uploadedDiaryUrl = (gameImg != null && !gameImg.isEmpty())
                ? s3Service.uploadImg(gameImg, null)
                : existingDiary.getGameImg();
        GameResultEnum gameResult = determineGameResult(postInfo.getHomeScore(), postInfo.getAwayScore());

        updateUserStatistics(userId, gameResult, existingDiary.getGameResult(), false);
        updateDiaryInfo(existingDiary, postInfo, uploadedDiaryUrl, gameResult);
        diaryRepository.save(existingDiary);
    }

    public DiaryDTO.DiaryDetailDto getDiaryDetails(HttpServletRequest request, HttpServletResponse response, UUID diaryId){
        UUID userId = tokenServiceImpl.getUserIdFromAccessToken(request, response);
        Diary existingDiary = diaryRepository.findByDiaryId(diaryId)
                .orElseThrow(() -> new PostException(PostErrorResult.NOT_FOUND_DIARY));

        if (!existingDiary.getUserId().equals(userId)) {
            throw new PostException(PostErrorResult.UNAUTHORIZED_ACCESS);
        }

        return convertToDiaryDetailDto(existingDiary);
    }

    public List<DiaryDTO.DiarySingleDto> getAllDiary(HttpServletRequest request, HttpServletResponse response){
        UUID userId = tokenServiceImpl.getUserIdFromAccessToken(request, response);
        List<Diary> diaries = diaryRepository.findByUserId(userId);

        return diaries.stream()
                .sorted(Comparator.comparing(Diary::getGameDate).reversed()
                        .thenComparing(Comparator.comparing(Diary::getCreatedAt).reversed()))
                .map(this::convertToDiarySingleDto)
                .collect(Collectors.toList());
    }

    public DiaryDTO.WinRateResponse getMyWinRate(HttpServletRequest request, HttpServletResponse response) {
        UUID userId = tokenServiceImpl.getUserIdFromAccessToken(request, response);

        User existingUser = userRepository.findByUserId(userId)
                .orElseThrow(() -> new UserException(UserErrorResult.NOT_FOUND_USER));
        String nickName = userOnboardingInfoRepository.findNickNameByUser(existingUser);

        return convertToUserWinRateDto(existingUser, nickName);
    }

    private GameResultEnum determineGameResult(Integer homeScore, Integer awayScore) {
        if (homeScore > awayScore) {
            return GameResultEnum.WIN;
        } else if (homeScore < awayScore) {
            return GameResultEnum.LOSE;
        } else {
            return GameResultEnum.DRAW;
        }
    }

    private void updateUserStatistics(UUID userId, GameResultEnum newGameResult, GameResultEnum oldGameResult, boolean isCreate) {
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new UserException(UserErrorResult.NOT_FOUND_USER));

        if (isCreate) {
            updateStatisticsForCreation(user, newGameResult);
        } else {
            updateStatisticsForUpdate(user, newGameResult, oldGameResult);
        }

        user.setWinRate(calculateWinningPercent(user.getTotalWins(), user.getTotalLoses()));
        userRepository.save(user);
    }

    private void updateStatisticsForCreation(User user, GameResultEnum newGameResult) {
        user.setTotalDiaryCount(user.getTotalDiaryCount() + 1);
        if (newGameResult == GameResultEnum.WIN) {
            user.setTotalWins(user.getTotalWins() + 1);
        } else if (newGameResult == GameResultEnum.LOSE) {
            user.setTotalLoses(user.getTotalLoses() + 1);
        } else {
            user.setTotalDraws(user.getTotalDraws() + 1);
        }
    }

    private void updateStatisticsForUpdate(User user, GameResultEnum newGameResult, GameResultEnum oldGameResult) {
        if (oldGameResult == GameResultEnum.WIN) {
            user.setTotalWins(user.getTotalWins() - 1);
        } else if (oldGameResult == GameResultEnum.LOSE) {
            user.setTotalLoses(user.getTotalLoses() - 1);
        } else if (oldGameResult == GameResultEnum.DRAW) {
            user.setTotalDraws(user.getTotalDraws() - 1);
        }

        if (newGameResult == GameResultEnum.WIN) {
            user.setTotalWins(user.getTotalWins() + 1);
        } else if (newGameResult == GameResultEnum.LOSE) {
            user.setTotalLoses(user.getTotalLoses() + 1);
        } else if (newGameResult == GameResultEnum.DRAW) {
            user.setTotalDraws(user.getTotalDraws() + 1);
        }
    }

    private BigDecimal calculateWinningPercent(Integer totalWins, Integer totalLoses) {
        Integer totalGames = totalWins + totalLoses;
        if (totalGames > 0) {
            BigDecimal winningRate = BigDecimal.valueOf(totalWins)
                    .divide(BigDecimal.valueOf(totalGames), 4, RoundingMode.HALF_UP);

            return winningRate.multiply(BigDecimal.valueOf(100));
        } else {
            return BigDecimal.ZERO;
        }
    }

    private Diary createDiaryInfo(UUID userId, DiaryDTO.DiaryRequest postInfo, String diaryImg, GameResultEnum gameResult) {
        StadiumEnum stadium = StadiumEnum.fromString(postInfo.getGameStadium());
        TeamEnum homeTeam = TeamEnum.fromString(postInfo.getHomeTeam());
        TeamEnum awayTeam = TeamEnum.fromString(postInfo.getAwayTeam());
        return Diary.builder()
                .userId(userId)
                .gameDate(postInfo.getGameDate())
                .gameStadium(stadium)
                .homeTeam(homeTeam)
                .awayTeam(awayTeam)
                .homeScore(postInfo.getHomeScore())
                .awayScore(postInfo.getAwayScore())
                .gameResult(gameResult)
                .gameImg(diaryImg)
                .content(postInfo.getContent())
                .createdAt(LocalDateTime.now())
                .build();
    }

    private void updateDiaryInfo(Diary existingDiary, DiaryDTO.DiaryRequest postInfo, String diaryImg, GameResultEnum gameResult) {
        StadiumEnum stadium = StadiumEnum.fromString(postInfo.getGameStadium());
        TeamEnum homeTeam = TeamEnum.fromString(postInfo.getHomeTeam());
        TeamEnum awayTeam = TeamEnum.fromString(postInfo.getAwayTeam());

        existingDiary.setGameDate(postInfo.getGameDate());
        existingDiary.setGameStadium(stadium);
        existingDiary.setHomeTeam(homeTeam);
        existingDiary.setAwayTeam(awayTeam);
        existingDiary.setHomeScore(postInfo.getHomeScore());
        existingDiary.setAwayScore(postInfo.getAwayScore());
        existingDiary.setGameResult(gameResult);
        existingDiary.setGameImg(diaryImg);
        existingDiary.setContent(postInfo.getContent());
        existingDiary.setUpdatedAt(LocalDateTime.now());
    }

    private DiaryDTO.DiaryDetailDto convertToDiaryDetailDto(Diary diary) {
        return new DiaryDTO.DiaryDetailDto(
                diary.getDiaryId(),
                diary.getGameDate(),
                diary.getGameStadium().toKorean(),
                diary.getHomeTeam().toKorean(),
                diary.getAwayTeam().toKorean(),
                diary.getHomeScore(),
                diary.getAwayScore(),
                diary.getGameImg(),
                diary.getContent(),
                diary.getGameResult().toEnglish()
        );
    }

    private DiaryDTO.DiarySingleDto convertToDiarySingleDto(Diary diary) {

        String homeTeam = diary.getHomeTeam().toKorean();
        String awayTeam = diary.getAwayTeam().toKorean();

        String firstWordOfHomeTeam = homeTeam.split(" ")[0];
        String firstWordOfAwayTeam = awayTeam.split(" ")[0];

        return new DiaryDTO.DiarySingleDto(
                diary.getDiaryId(),
                diary.getGameDate(),
                diary.getGameStadium().toKorean(),
                firstWordOfHomeTeam,
                firstWordOfAwayTeam,
                diary.getGameResult().toEnglish(),
                diary.getGameImg()
        );
    }

    private DiaryDTO.WinRateResponse convertToUserWinRateDto(User user, String nickName) {
        return new DiaryDTO.WinRateResponse(
                nickName,
                user.getWinRate(),
                user.getTotalLoses(),
                user.getTotalDraws(),
                user.getTotalDiaryCount(),
                user.getTotalWins()
        );
    }
}