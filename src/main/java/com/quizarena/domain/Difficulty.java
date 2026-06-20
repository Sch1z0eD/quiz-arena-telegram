package com.quizarena.domain;

public enum Difficulty {
    EASY("easy"),
    MEDIUM("medium"),
    HARD("hard");

    private final String value;

    Difficulty(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }

    public static Difficulty fromValue(String value) {
        for (Difficulty difficulty : values()) {
            if (difficulty.value.equals(value)) {
                return difficulty;
            }
        }
        return null;
    }
}
