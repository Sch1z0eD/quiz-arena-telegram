package com.quizarena.integration;

import com.quizarena.domain.Language;
import com.quizarena.service.LanguageRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LanguageRegistryIT extends AbstractIntegrationTest {

    @Autowired
    private LanguageRegistry registry;
    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void reloadExposesEnabledLanguagesAndHidesDisabledOnes() {
        jdbc.update("INSERT INTO languages (code, name, enabled) VALUES ('de', 'Deutsch', TRUE)");
        jdbc.update("INSERT INTO languages (code, name, enabled) VALUES ('zz', 'Disabled', FALSE)");
        try {
            registry.reload();

            List<String> codes = registry.enabled().stream().map(Language::code).toList();
            assertTrue(codes.containsAll(List.of("en", "ru", "de")), "enabled languages (incl. the new one) are listed");
            assertFalse(codes.contains("zz"), "a disabled language is not offered");
            assertTrue(registry.isEnabled("de"));
            assertFalse(registry.isEnabled("zz"));
        } finally {
            jdbc.update("DELETE FROM languages WHERE code IN ('de', 'zz')");
            registry.reload();
        }
    }
}
