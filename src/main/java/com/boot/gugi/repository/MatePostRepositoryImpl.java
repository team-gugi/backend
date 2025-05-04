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

import java.time.LocalDate;
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
        BooleanExpression ageCondition = createAgeRangeCondition(qmatePost, matePostOptions.getAge());
        BooleanExpression dateCondition = createDateCondition(qmatePost, matePostOptions.getDate());
        BooleanExpression teamCondition = createTeamCondition(qmatePost, matePostOptions.getTeam());
        BooleanExpression memberCondition = createMemberCondition(qmatePost, matePostOptions.getMember());
        BooleanExpression stadiumCondition = createStadiumCondition(qmatePost, matePostOptions.getStadium());

        NumberExpression<Long> matchScore = createMatchScore(qmatePost, matePostOptions,
                genderCondition, ageCondition, dateCondition, teamCondition, memberCondition, stadiumCondition);

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
                        buildCursorCondition(cursor, maxScore, matchScore, matchCountCursor, updatedAtCursor),
                        matchScore.goe(1)
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

    private BooleanExpression createAgeRangeCondition(QMatePost qmatePost, String age) {
        AgeRangeEnum ageRangeEnum = AgeRangeEnum.fromString(age);
        return ageRangeEnum == AgeRangeEnum.ANY ? Expressions.TRUE : qmatePost.age.eq(ageRangeEnum);
    }

    private BooleanExpression createDateCondition(QMatePost qmatePost, LocalDate date) {
        if (date == null) {
            return Expressions.TRUE;
        }
        return qmatePost.gameDate.eq(date);
    }

    private BooleanExpression createTeamCondition(QMatePost qmatePost, String team) {
        TeamEnum teamEnum = TeamEnum.fromString(team);
        return teamEnum == TeamEnum.ANY ? Expressions.TRUE : qmatePost.homeTeam.eq(teamEnum);
    }

    private BooleanExpression createMemberCondition(QMatePost qmatePost, Integer member) {
        if (member == null) {
            return Expressions.TRUE;
        }
        return qmatePost.member.eq(member);
    }

    private BooleanExpression createStadiumCondition(QMatePost qmatePost, String stadium) {
        StadiumEnum stadiumEnum = StadiumEnum.fromString(stadium);
        return stadiumEnum == StadiumEnum.ANY ? Expressions.TRUE : qmatePost.gameStadium.eq(stadiumEnum);
    }

    private NumberExpression<Long> createMatchScore(QMatePost qmatePost, MateDTO.RequestOption matePostOptions,
                                                    BooleanExpression genderCondition, BooleanExpression ageCondition,
                                                    BooleanExpression dateCondition, BooleanExpression teamCondition,
                                                    BooleanExpression memberCondition, BooleanExpression stadiumCondition) {
        return new CaseBuilder()
                .when(dateCondition).then(1L).otherwise(0L)
                .add(new CaseBuilder()
                        .when(genderCondition).then(1L).otherwise(0L))
                .add(new CaseBuilder()
                        .when(ageCondition).then(1L).otherwise(0L))
                .add(new CaseBuilder()
                        .when(teamCondition).then(1L).otherwise(0L))
                .add(new CaseBuilder()
                        .when(memberCondition).then(1L).otherwise(0L))
                .add(new CaseBuilder()
                        .when(stadiumCondition).then(1L).otherwise(0L));

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