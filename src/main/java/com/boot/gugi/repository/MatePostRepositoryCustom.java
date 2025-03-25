package com.boot.gugi.repository;

import com.boot.gugi.base.dto.MateDTO;
import com.boot.gugi.base.dto.MatePostWithScore;
import com.boot.gugi.model.MatePost;

import java.time.LocalDateTime;
import java.util.List;

public interface MatePostRepositoryCustom {
    List<MatePost> findPostsSortedByDate(LocalDateTime cursor, int size);
    List<MatePostWithScore> findPostsSortedByRelevance(String cursor, int size, MateDTO.RequestOption matePostOptions);
}