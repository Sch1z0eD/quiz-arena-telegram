package com.quizarena.admin.web;

import com.quizarena.domain.Question;
import com.quizarena.repository.AnswerRepository;
import com.quizarena.repository.QuestionRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

@Service
@ConditionalOnProperty(prefix = "admin.panel", name = "enabled", havingValue = "true")
public class AdminQuestionService {

    private final QuestionRepository questions;
    private final AnswerRepository answers;

    public AdminQuestionService(QuestionRepository questions, AnswerRepository answers) {
        this.questions = questions;
        this.answers = answers;
    }

    public PageResponse<QuestionSummary> list(String text, String category, String difficulty,
                                              String language, Pageable pageable) {
        Page<Question> page = questions.search(nz(text), nz(category), nz(difficulty), nz(language), pageable);
        List<QuestionSummary> content = page.getContent().stream()
                .map(q -> new QuestionSummary(q.getId(), q.getText(), q.getCategory(), q.getDifficulty(), q.getLanguage()))
                .toList();
        return new PageResponse<>(content, page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages());
    }

    public Optional<QuestionDetail> detail(long id) {
        return questions.findById(id).map(this::toDetail);
    }

    public List<CategoryRow> categories() {
        Map<String, Map<String, Long>> grouped = new LinkedHashMap<>();
        for (QuestionRepository.CategoryLanguageCount row : questions.categoryCounts()) {
            grouped.computeIfAbsent(row.getCategory(), key -> new TreeMap<>()).put(row.getLanguage(), row.getCount());
        }
        return grouped.entrySet().stream()
                .map(entry -> new CategoryRow(entry.getKey(),
                        entry.getValue().values().stream().mapToLong(Long::longValue).sum(),
                        entry.getValue()))
                .sorted(Comparator.comparing(CategoryRow::slug))
                .toList();
    }

    private QuestionDetail toDetail(Question q) {
        long answered = answers.countByQuestionId(q.getId());
        long correct = answers.countByQuestionIdAndCorrectTrue(q.getId());
        int accuracy = answered == 0 ? 0 : (int) Math.round(correct * 100.0 / answered);
        List<String> options = List.of(q.getOptionByIndex(0), q.getOptionByIndex(1),
                q.getOptionByIndex(2), q.getOptionByIndex(3));
        return new QuestionDetail(q.getId(), q.getText(), options, q.getCorrectOption(),
                q.getCategory(), q.getDifficulty(), q.getLanguage(), q.getQuestionHash(),
                new QuestionStats(answered, correct, accuracy));
    }

    private static String nz(String value) {
        return value == null ? "" : value.trim();
    }
}
