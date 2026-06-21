package com.quizarena.admin.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.util.List;

@ConfigurationProperties("admin.panel")
public record AdminPanelProperties(
        @DefaultValue("false") boolean enabled,
        @DefaultValue List<Long> admins,
        @DefaultValue("86400") long authMaxAgeSeconds,
        @DefaultValue("false") boolean devLogin,
        @DefaultValue("0") long devAdminId,
        @DefaultValue("Dev Admin") String devAdminName) {
}
