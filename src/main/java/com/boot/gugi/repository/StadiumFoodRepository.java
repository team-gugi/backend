package com.boot.gugi.repository;

import com.boot.gugi.model.StadiumFood;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface StadiumFoodRepository extends JpaRepository<StadiumFood, Integer> {

    @Query("SELECT COUNT(sf) FROM StadiumFood sf WHERE sf.stadium.stadiumCode = :stadiumCode")
    long countByStadiumCode(@Param("stadiumCode") Integer stadiumCode);

    @Query("SELECT sf FROM StadiumFood sf JOIN FETCH sf.food WHERE sf.stadium.stadiumCode = :stadiumCode")
    List<StadiumFood> findByStadiumCodeWithFood(@Param("stadiumCode") Integer stadiumCode);
}

