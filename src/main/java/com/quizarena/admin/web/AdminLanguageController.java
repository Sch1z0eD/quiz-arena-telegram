package com.quizarena.admin.web;

import com.quizarena.domain.Language;
import com.quizarena.service.LanguageRegistry;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/languages")
@ConditionalOnProperty(prefix = "admin.panel", name = "enabled", havingValue = "true")
public class AdminLanguageController {

    private final LanguageRegistry registry;

    public AdminLanguageController(LanguageRegistry registry) {
        this.registry = registry;
    }

    @GetMapping
    public List<Language> enabled() {
        return registry.enabled();
    }
}
