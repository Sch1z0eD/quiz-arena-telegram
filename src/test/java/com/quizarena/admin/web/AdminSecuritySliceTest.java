package com.quizarena.admin.web;

import com.quizarena.admin.auth.AdminSecurityConfig;
import com.quizarena.admin.auth.VerifiedAdmin;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminController.class)
@Import(AdminSecurityConfig.class)
@TestPropertySource(properties = "admin.panel.enabled=true")
class AdminSecuritySliceTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private AdminStatsService stats;

    @Test
    void meRequiresAuthentication() throws Exception {
        mvc.perform(get("/api/admin/me")).andExpect(status().isUnauthorized());
    }

    @Test
    void meReturnsPrincipalWhenAuthenticated() throws Exception {
        mvc.perform(get("/api/admin/me").with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(777))
                .andExpect(jsonPath("$.name").value("Alice"));
    }

    @Test
    void statsReturnsCountsWhenAuthenticated() throws Exception {
        when(stats.snapshot()).thenReturn(new StatsResponse(12, 340));
        mvc.perform(get("/api/admin/stats").with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.questions").value(12))
                .andExpect(jsonPath("$.answers").value(340));
    }

    @Test
    void overviewRequiresAuthentication() throws Exception {
        mvc.perform(get("/api/admin/stats/overview")).andExpect(status().isUnauthorized());
    }

    @Test
    void overviewReturnsDataWhenAuthenticated() throws Exception {
        when(stats.overview()).thenReturn(new OverviewResponse(
                new OverviewResponse.Players(5, 3, 4),
                new OverviewResponse.Games(2, 1, 1),
                new OverviewResponse.QuestionBreakdown(10, 2, List.of(), List.of(), List.of()),
                new OverviewResponse.CategoryStats(3, 1),
                List.of(new OverviewResponse.DailyCount("2026-06-21", 7)),
                List.of(new OverviewResponse.NamedCount("science", 9)),
                75, 100));
        mvc.perform(get("/api/admin/stats/overview").with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.players.total").value(5))
                .andExpect(jsonPath("$.accuracyPercent").value(75))
                .andExpect(jsonPath("$.answersPerDay[0].day").value("2026-06-21"))
                .andExpect(jsonPath("$.topCategories[0].name").value("science"));
    }

    private static RequestPostProcessor admin() {
        return authentication(UsernamePasswordAuthenticationToken.authenticated(
                new VerifiedAdmin(777, "Alice"), null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
    }
}
