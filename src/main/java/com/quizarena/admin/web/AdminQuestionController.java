package com.quizarena.admin.web;

import com.quizarena.admin.auth.VerifiedAdmin;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Set;

@RestController
@RequestMapping("/api/admin")
@ConditionalOnProperty(prefix = "admin.panel", name = "enabled", havingValue = "true")
public class AdminQuestionController {

    private static final Set<String> SORTABLE = Set.of("id", "category", "difficulty", "language");

    private final AdminQuestionService service;

    public AdminQuestionController(AdminQuestionService service) {
        this.service = service;
    }

    @GetMapping("/questions")
    public PageResponse<QuestionSummary> questions(
            @RequestParam(required = false, defaultValue = "") String q,
            @RequestParam(required = false, defaultValue = "") String category,
            @RequestParam(required = false, defaultValue = "") String difficulty,
            @RequestParam(required = false, defaultValue = "") String language,
            @PageableDefault(size = 20) Pageable pageable) {
        return service.list(q, category, difficulty, language, Pageables.sanitize(pageable, SORTABLE, Sort.by("id")));
    }

    @GetMapping("/questions/{id}")
    public QuestionDetail question(@PathVariable long id) {
        return service.detail(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    @PostMapping("/questions")
    @ResponseStatus(HttpStatus.CREATED)
    public QuestionDetail create(@AuthenticationPrincipal VerifiedAdmin admin, @RequestBody QuestionRequest request) {
        return service.create(admin, request);
    }

    @PutMapping("/questions/{id}")
    public QuestionDetail update(@AuthenticationPrincipal VerifiedAdmin admin, @PathVariable long id,
                                 @RequestBody QuestionRequest request) {
        return service.update(admin, id, request);
    }

    @PutMapping("/questions/{id}/active")
    public QuestionDetail setActive(@AuthenticationPrincipal VerifiedAdmin admin, @PathVariable long id,
                                    @RequestBody ActiveRequest request) {
        return service.setActive(admin, id, request.active());
    }

}
