package com.quizarena.admin.web;

import com.quizarena.admin.auth.AdminSecurityConfig;
import com.quizarena.admin.auth.VerifiedAdmin;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminUserController.class)
@Import(AdminSecurityConfig.class)
@TestPropertySource(properties = "admin.panel.enabled=true")
class AdminUserSliceTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private AdminUserService service;

    @Test
    void readEndpointsRequireAuthentication() throws Exception {
        mvc.perform(get("/api/admin/users")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/admin/users/1")).andExpect(status().isUnauthorized());
    }

    @Test
    void listReturnsPageWhenAuthenticated() throws Exception {
        when(service.list(any(), any())).thenReturn(new PageResponse<>(
                List.of(new UserRow(7, "Alice", "al", "ru", 4, 75, 1200, 100L, 200L, false, false)), 0, 20, 1, 1));
        mvc.perform(get("/api/admin/users").with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(7))
                .andExpect(jsonPath("$.content[0].accuracyPercent").value(75));
    }

    @Test
    void detailReturns404WhenMissing() throws Exception {
        when(service.detail(anyLong())).thenReturn(Optional.empty());
        mvc.perform(get("/api/admin/users/999").with(admin())).andExpect(status().isNotFound());
    }

    @Test
    void nonWhitelistedSortFallsBackToLastSeenDesc() throws Exception {
        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        when(service.list(any(), captor.capture())).thenReturn(new PageResponse<>(List.of(), 0, 20, 0, 0));

        mvc.perform(get("/api/admin/users?sort=banned,asc").with(admin())).andExpect(status().isOk());

        assertEquals(Sort.by(Sort.Order.desc("lastSeen")), captor.getValue().getSort());
    }

    @Test
    void banRequiresAuthentication() throws Exception {
        mvc.perform(put("/api/admin/users/1/banned").with(csrf())
                .contentType(APPLICATION_JSON).content("{\"banned\":true}")).andExpect(status().isUnauthorized());
    }

    @Test
    void banRequiresCsrf() throws Exception {
        mvc.perform(put("/api/admin/users/1/banned").with(admin())
                .contentType(APPLICATION_JSON).content("{\"banned\":true}")).andExpect(status().isForbidden());
    }

    @Test
    void banSucceedsWithSessionAndCsrf() throws Exception {
        mvc.perform(put("/api/admin/users/1/banned").with(admin()).with(csrf())
                .contentType(APPLICATION_JSON).content("{\"banned\":true}")).andExpect(status().isNoContent());
    }

    private static RequestPostProcessor admin() {
        return authentication(UsernamePasswordAuthenticationToken.authenticated(
                new VerifiedAdmin(1, "Admin"), null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
    }
}
