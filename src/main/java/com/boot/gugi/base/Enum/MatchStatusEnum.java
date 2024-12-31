package com.boot.gugi.base.Enum;

import java.util.HashMap;
import java.util.Map;

public enum MatchStatusEnum {
    PENDING("PENDING","대기"),
    ACCEPTED("ACCEPTED","수락"),
    REJECTED("REJECTED","거절");

    private final String displayEnglish;
    private final String displayKorean;

    private static final Map<String, MatchStatusEnum> DISPLAY_MATCH_MAP = new HashMap<>();

    static {
        for (MatchStatusEnum gender : values()) {
            DISPLAY_MATCH_MAP.put(gender.getKorean(), gender);
        }
    }

    MatchStatusEnum(String displayEnglish, String displayKorean) {
        this.displayEnglish = displayEnglish;
        this.displayKorean = displayKorean;
    }

    public String getEnglish() {
        return displayEnglish;
    }

    public String getKorean() {
        return displayKorean;
    }

    public static MatchStatusEnum fromKorean(String matchStatus) {
        MatchStatusEnum status = DISPLAY_MATCH_MAP.get(matchStatus);
        if (status != null) {
            return status;
        }
        throw new IllegalArgumentException("No enum constant for match status: " + matchStatus);
    }
}
