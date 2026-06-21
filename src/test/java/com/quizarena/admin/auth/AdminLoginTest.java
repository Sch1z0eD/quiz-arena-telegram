package com.quizarena.admin.auth;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class AdminLoginTest {

    private final AdminLogin login = new AdminLogin(new HttpSessionSecurityContextRepository());

    @Test
    void rotatesSessionIdToPreventFixation() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockHttpSession fixated = new MockHttpSession();
        request.setSession(fixated);
        String originalId = fixated.getId();

        login.establish(new VerifiedAdmin(1L, "Dev"), request, response);

        assertNotNull(request.getSession(false));
        assertNotEquals(originalId, request.getSession(false).getId());
    }
}
