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
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminQuestionController.class)
@Import(AdminSecurityConfig.class)
@TestPropertySource(properties = "admin.panel.enabled=true")
class AdminQuestionSliceTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private AdminQuestionService service;

    @Test
    void endpointsRequireAuthentication() throws Exception {
        mvc.perform(get("/api/admin/questions")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/admin/questions/1")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/admin/categories")).andExpect(status().isUnauthorized());
    }

    @Test
    void listReturnsPageWhenAuthenticated() throws Exception {
        when(service.list(anyString(), anyString(), anyString(), anyString(), any()))
                .thenReturn(new PageResponse<>(
                        List.of(new QuestionSummary(1, "Q", "science", "easy", "en")), 0, 20, 1, 1));
        mvc.perform(get("/api/admin/questions").with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void detailReturns404WhenMissing() throws Exception {
        when(service.detail(anyLong())).thenReturn(Optional.empty());
        mvc.perform(get("/api/admin/questions/999").with(admin())).andExpect(status().isNotFound());
    }

    @Test
    void categoriesReturnRowsWhenAuthenticated() throws Exception {
        when(service.categories()).thenReturn(List.of(new CategoryRow("science", 5, Map.of("en", 5L))));
        mvc.perform(get("/api/admin/categories").with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].slug").value("science"))
                .andExpect(jsonPath("$[0].total").value(5));
    }

    private static RequestPostProcessor admin() {
        return authentication(UsernamePasswordAuthenticationToken.authenticated(
                new VerifiedAdmin(1, "Admin"), null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
    }
}
