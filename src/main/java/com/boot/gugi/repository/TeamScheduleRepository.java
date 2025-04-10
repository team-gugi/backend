package com.boot.gugi.repository;

import com.boot.gugi.model.TeamSchedule;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface TeamScheduleRepository extends JpaRepository<TeamSchedule, Long> {

    Optional<TeamSchedule> findByScheduleKey(String scheduleKey);

    @Query("SELECT s FROM TeamSchedule s WHERE s.scheduleKey NOT IN :keys")
    List<TeamSchedule> findAllExceptKeys(@Param("keys") Set<String> keys);

    @Query("SELECT t FROM TeamSchedule t WHERE t.date = :date AND (t.homeTeam = :team OR t.awayTeam = :team)")
    List<TeamSchedule> findByDateAndTeam(@Param("date") String date, @Param("team") String team);
}
