package com.quizarena.admin.web;

import java.util.List;

public record QuestionRequest(String text, List<String> options, int correctOption,
                              String category, String difficulty, String language) {
}
