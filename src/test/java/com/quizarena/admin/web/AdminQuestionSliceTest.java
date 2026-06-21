package com.quizarena.admin.web;

import com.quizarena.admin.auth.AdminSecurityConfig;
import com.quizarena.admin.auth.VerifiedAdmin;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminQuestionController.class)
@Import(AdminSecurityConfig.class)
@TestPropertySource(properties = "admin.panel.enabled=true")
class AdminQuestionSliceTest {

    private static final String BODY = "{\"text\":\"Q\",\"options\":[\"A\",\"B\",\"C\",\"D\"]," +
            "\"correctOption\":1,\"category\":\"science\",\"difficulty\":\"easy\",\"language\":\"en\"}";

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private AdminQuestionService service;

    @Test
    void readEndpointsRequireAuthentication() throws Exception {
        mvc.perform(get("/api/admin/questions")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/admin/questions/1")).andExpect(status().isUnauthorized());
    }

    @Test
    void mutationsRequireAuthentication() throws Exception {
        mvc.perform(post("/api/admin/questions").with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(BODY)).andExpect(status().isUnauthorized());
        mvc.perform(put("/api/admin/questions/1").with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(BODY)).andExpect(status().isUnauthorized());
        mvc.perform(put("/api/admin/questions/1/active").with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content("{\"active\":false}")).andExpect(status().isUnauthorized());
    }

    @Test
    void mutationsRequireCsrf() throws Exception {
        mvc.perform(post("/api/admin/questions").with(admin())
                .contentType(MediaType.APPLICATION_JSON).content(BODY)).andExpect(status().isForbidden());
    }

    @Test
    void listReturnsPageWhenAuthenticated() throws Exception {
        when(service.list(anyString(), anyString(), anyString(), anyString(), any()))
                .thenReturn(new PageResponse<>(
                        List.of(new QuestionSummary(1, "Q", "science", "easy", "en", true)), 0, 20, 1, 1));
        mvc.perform(get("/api/admin/questions").with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].active").value(true));
    }

    @Test
    void detailReturns404WhenMissing() throws Exception {
        when(service.detail(anyLong())).thenReturn(java.util.Optional.empty());
        mvc.perform(get("/api/admin/questions/999").with(admin())).andExpect(status().isNotFound());
    }

    @Test
    void createSucceedsWithSessionAndCsrf() throws Exception {
        when(service.create(any(), any())).thenReturn(detail());
        mvc.perform(post("/api/admin/questions").with(admin()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void duplicateReturnsConflictWithMessage() throws Exception {
        doThrow(new DuplicateQuestionException()).when(service).create(any(), any());
        mvc.perform(post("/api/admin/questions").with(admin()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("A question with the same text already exists"));
    }

    private static QuestionDetail detail() {
        return new QuestionDetail(1, "Q", List.of("A", "B", "C", "D"), 1, "science", "easy", "en",
                "hash", true, new QuestionStats(0, 0, 0));
    }

    private static RequestPostProcessor admin() {
        return authentication(UsernamePasswordAuthenticationToken.authenticated(
                new VerifiedAdmin(1, "Admin"), null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
    }
}
