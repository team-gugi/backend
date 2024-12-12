package com.boot.gugi.repository;

import com.boot.gugi.model.Stadium;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StadiumRepository extends JpaRepository<Stadium, Long> {
    Stadium findByStadiumCode(Integer stadiumCode);
}
