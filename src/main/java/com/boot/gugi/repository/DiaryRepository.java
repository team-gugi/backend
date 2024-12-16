package com.boot.gugi.repository;

import com.boot.gugi.model.Diary;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DiaryRepository extends JpaRepository<Diary, UUID> {
    Optional<Diary> findByDiaryId(UUID diaryId);
}