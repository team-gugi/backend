package com.boot.gugi.base.Enum;

import java.util.HashMap;
import java.util.Map;

public enum GenderEnum {
    MALE_ONLY("FO","남자만"),
    FEMALE_ONLY("MO","여자만"),
    ANY("ANY","상관없음");

    private final String displayEnglish;
    private final String displayKorean;

    private static final Map<String, GenderEnum> DISPLAY_GENDER_MAP = new HashMap<>();

    static {
        for (GenderEnum gender : values()) {
            DISPLAY_GENDER_MAP.put(gender.toKorean(), gender);
        }
    }

    GenderEnum(String displayEnglish, String displayKorean) {
        this.displayEnglish = displayEnglish;
        this.displayKorean = displayKorean;
    }

    public String toEnglish() {
        return displayEnglish;
    }

    public String toKorean() {
        return displayKorean;
    }

    public static GenderEnum fromKorean(String genderChoice) {
        if (genderChoice == null || genderChoice.isBlank()) {
            return GenderEnum.ANY;
        }

        GenderEnum gender = DISPLAY_GENDER_MAP.get(genderChoice);
        if (gender != null) {
            return gender;
        }
        throw new IllegalArgumentException("No enum constant for gender choice: " + genderChoice);
    }
}
