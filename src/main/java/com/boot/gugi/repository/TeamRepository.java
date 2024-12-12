package com.boot.gugi.repository;

import com.boot.gugi.base.dto.TeamDTO;
import com.boot.gugi.model.Team;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TeamRepository extends JpaRepository<Team, Long> {

    Team findByTeamCode(String teamCode);
}
