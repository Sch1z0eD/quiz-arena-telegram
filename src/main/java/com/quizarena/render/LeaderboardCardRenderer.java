package com.quizarena.render;

import com.quizarena.domain.Standing;
import com.quizarena.domain.TopScope;
import com.quizarena.i18n.Localizer;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
public class LeaderboardCardRenderer {

    private static final int ROW_TOP = 120;
    private static final int ROW_HEIGHT = 72;
    private static final int FOOTER_SPACE = 44;

    private final SvgCardRenderer svg;
    private final Localizer localizer;

    public LeaderboardCardRenderer(SvgCardRenderer svg, Localizer localizer) {
        this.svg = svg;
        this.localizer = localizer;
    }

    public byte[] render(TopScope scope, List<Standing> top, Locale locale) {
        int count = top.size();
        int height = ROW_TOP + count * ROW_HEIGHT + FOOTER_SPACE;
        String rowTemplate = svg.loadTemplate("leaderboard_row.svg");
        StringBuilder rows = new StringBuilder();
        for (int i = 0; i < count; i++) {
            int rank = i + 1;
            int rectY = ROW_TOP + i * ROW_HEIGHT;
            rows.append(svg.fill(rowTemplate, Map.of(
                    "RECT_Y", Integer.toString(rectY),
                    "TEXT_Y", Integer.toString(rectY + 38),
                    "RANK", Integer.toString(rank),
                    "NAME", top.get(i).name(),
                    "SCORE", Long.toString(top.get(i).score()),
                    "RANK_COLOR", rankColor(rank),
                    "SCORE_COLOR", rank == 1 ? "#13d6c0" : "#ffffff")));
        }
        String card = svg.loadTemplate("leaderboard_card.svg")
                .replace("{{SCOPE}}", svg.xmlEscape(localizer.get(locale, "scope." + scope.name())))
                .replace("{{LABEL_TITLE}}", svg.xmlEscape(localizer.get(locale, "card.leaderboard")))
                .replace("{{HEIGHT}}", Integer.toString(height))
                .replace("{{INNER_HEIGHT}}", Integer.toString(height - 8))
                .replace("{{FOOTER_Y}}", Integer.toString(height - 22))
                .replace("{{ROWS}}", rows.toString());
        return svg.rasterize(card);
    }

    private static String rankColor(int rank) {
        if (rank == 1) {
            return "#13d6c0";
        }
        if (rank <= 3) {
            return "#aeb4d6";
        }
        return "#8b91b8";
    }
}
