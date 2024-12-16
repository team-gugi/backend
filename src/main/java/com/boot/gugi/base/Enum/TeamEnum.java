package com.boot.gugi.base.Enum;

import com.boot.gugi.model.Translate;

import java.util.HashMap;
import java.util.Map;

public enum TeamEnum implements Translate {

    KIA(0, "KIA 타이거즈"),
    KT(1, "KT 위즈"),
    LG(2, "LG 트윈스"),
    NC(3, "NC 다이노스"),
    SSG(4, "SSG 랜더스"),
    DOOSAN(5, "두산 베어스"),
    LOTTE(6, "롯데 자이언츠"),
    SAMSUNG(7, "삼성 라이온즈"),
    KIWOOM(8, "키움 히어로즈"),
    HANWHA(9, "한화 이글스"),
    ANY(10, "상관없음");

    private final int id;
    private final String displayNameKorean;
    private static final Map<String, TeamEnum> KOREAN_NAME_MAP = new HashMap<>();

    static {
        for (TeamEnum team : values()) {
            KOREAN_NAME_MAP.put(team.toKorean(), team);
        }
    }

    TeamEnum(int id, String displayNameKorean) {
        this.id = id;
        this.displayNameKorean = displayNameKorean;
    }

    public Integer getId() {
        return id;
    }

    public String toKorean() {
        return displayNameKorean;
    }

    public static TeamEnum fromString(String teamName) {
        TeamEnum team = KOREAN_NAME_MAP.get(teamName);
        if (team != null) {
            return team;
        }
        throw new IllegalArgumentException("No enum constant for team name: " + teamName);
    }
}