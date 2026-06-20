package com.quizarena.render;

import com.quizarena.i18n.Localizer;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class BannerRenderer {

    private final SvgCardRenderer svg;
    private final Localizer localizer;
    private final ConcurrentHashMap<String, byte[]> cache = new ConcurrentHashMap<>();

    public BannerRenderer(SvgCardRenderer svg, Localizer localizer) {
        this.svg = svg;
        this.localizer = localizer;
    }

    public byte[] render(String template, Locale locale) {
        return cache.computeIfAbsent(template + ":" + locale.getLanguage(), key -> {
            String filled = svg.loadTemplate(template)
                    .replace("{{TAGLINE}}", svg.xmlEscape(localizer.get(locale, "banner.tagline")));
            return svg.rasterize(filled);
        });
    }
}
