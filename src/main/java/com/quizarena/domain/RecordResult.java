package com.quizarena.domain;

public record RecordResult(Status status, boolean correct, long points, boolean allAnswered) {

    public enum Status {
        NO_GAME,
        NOT_PARTICIPANT,
        LATE,
        DUPLICATE,
        ANSWERED
    }

    public static RecordResult of(Status status) {
        return new RecordResult(status, false, 0L, false);
    }
}
