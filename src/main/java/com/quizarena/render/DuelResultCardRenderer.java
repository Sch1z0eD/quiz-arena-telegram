package com.quizarena.render;

import com.quizarena.domain.DuelResult;
import com.quizarena.i18n.Localizer;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Map;

@Component
public class DuelResultCardRenderer {

    private static final float NAME_FONT = 24f;
    private static final double NAME_MAX_WIDTH = 258;
    private static final String WINNER_COLOR = "#13d6c0";
    private static final String OTHER_COLOR = "#8b91b8";

    private final SvgCardRenderer svg;
    private final Localizer localizer;

    public DuelResultCardRenderer(SvgCardRenderer svg, Localizer localizer) {
        this.svg = svg;
        this.localizer = localizer;
    }

    public byte[] render(DuelResult result, Locale locale) {
        boolean aWins = result.outcome() == DuelResult.Outcome.A_WINS;
        boolean bWins = result.outcome() == DuelResult.Outcome.B_WINS;
        Map<String, String> values = Map.ofEntries(
                Map.entry("CATEGORY", categoryLabel(result.categorySlug(), locale)),
                Map.entry("INITIALS_A", svg.initials(result.nameA())),
                Map.entry("NAME_A", svg.truncateToWidth(result.nameA(), NAME_FONT, NAME_MAX_WIDTH)),
                Map.entry("SCORE_A", Long.toString(result.scoreA())),
                Map.entry("RESULT_A", label(result.outcome(), true, locale)),
                Map.entry("RESULT_A_COLOR", aWins ? WINNER_COLOR : OTHER_COLOR),
                Map.entry("INITIALS_B", svg.initials(result.nameB())),
                Map.entry("NAME_B", svg.truncateToWidth(result.nameB(), NAME_FONT, NAME_MAX_WIDTH)),
                Map.entry("SCORE_B", Long.toString(result.scoreB())),
                Map.entry("RESULT_B", label(result.outcome(), false, locale)),
                Map.entry("RESULT_B_COLOR", bWins ? WINNER_COLOR : OTHER_COLOR),
                Map.entry("LABEL_FOOTER", localizer.get(locale, "card.duelFinished")));
        return svg.rasterize(svg.fill(svg.loadTemplate("duel_result.svg"), values));
    }

    private String label(DuelResult.Outcome outcome, boolean playerA, Locale locale) {
        if (outcome == DuelResult.Outcome.DRAW) {
            return localizer.get(locale, "card.result.draw");
        }
        boolean won = playerA ? outcome == DuelResult.Outcome.A_WINS : outcome == DuelResult.Outcome.B_WINS;
        return localizer.get(locale, won ? "card.result.win" : "card.result.lose");
    }

    private String categoryLabel(String slug, Locale locale) {
        return localizer.get(locale, slug == null || slug.isEmpty() ? "category.any" : "category." + slug);
    }
}
