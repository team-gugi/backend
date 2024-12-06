package com.boot.gugi.repository;

import com.boot.gugi.model.TeamRank;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TeamRankRepository extends JpaRepository<TeamRank, Long> {

    TeamRank findByTeam(String team);
}