package com.quizarena.admin.web;

import com.quizarena.repository.AnswerRepository;
import com.quizarena.repository.QuestionRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(prefix = "admin.panel", name = "enabled", havingValue = "true")
public class AdminStatsService {

    private final QuestionRepository questions;
    private final AnswerRepository answers;

    public AdminStatsService(QuestionRepository questions, AnswerRepository answers) {
        this.questions = questions;
        this.answers = answers;
    }

    public StatsResponse snapshot() {
        return new StatsResponse(questions.count(), answers.count());
    }
}
