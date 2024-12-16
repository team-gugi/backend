package com.boot.gugi.base.Enum;

import com.boot.gugi.model.Translate;

import java.util.HashMap;
import java.util.Map;

public enum StadiumEnum implements Translate {
    JAMSIL(0, "잠실 야구장"),
    GOCHEOK(1, "고척 스카이돔"),
    INCHEON(2, "인천 SSG 랜더스필드"),
    DAEGU(3, "대구 삼성 라이온즈 파크"),
    DAEJEON(4, "대전 한화생명 이글스파크"),
    GWANGJU(5, "광주 기아 챔피언스 필드"),
    CHANGWON(6, "창원 NC 파크"),
    SUWON(7, "수원 KT 위즈 파크"),
    BUSAN(8, "부산 사직 야구장"),
    ANY(9, "상관없음");

    private final int id;
    private final String displayNameKorean;
    private static final Map<String, StadiumEnum> KOREAN_NAME_MAP = new HashMap<>();

    static {
        for (StadiumEnum stadium : values()) {
            KOREAN_NAME_MAP.put(stadium.toKorean(), stadium);
        }
    }

    StadiumEnum(int id, String displayNameKorean) {
        this.id = id;
        this.displayNameKorean = displayNameKorean;
    }

    public Integer getId() {
        return id;
    }

    public String toKorean() {
        return displayNameKorean;
    }

    public static StadiumEnum fromString(String stadiumName) {
        StadiumEnum stadium = KOREAN_NAME_MAP.get(stadiumName);
        if (stadium != null) {
            return stadium;
        }
        throw new IllegalArgumentException("No enum constant for stadium name: " + stadiumName);
    }
}
