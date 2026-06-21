package com.quizarena.render;

import com.quizarena.domain.Matchup;
import com.quizarena.i18n.Localizer;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Map;

@Component
public class DuelMatchupCardRenderer {

    private static final float NAME_FONT = 24f;
    private static final double NAME_MAX_WIDTH = 258;

    private final SvgCardRenderer svg;
    private final Localizer localizer;

    public DuelMatchupCardRenderer(SvgCardRenderer svg, Localizer localizer) {
        this.svg = svg;
        this.localizer = localizer;
    }

    public byte[] render(Matchup matchup, Locale locale) {
        String template = svg.loadTemplate("duel_matchup.svg");
        // Avatars are trusted SVG (base64 image or escaped-initials circle), inserted raw past fill().
        template = template.replace("{{AVATAR_A}}",
                svg.avatarSlot(matchup.avatarA(), svg.initials(matchup.nameA()), 181, 186, 42, "avA"));
        template = template.replace("{{AVATAR_B}}",
                svg.avatarSlot(matchup.avatarB(), svg.initials(matchup.nameB()), 499, 186, 42, "avB"));
        Map<String, String> values = Map.of(
                "NAME_A", svg.truncateToWidth(matchup.nameA(), NAME_FONT, NAME_MAX_WIDTH),
                "ELO_A", Integer.toString(matchup.eloA()),
                "NAME_B", svg.truncateToWidth(matchup.nameB(), NAME_FONT, NAME_MAX_WIDTH),
                "ELO_B", Integer.toString(matchup.eloB()),
                "CATEGORY", categoryLabel(matchup.category(), locale),
                "DIFFICULTY", difficultyLabel(matchup.difficulty(), locale),
                "LABEL_FOOTER", localizer.get(locale, "card.matchupFound"));
        return svg.rasterize(svg.fill(template, values));
    }

    private String categoryLabel(String slug, Locale locale) {
        return localizer.get(locale, slug == null || slug.isEmpty() ? "category.any" : "category." + slug);
    }

    private String difficultyLabel(String value, Locale locale) {
        return switch (value == null ? "" : value) {
            case "easy", "medium", "hard" -> localizer.get(locale, "difficulty." + value);
            default -> localizer.get(locale, "difficulty.any");
        };
    }
}
