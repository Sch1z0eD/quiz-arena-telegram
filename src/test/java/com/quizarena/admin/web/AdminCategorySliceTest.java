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
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminCategoryController.class)
@Import(AdminSecurityConfig.class)
@TestPropertySource(properties = "admin.panel.enabled=true")
class AdminCategorySliceTest {

    private static final String BODY = "{\"names\":{\"ru\":\"Наука\",\"en\":\"Science\"}}";

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private AdminCategoryService service;

    @Test
    void readRequiresAuthentication() throws Exception {
        mvc.perform(get("/api/admin/categories")).andExpect(status().isUnauthorized());
    }

    @Test
    void mutationsRequireAuthentication() throws Exception {
        mvc.perform(post("/api/admin/categories").with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(BODY)).andExpect(status().isUnauthorized());
        mvc.perform(delete("/api/admin/categories/science").with(csrf())).andExpect(status().isUnauthorized());
        mvc.perform(put("/api/admin/categories/science/active").with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content("{\"active\":false}")).andExpect(status().isUnauthorized());
    }

    @Test
    void mutationsRequireCsrf() throws Exception {
        mvc.perform(post("/api/admin/categories").with(admin())
                .contentType(MediaType.APPLICATION_JSON).content(BODY)).andExpect(status().isForbidden());
    }

    @Test
    void createSucceedsWithSessionAndCsrf() throws Exception {
        when(service.create(any(), any(), anyBoolean()))
                .thenReturn(new CategoryRow("science", Map.of("ru", "Наука", "en", "Science"), false, 0, Map.of()));
        mvc.perform(post("/api/admin/categories").with(admin()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.slug").value("science"))
                .andExpect(jsonPath("$.active").value(false));
    }

    @Test
    void toggleActiveSucceedsWithSessionAndCsrf() throws Exception {
        when(service.setActive(any(), eq("science"), eq(true)))
                .thenReturn(new CategoryRow("science", Map.of("ru", "Наука", "en", "Science"), true, 0, Map.of()));
        mvc.perform(put("/api/admin/categories/science/active").with(admin()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"active\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void toggleActiveRequiresCsrf() throws Exception {
        mvc.perform(put("/api/admin/categories/science/active").with(admin())
                .contentType(MediaType.APPLICATION_JSON).content("{\"active\":true}")).andExpect(status().isForbidden());
    }

    @Test
    void deleteInUseReturnsConflictWithMessage() throws Exception {
        doThrow(new CategoryInUseException(5)).when(service).delete(any(), eq("science"));
        mvc.perform(delete("/api/admin/categories/science").with(admin()).with(csrf()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("5 questions reference this category"));
    }

    private static RequestPostProcessor admin() {
        return authentication(UsernamePasswordAuthenticationToken.authenticated(
                new VerifiedAdmin(1, "Admin"), null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
    }
}
