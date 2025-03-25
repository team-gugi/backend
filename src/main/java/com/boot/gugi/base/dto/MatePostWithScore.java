package com.boot.gugi.base.dto;

import com.boot.gugi.model.MatePost;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class MatePostWithScore {
    private MatePost matePost;
    private long matchScore;
}