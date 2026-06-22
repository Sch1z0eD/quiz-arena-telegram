package com.quizarena.admin.web;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

import java.io.IOException;

/**
 * Serves the bundled admin UI (classpath:/static, produced by the Docker frontend build) and falls back to
 * index.html for client-side routes so React Router deep links survive a refresh. Active only with the panel
 * enabled. The fallback never applies to /api/** or to asset paths (anything with an extension), so it cannot
 * shadow the API or mask a missing script; real files are served as-is.
 */
@Configuration
@ConditionalOnProperty(prefix = "admin.panel", name = "enabled", havingValue = "true")
public class SpaStaticConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/")
                .resourceChain(true)
                .addResolver(new PathResourceResolver() {
                    @Override
                    protected Resource getResource(String resourcePath, Resource location) throws IOException {
                        Resource requested = location.createRelative(resourcePath);
                        if (requested.exists() && requested.isReadable()) {
                            return requested;
                        }
                        if (resourcePath.startsWith("api/")) {
                            return null;
                        }
                        String lastSegment = resourcePath.substring(resourcePath.lastIndexOf('/') + 1);
                        if (lastSegment.contains(".")) {
                            return null;
                        }
                        Resource index = new ClassPathResource("static/index.html");
                        return index.exists() ? index : null;
                    }
                });
    }
}
