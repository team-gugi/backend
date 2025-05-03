package com.boot.gugi.base.Enum;

public enum SexEnum {
    MALE(1, "남자"),
    FEMALE(2, "여자");

    private final int id;
    private final String displayKorean;

    SexEnum(int id, String displayKorean) {
        this.id = id;
        this.displayKorean = displayKorean;
    }

    public Integer getId() { return id; }

    public String toKorean() {
        return displayKorean;
    }

    public static SexEnum fromKorean(String korean) {
        for (SexEnum sex : values()) {
            if (sex.toKorean().equals(korean)) {
                return sex;
            }
        }
        throw new IllegalArgumentException("Invalid gender display name: " + korean);
    }

    public static SexEnum fromId(int id) {
        for (SexEnum sex : values()) {
            if (sex.getId() == id) {
                return sex;
            }
        }
        throw new IllegalArgumentException("Invalid gender code: " + id);
    }
}
