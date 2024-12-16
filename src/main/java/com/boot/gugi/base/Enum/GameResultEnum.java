package com.boot.gugi.base.Enum;

public enum GameResultEnum {
    WIN("WIN"),
    LOSE("LOSE"),
    DRAW("DRAW");

    private final String displayNameEnglish;

    GameResultEnum(String displayNameEnglish) {
        this.displayNameEnglish = displayNameEnglish;
    }

    public String toEnglish() {
        return displayNameEnglish;
    }
}