package com.boot.gugi.base.Enum;

import com.boot.gugi.model.Translate;

import java.util.HashMap;
import java.util.Map;

public enum AgeRangeEnum implements Translate {
    AGE_10s(10,"10대"),
    AGE_20s(20,"20대"),
    AGE_30s(30,"30대");

    private final int id;
    private final String displayNameKorean;

    private static final Map<String, AgeRangeEnum> DISPLAY_AGE_MAP = new HashMap<>();

    static {
        for (AgeRangeEnum age : values()) {
            DISPLAY_AGE_MAP.put(age.toKorean(), age);
        }
    }

    AgeRangeEnum(int id, String displayNameKorean) {
        this.id = id;
        this.displayNameKorean = displayNameKorean;
    }

    public Integer getId() { return id; }

    public String toKorean() {
        return displayNameKorean;
    }

    public static AgeRangeEnum fromString(String ageRange) {
        AgeRangeEnum age = DISPLAY_AGE_MAP.get(ageRange);
        if (age != null) {
            return age;
        }
        throw new IllegalArgumentException("No enum constant for age range: " + ageRange);
    }
}
