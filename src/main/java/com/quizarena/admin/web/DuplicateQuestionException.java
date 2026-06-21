package com.quizarena.admin.web;

public class DuplicateQuestionException extends RuntimeException {

    public DuplicateQuestionException() {
        super("A question with the same text already exists");
    }
}
