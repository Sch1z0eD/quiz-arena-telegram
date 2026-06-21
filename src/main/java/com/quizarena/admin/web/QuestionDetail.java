package com.quizarena.admin.web;

import java.util.List;

public record QuestionDetail(long id, String text, List<String> options, int correctOption,
                             String category, String difficulty, String language, String hash,
                             boolean active, QuestionStats stats) {
}
