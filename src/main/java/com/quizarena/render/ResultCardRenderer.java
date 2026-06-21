package com.quizarena.render;

import com.quizarena.i18n.Localizer;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Map;

@Component
public class ResultCardRenderer {

    private final SvgCardRenderer svg;
    private final Localizer localizer;

    public ResultCardRenderer(SvgCardRenderer svg, Localizer localizer) {
        this.svg = svg;
        this.localizer = localizer;
    }

    public byte[] render(String categorySlug, String name, long score, int accuracyPercent, long correct,
                         Long place, byte[] avatar, Locale locale) {
        String template = svg.loadTemplate("result_card.svg")
                .replace("{{AVATAR}}", svg.avatarSlot(avatar, svg.initials(name), 78, 178, 40, "res"));
        Map<String, String> values = Map.ofEntries(
                Map.entry("CATEGORY", categoryLabel(categorySlug, locale)),
                Map.entry("NAME", name),
                Map.entry("PLACE", place == null
                        ? localizer.get(locale, "card.noPlace")
                        : localizer.get(locale, "card.place", place)),
                Map.entry("SCORE", Long.toString(score)),
                Map.entry("ACCURACY", accuracyPercent + "%"),
                Map.entry("CORRECT", Long.toString(correct)),
                Map.entry("LABEL_SCORE", localizer.get(locale, "card.score")),
                Map.entry("LABEL_ACCURACY", localizer.get(locale, "card.accuracy")),
                Map.entry("LABEL_CORRECT", localizer.get(locale, "card.correct")),
                Map.entry("LABEL_FOOTER", localizer.get(locale, "card.quizFinished")));
        return svg.rasterize(svg.fill(template, values));
    }

    private String categoryLabel(String slug, Locale locale) {
        return localizer.get(locale, slug == null || slug.isEmpty() ? "category.any" : "category." + slug);
    }
}
