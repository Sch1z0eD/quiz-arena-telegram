package com.quizarena.admin.web;

import com.quizarena.admin.audit.AuditService;
import com.quizarena.admin.auth.VerifiedAdmin;
import com.quizarena.domain.Question;
import com.quizarena.repository.AnswerRepository;
import com.quizarena.repository.CategoryRepository;
import com.quizarena.repository.QuestionRepository;
import com.quizarena.service.LanguageRegistry;
import com.quizarena.service.QuestionHash;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminQuestionServiceTest {

    private static final VerifiedAdmin ADMIN = new VerifiedAdmin(1, "Admin");

    private final QuestionRepository questions = mock(QuestionRepository.class);
    private final AnswerRepository answers = mock(AnswerRepository.class);
    private final CategoryRepository categories = mock(CategoryRepository.class);
    private final AuditService audit = mock(AuditService.class);
    private final LanguageRegistry languageRegistry = mock(LanguageRegistry.class);
    private final AdminQuestionService service =
            new AdminQuestionService(questions, answers, categories, audit, languageRegistry);

    @BeforeEach
    void enableSeededLanguages() {
        lenient().when(languageRegistry.isEnabled("en")).thenReturn(true);
        lenient().when(languageRegistry.isEnabled("ru")).thenReturn(true);
    }

    @Test
    void detailComputesAccuracyFromAnswers() {
        when(questions.findById(7L)).thenReturn(Optional.of(question(7L)));
        when(answers.countByQuestionId(7L)).thenReturn(4L);
        when(answers.countByQuestionIdAndCorrectTrue(7L)).thenReturn(3L);

        QuestionDetail detail = service.detail(7L).orElseThrow();

        assertEquals(7L, detail.id());
        assertEquals(75, detail.stats().accuracyPercent());
        assertEquals(List.of("A", "B", "C", "D"), detail.options());
        assertTrue(detail.active());
    }

    @Test
    void listMapsPageMetadataAndContent() {
        when(questions.search("", "", "", "", PageRequest.of(1, 20)))
                .thenReturn(new PageImpl<>(List.of(question(7L)), PageRequest.of(1, 20), 45));

        PageResponse<QuestionSummary> response = service.list(null, null, null, null, PageRequest.of(1, 20));

        assertEquals(45, response.totalElements());
        assertEquals(7L, response.content().get(0).id());
        assertTrue(response.content().get(0).active());
    }

    @Test
    void createValidatesHashesPersistsAndAudits() {
        when(categories.existsBySlug("science")).thenReturn(true);
        String hash = QuestionHash.of("Capital of Australia?");
        when(questions.existsByQuestionHash(hash)).thenReturn(false);
        when(questions.save(any())).thenAnswer(invocation -> {
            Question saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 100L);
            return saved;
        });

        service.create(ADMIN, request("Capital of Australia?"));

        ArgumentCaptor<Question> saved = ArgumentCaptor.forClass(Question.class);
        verify(questions).save(saved.capture());
        assertEquals(hash, saved.getValue().getQuestionHash());
        assertTrue(saved.getValue().isActive());
        verify(audit).record(eq(ADMIN), eq("question.created"), eq("100"), anyString());
    }

    @Test
    void createRejectsDuplicateText() {
        when(categories.existsBySlug("science")).thenReturn(true);
        when(questions.existsByQuestionHash(QuestionHash.of("Capital of Australia?"))).thenReturn(true);

        assertThrows(DuplicateQuestionException.class, () -> service.create(ADMIN, request("Capital of Australia?")));
        verify(questions, never()).save(any());
        verify(audit, never()).record(any(), anyString(), anyString(), any());
    }

    @Test
    void createRejectsWrongOptionCount() {
        when(categories.existsBySlug("science")).thenReturn(true);
        QuestionRequest bad = new QuestionRequest("Q", List.of("A", "B", "C"), 0, "science", "easy", "en");
        assertThrows(IllegalArgumentException.class, () -> service.create(ADMIN, bad));
        verify(questions, never()).save(any());
    }

    @Test
    void createRejectsUnknownCategory() {
        when(categories.existsBySlug("nope")).thenReturn(false);
        QuestionRequest bad = new QuestionRequest("Q", List.of("A", "B", "C", "D"), 0, "nope", "easy", "en");
        assertThrows(IllegalArgumentException.class, () -> service.create(ADMIN, bad));
        verify(questions, never()).save(any());
    }

    @Test
    void updateRecomputesHashWhenTextChangesAndDetectsDuplicate() {
        when(categories.existsBySlug("science")).thenReturn(true);
        Question existing = question(7L);
        when(questions.findById(7L)).thenReturn(Optional.of(existing));
        when(questions.existsByQuestionHash(QuestionHash.of("Changed text?"))).thenReturn(true);

        assertThrows(DuplicateQuestionException.class, () -> service.update(ADMIN, 7L, request("Changed text?")));
        verify(audit, never()).record(any(), anyString(), anyString(), any());
    }

    @Test
    void updateRenamesRecomputesHashAndAudits() {
        when(categories.existsBySlug("science")).thenReturn(true);
        Question existing = question(7L);
        when(questions.findById(7L)).thenReturn(Optional.of(existing));
        when(questions.existsByQuestionHash(QuestionHash.of("Changed text?"))).thenReturn(false);

        service.update(ADMIN, 7L, request("Changed text?"));

        assertEquals(QuestionHash.of("Changed text?"), existing.getQuestionHash());
        verify(audit).record(eq(ADMIN), eq("question.updated"), eq("7"), anyString());
    }

    @Test
    void updateKeepsHashWhenTextUnchanged() {
        when(categories.existsBySlug("science")).thenReturn(true);
        Question existing = question(7L);
        String originalHash = existing.getQuestionHash();
        when(questions.findById(7L)).thenReturn(Optional.of(existing));

        service.update(ADMIN, 7L, request("Q text"));

        assertEquals(originalHash, existing.getQuestionHash());
        verify(questions, never()).existsByQuestionHash(anyString());
        verify(audit).record(eq(ADMIN), eq("question.updated"), eq("7"), anyString());
    }

    @Test
    void disableTogglesActiveAndAudits() {
        Question existing = question(7L);
        when(questions.findById(7L)).thenReturn(Optional.of(existing));

        service.setActive(ADMIN, 7L, false);

        assertTrue(!existing.isActive());
        verify(audit).record(eq(ADMIN), eq("question.disabled"), eq("7"), isNull());
    }

    @Test
    void enableTogglesActiveAndAudits() {
        Question existing = question(7L);
        existing.setActive(false);
        when(questions.findById(7L)).thenReturn(Optional.of(existing));

        service.setActive(ADMIN, 7L, true);

        assertTrue(existing.isActive());
        verify(audit).record(eq(ADMIN), eq("question.enabled"), eq("7"), isNull());
    }

    private static QuestionRequest request(String text) {
        return new QuestionRequest(text, List.of("A", "B", "C", "D"), 1, "science", "easy", "en");
    }

    private static Question question(long id) {
        Question q = new Question("Q text", "A", "B", "C", "D", 1, "science", "easy", "en", "h" + id);
        ReflectionTestUtils.setField(q, "id", id);
        return q;
    }
}
