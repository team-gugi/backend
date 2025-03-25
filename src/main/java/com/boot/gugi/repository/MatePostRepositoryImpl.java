package com.boot.gugi.repository;

import com.boot.gugi.base.Enum.AgeRangeEnum;
import com.boot.gugi.base.Enum.GenderEnum;
import com.boot.gugi.base.Enum.StadiumEnum;
import com.boot.gugi.base.Enum.TeamEnum;
import com.boot.gugi.base.dto.MateDTO;
import com.boot.gugi.base.dto.MatePostWithScore;
import com.boot.gugi.model.MatePost;
import com.boot.gugi.model.QMatePost;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class MatePostRepositoryImpl implements MatePostRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<MatePost> findPostsSortedByDate(LocalDateTime cursor, int size) {
        return queryFactory
                .selectFrom(QMatePost.matePost)
                .where(
                        QMatePost.matePost.expired.isFalse(),
                        cursor != null ? QMatePost.matePost.updatedAt.lt(cursor) : null
                )
                .orderBy(QMatePost.matePost.updatedAt.desc())
                .limit(size)
                .fetch();
    }

    @Override
    public List<MatePostWithScore> findPostsSortedByRelevance(String cursor, int size, MateDTO.RequestOption matePostOptions) {
        long matchCountCursor = cursor != null ? Long.parseLong(cursor.split("_")[0]) : Long.MAX_VALUE;
        LocalDateTime updatedAtCursor = cursor != null ? LocalDateTime.parse(cursor.split("_")[1]) : LocalDateTime.now();

        QMatePost qmatePost = QMatePost.matePost;

        BooleanExpression baseCondition = qmatePost.expired.isFalse();
        BooleanExpression genderCondition = createGenderCondition(qmatePost, matePostOptions.getGender());
        BooleanExpression teamCondition = createTeamCondition(qmatePost, matePostOptions.getTeam());

        NumberExpression<Long> matchScore = createMatchScore(qmatePost, matePostOptions, genderCondition, teamCondition);

        Long maxScore = queryFactory
                .select(matchScore.max())
                .from(qmatePost)
                .where(baseCondition)
                .fetchOne();

        List<Tuple> results = queryFactory
                .select(qmatePost, matchScore)
                .from(qmatePost)
                .where(
                        baseCondition,
                        buildCursorCondition(cursor, maxScore, matchScore, matchCountCursor, updatedAtCursor)
                )
                .orderBy(matchScore.desc(), qmatePost.updatedAt.desc())
                .limit(size)
                .fetch();

        return results.stream()
                .map(tuple -> new MatePostWithScore(
                        tuple.get(qmatePost),
                        tuple.get(matchScore)
                ))
                .collect(Collectors.toList());
    }

    private BooleanExpression createGenderCondition(QMatePost qmatePost, String gender) {
        GenderEnum genderEnum = GenderEnum.fromKorean(gender);
        return genderEnum == GenderEnum.ANY ? Expressions.TRUE : qmatePost.gender.eq(genderEnum);
    }

    private BooleanExpression createTeamCondition(QMatePost qmatePost, String team) {
        TeamEnum teamEnum = TeamEnum.fromString(team);
        return teamEnum == TeamEnum.ANY ? Expressions.TRUE : qmatePost.homeTeam.eq(teamEnum);
    }

    private NumberExpression<Long> createMatchScore(QMatePost qmatePost, MateDTO.RequestOption matePostOptions,
                                                    BooleanExpression genderCondition, BooleanExpression teamCondition) {
        return new CaseBuilder()
                .when(qmatePost.gameDate.eq(matePostOptions.getDate())).then(1L).otherwise(0L)
                .add(new CaseBuilder()
                        .when(genderCondition).then(1L).otherwise(0L))
                .add(new CaseBuilder()
                        .when(qmatePost.age.eq(AgeRangeEnum.fromString(matePostOptions.getAge()))).then(1L).otherwise(0L))
                .add(new CaseBuilder()
                        .when(teamCondition).then(1L).otherwise(0L))
                .add(new CaseBuilder()
                        .when(qmatePost.gameStadium.eq(StadiumEnum.fromString(matePostOptions.getStadium()))).then(1L).otherwise(0L))
                .add(new CaseBuilder()
                        .when(qmatePost.member.eq(matePostOptions.getMember())).then(1L).otherwise(0L));
    }

    private BooleanExpression buildCursorCondition(String cursor, Long maxScore, NumberExpression<Long> matchScore, long matchCountCursor, LocalDateTime updatedAtCursor) {
        if (cursor == null) {
            return matchScore.loe(maxScore);
        }
        return matchScore.loe(matchCountCursor)
                .and(
                        matchScore.eq(matchCountCursor)
                                .and(QMatePost.matePost.updatedAt.lt(updatedAtCursor))
                                .or(matchScore.lt(matchCountCursor))
                );
    }

}