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

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminAuditController.class)
@Import(AdminSecurityConfig.class)
@TestPropertySource(properties = "admin.panel.enabled=true")
class AdminAuditSliceTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private AdminAuditService service;

    @Test
    void readEndpointsRequireAuthentication() throws Exception {
        mvc.perform(get("/api/admin/audit")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/admin/audit/actions")).andExpect(status().isUnauthorized());
    }

    @Test
    void listReturnsPageWhenAuthenticated() throws Exception {
        when(service.list(any(), any(), any(), any(), any(), any())).thenReturn(new PageResponse<>(
                List.of(new AuditEntry(1, Instant.parse("2026-06-21T10:00:00Z"), 7, "category.created", "science", "ru=Наука")),
                0, 20, 1, 1));
        mvc.perform(get("/api/admin/audit").with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].action").value("category.created"))
                .andExpect(jsonPath("$.content[0].adminId").value(7));
    }

    @Test
    void nonWhitelistedSortFallsBackToTsDesc() throws Exception {
        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        when(service.list(any(), any(), any(), any(), any(), captor.capture()))
                .thenReturn(new PageResponse<>(List.of(), 0, 20, 0, 0));

        mvc.perform(get("/api/admin/audit?sort=details,asc").with(admin())).andExpect(status().isOk());

        assertEquals(Sort.by(Sort.Order.desc("ts")), captor.getValue().getSort());
    }

    @Test
    void actionsReturnsListWhenAuthenticated() throws Exception {
        when(service.actions()).thenReturn(List.of("category.created", "question.created"));
        mvc.perform(get("/api/admin/audit/actions").with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").value("category.created"));
    }

    private static RequestPostProcessor admin() {
        return authentication(UsernamePasswordAuthenticationToken.authenticated(
                new VerifiedAdmin(1, "Admin"), null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
    }
}
