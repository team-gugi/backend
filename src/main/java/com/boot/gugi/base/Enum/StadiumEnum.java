package com.boot.gugi.base.Enum;

import com.boot.gugi.model.Translate;

import java.util.HashMap;
import java.util.Map;

public enum StadiumEnum implements Translate {
    JAMSIL(0, "잠실 야구장","잠실"),
    GOCHEOK(1, "고척 스카이돔", "고척"),
    INCHEON(2, "인천 SSG 랜더스필드", "문학"),
    DAEGU(3, "대구 삼성 라이온즈 파크", "대구"),
    DAEJEON(4, "대전 한화생명 이글스파크", "대전"),
    GWANGJU(5, "광주 기아 챔피언스 필드", "광주"),
    CHANGWON(6, "창원 NC 파크", "창원"),
    SUWON(7, "수원 KT 위즈 파크", "수원"),
    BUSAN(8, "부산 사직 야구장", "사직"),
    ANY(9, "상관없음", "없음"),
    CHEONGJU(10, "청주종합경기장", "청주"),
    ULSAN(11, "울산 문수 야구장", "울산"),
    POHANG(12, "포항 야구장", "포항"),
    NEWDAEJEON(13, "대전 한화생명 볼파크", "대전(신)");

    private final int id;
    private final String displayNameKorean;
    private final String shortName;
    private static final Map<String, StadiumEnum> KOREAN_NAME_MAP = new HashMap<>();

    static {
        for (StadiumEnum stadium : values()) {
            KOREAN_NAME_MAP.put(stadium.displayNameKorean, stadium);
            KOREAN_NAME_MAP.put(stadium.shortName, stadium);
        }
    }

    StadiumEnum(int id, String displayNameKorean, String shortName) {
        this.id = id;
        this.displayNameKorean = displayNameKorean;
        this.shortName = shortName;

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

    public static String getDisplayNameKoreanByShortName(String shortName) {
        StadiumEnum stadium = KOREAN_NAME_MAP.get(shortName);
        if (stadium != null) {
            return stadium.displayNameKorean;
        }
        throw new IllegalArgumentException("No enum constant for short name: " + shortName);
    }
}
