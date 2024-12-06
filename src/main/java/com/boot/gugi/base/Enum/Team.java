package com.boot.gugi.base.Enum;

import com.boot.gugi.model.TeamName;

public enum Team implements TeamName {
    LG("LG", "LG 트윈스"),
    KT("KT", "KT 위즈"),
    SSG("SSG", "SSG 랜더스"),
    NC("NC", "NC 다이노스"),
    DOOSAN("DOOSAN", "두산 베어스"),
    KIA("KIA", "KIA 타이거즈"),
    LOTTE("LOTTE", "롯데 자이언츠"),
    SAMSUNG("SAMSUNG", "삼성 라이온즈"),
    HANWHA("HANWHA", "한화 이글스"),
    KIWOOM("KIWOOM", "키움 히어로즈"),
    ANY("ANY", "상관없음");

    private final String displayNameEnglish;
    private final String displayNameKorean;

    Team(String displayNameEnglish, String displayNameKorean) {
        this.displayNameEnglish = displayNameEnglish;
        this.displayNameKorean = displayNameKorean;
    }

    @Override
    public String toEnglish() {
        return displayNameEnglish;
    }

    @Override
    public String toKorean() {
        return displayNameKorean;
    }
}