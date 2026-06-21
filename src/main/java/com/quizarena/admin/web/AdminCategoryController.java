package com.quizarena.admin.web;

import com.quizarena.admin.auth.VerifiedAdmin;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/categories")
@ConditionalOnProperty(prefix = "admin.panel", name = "enabled", havingValue = "true")
public class AdminCategoryController {

    private final AdminCategoryService service;

    public AdminCategoryController(AdminCategoryService service) {
        this.service = service;
    }

    @GetMapping
    public List<CategoryRow> list() {
        return service.list();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CategoryRow create(@AuthenticationPrincipal VerifiedAdmin admin, @RequestBody CreateCategoryRequest request) {
        return service.create(admin, request.names(), request.active());
    }

    @PutMapping("/{slug}")
    public CategoryRow update(@AuthenticationPrincipal VerifiedAdmin admin, @PathVariable String slug,
                              @RequestBody UpdateCategoryRequest request) {
        return service.update(admin, slug, request.names());
    }

    @PutMapping("/{slug}/active")
    public CategoryRow setActive(@AuthenticationPrincipal VerifiedAdmin admin, @PathVariable String slug,
                                 @RequestBody ActiveRequest request) {
        return service.setActive(admin, slug, request.active());
    }

    @DeleteMapping("/{slug}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal VerifiedAdmin admin, @PathVariable String slug) {
        service.delete(admin, slug);
    }
}
