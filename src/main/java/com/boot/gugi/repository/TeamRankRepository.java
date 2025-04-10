package com.boot.gugi.repository;

import com.boot.gugi.model.TeamRank;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface TeamRankRepository extends JpaRepository<TeamRank, Long> {

    TeamRank findByTeam(String team);

    Optional<TeamRank> findByRankKey(String rankKey);

    @Query("SELECT r FROM TeamRank r WHERE r.rankKey NOT IN :keys")
    List<TeamRank> findAllExceptKeys(@Param("keys") Set<String> keys);
}