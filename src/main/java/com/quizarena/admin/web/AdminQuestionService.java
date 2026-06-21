package com.quizarena.admin.web;

import com.quizarena.admin.audit.AuditService;
import com.quizarena.admin.auth.VerifiedAdmin;
import com.quizarena.domain.Difficulty;
import com.quizarena.domain.Question;
import com.quizarena.repository.AnswerRepository;
import com.quizarena.repository.CategoryRepository;
import com.quizarena.repository.QuestionRepository;
import com.quizarena.service.QuestionHash;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@ConditionalOnProperty(prefix = "admin.panel", name = "enabled", havingValue = "true")
public class AdminQuestionService {

    private static final int MAX_TEXT_LENGTH = 500;
    private static final int MAX_OPTION_LENGTH = 200;
    private static final Set<String> DIFFICULTIES =
            Arrays.stream(Difficulty.values()).map(Difficulty::value).collect(Collectors.toUnmodifiableSet());
    private static final Set<String> LANGUAGES = Set.of("en", "ru");

    private final QuestionRepository questions;
    private final AnswerRepository answers;
    private final CategoryRepository categories;
    private final AuditService audit;

    public AdminQuestionService(QuestionRepository questions, AnswerRepository answers,
                                CategoryRepository categories, AuditService audit) {
        this.questions = questions;
        this.answers = answers;
        this.categories = categories;
        this.audit = audit;
    }

    public PageResponse<QuestionSummary> list(String text, String category, String difficulty,
                                              String language, Pageable pageable) {
        Page<Question> page = questions.search(nz(text), nz(category), nz(difficulty), nz(language), pageable);
        List<QuestionSummary> content = page.getContent().stream()
                .map(q -> new QuestionSummary(q.getId(), q.getText(), q.getCategory(), q.getDifficulty(),
                        q.getLanguage(), q.isActive()))
                .toList();
        return new PageResponse<>(content, page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages());
    }

    public Optional<QuestionDetail> detail(long id) {
        return questions.findById(id).map(this::toDetail);
    }

    @Transactional
    public QuestionDetail create(VerifiedAdmin admin, QuestionRequest request) {
        Validated v = validate(request);
        String hash = QuestionHash.of(v.text());
        if (questions.existsByQuestionHash(hash)) {
            throw new DuplicateQuestionException();
        }
        Question question = new Question(v.text(), v.options().get(0), v.options().get(1), v.options().get(2),
                v.options().get(3), v.correctOption(), v.category(), v.difficulty(), v.language(), hash);
        Question saved = questions.save(question);
        audit.record(admin, "question.created", String.valueOf(saved.getId()), v.text());
        return toDetail(saved);
    }

    @Transactional
    public QuestionDetail update(VerifiedAdmin admin, long id, QuestionRequest request) {
        Validated v = validate(request);
        Question question = questions.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        String hash = question.getQuestionHash();
        if (!question.getText().equals(v.text())) {
            hash = QuestionHash.of(v.text());
            if (questions.existsByQuestionHash(hash)) {
                throw new DuplicateQuestionException();
            }
        }
        question.update(v.text(), v.options().get(0), v.options().get(1), v.options().get(2),
                v.options().get(3), v.correctOption(), v.category(), v.difficulty(), v.language(), hash);
        questions.save(question);
        audit.record(admin, "question.updated", String.valueOf(id), v.text());
        return toDetail(question);
    }

    @Transactional
    public QuestionDetail setActive(VerifiedAdmin admin, long id, boolean active) {
        Question question = questions.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        question.setActive(active);
        questions.save(question);
        audit.record(admin, active ? "question.enabled" : "question.disabled", String.valueOf(id), null);
        return toDetail(question);
    }

    private QuestionDetail toDetail(Question q) {
        long answered = answers.countByQuestionId(q.getId());
        long correct = answers.countByQuestionIdAndCorrectTrue(q.getId());
        int accuracy = answered == 0 ? 0 : (int) Math.round(correct * 100.0 / answered);
        List<String> options = List.of(q.getOptionByIndex(0), q.getOptionByIndex(1),
                q.getOptionByIndex(2), q.getOptionByIndex(3));
        return new QuestionDetail(q.getId(), q.getText(), options, q.getCorrectOption(),
                q.getCategory(), q.getDifficulty(), q.getLanguage(), q.getQuestionHash(),
                q.isActive(), new QuestionStats(answered, correct, accuracy));
    }

    private Validated validate(QuestionRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("request is required");
        }
        String text = trimRequired(request.text(), "text", MAX_TEXT_LENGTH);
        List<String> options = request.options();
        if (options == null || options.size() != 4) {
            throw new IllegalArgumentException("exactly 4 options are required");
        }
        List<String> trimmed = new ArrayList<>(4);
        for (int i = 0; i < options.size(); i++) {
            trimmed.add(trimRequired(options.get(i), "option " + (i + 1), MAX_OPTION_LENGTH));
        }
        if (request.correctOption() < 0 || request.correctOption() > 3) {
            throw new IllegalArgumentException("correctOption must be between 0 and 3");
        }
        String category = request.category() == null ? "" : request.category().trim();
        if (category.isEmpty() || !categories.existsBySlug(category)) {
            throw new IllegalArgumentException("unknown category");
        }
        String difficulty = request.difficulty() == null ? "" : request.difficulty().trim();
        if (!DIFFICULTIES.contains(difficulty)) {
            throw new IllegalArgumentException("unsupported difficulty");
        }
        String language = request.language() == null ? "" : request.language().trim();
        if (!LANGUAGES.contains(language)) {
            throw new IllegalArgumentException("unsupported language");
        }
        return new Validated(text, trimmed, request.correctOption(), category, difficulty, language);
    }

    private static String trimRequired(String value, String field, int max) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        String trimmed = value.trim();
        if (trimmed.length() > max) {
            throw new IllegalArgumentException(field + " exceeds " + max + " characters");
        }
        return trimmed;
    }

    private static String nz(String value) {
        return value == null ? "" : value.trim();
    }

    private record Validated(String text, List<String> options, int correctOption,
                             String category, String difficulty, String language) {
    }
}
