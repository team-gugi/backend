package com.boot.gugi.repository;

import com.boot.gugi.model.TeamSchedule;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface TeamScheduleRepository extends JpaRepository<TeamSchedule, Long> {

    Optional<TeamSchedule> findByDateAndSpecificDateAndHomeTeamAndAwayTeam(
            String date,
            String specificDate,
            String homeTeam,
            String awayTeam
    );

    @Query("SELECT t FROM TeamSchedule t WHERE t.date = :date AND (t.homeTeam = :team OR t.awayTeam = :team)")
    List<TeamSchedule> findByDateAndTeam(@Param("date") String date, @Param("team") String team);
}
