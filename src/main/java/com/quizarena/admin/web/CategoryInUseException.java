package com.quizarena.admin.web;

public class CategoryInUseException extends RuntimeException {

    private final long questionCount;

    public CategoryInUseException(long questionCount) {
        super(questionCount + " questions reference this category");
        this.questionCount = questionCount;
    }

    public long questionCount() {
        return questionCount;
    }
}
