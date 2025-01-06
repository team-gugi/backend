package com.boot.gugi.base.Enum;

import java.util.Map;
import java.util.Optional;

public enum ApplicationStatusEnum {
    PENDING("PENDING","대기"),
    ACCEPTED("ACCEPTED","수락"),
    REJECTED("REJECTED","거절");

    private final String displayEnglish;
    private final String displayKorean;

    private static final Map<String, ApplicationStatusEnum> DISPLAY_MATCH_MAP =
            Map.of(
                    PENDING.getKorean(), PENDING,
                    ACCEPTED.getKorean(), ACCEPTED,
                    REJECTED.getKorean(), REJECTED
            );

    ApplicationStatusEnum(String displayEnglish, String displayKorean) {
        this.displayEnglish = displayEnglish;
        this.displayKorean = displayKorean;
    }

    public String getEnglish() {
        return displayEnglish;
    }

    public String getKorean() {
        return displayKorean;
    }

    public static ApplicationStatusEnum fromKorean(String koreanStatus) {
        return Optional.ofNullable(DISPLAY_MATCH_MAP.get(koreanStatus))
                .orElseThrow(() -> new IllegalArgumentException("No enum constant for status: " + koreanStatus));
    }
}