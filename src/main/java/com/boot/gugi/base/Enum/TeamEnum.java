package com.boot.gugi.base.Enum;

import com.boot.gugi.model.Translate;

import java.util.HashMap;
import java.util.Map;

public enum TeamEnum implements Translate {
    KIA(0, "KIA 타이거즈", "KIA", "kia"),
    KT(1, "KT 위즈", "KT", "kt"),
    LG(2, "LG 트윈스", "LG", "lg"),
    NC(3, "NC 다이노스", "NC", "nc"),
    SSG(4, "SSG 랜더스", "SSG", "ssg"),
    DOOSAN(5, "두산 베어스", "두산", "doosan"),
    LOTTE(6, "롯데 자이언츠", "롯데", "lotte"),
    SAMSUNG(7, "삼성 라이온즈", "삼성", "samsung"),
    KIWOOM(8, "키움 히어로즈","키움", "kiwoom"),
    HANWHA(9, "한화 이글스", "한화", "hanwha"),
    ANY(10, "상관없음", "없음", "any");

    private final int id;
    private final String displayNameKorean;
    private final String shortName;
    private final String lowerCase;
    private static final Map<String, TeamEnum> KOREAN_NAME_MAP = new HashMap<>();

    static {
        for (TeamEnum team : values()) {
            KOREAN_NAME_MAP.put(team.displayNameKorean, team);
            KOREAN_NAME_MAP.put(team.shortName, team);
            KOREAN_NAME_MAP.put(team.lowerCase, team);
        }
    }

    TeamEnum(int id, String displayNameKorean, String shortName, String lowerCase) {
        this.id = id;
        this.displayNameKorean = displayNameKorean;
        this.shortName = shortName;
        this.lowerCase = lowerCase;
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

    public static String getShortNameByLowerCase(String lowerCase) {
        TeamEnum team = KOREAN_NAME_MAP.get(lowerCase);
        if (team != null) {
            return team.shortName;
        }
        throw new IllegalArgumentException("No enum constant for lower name: " + lowerCase);
    }
}