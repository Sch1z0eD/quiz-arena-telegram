package com.quizarena.render;

import com.quizarena.domain.PersonalRank;
import com.quizarena.i18n.Localizer;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Map;

@Component
public class RankCardRenderer {

    private final SvgCardRenderer svg;
    private final Localizer localizer;

    public RankCardRenderer(SvgCardRenderer svg, Localizer localizer) {
        this.svg = svg;
        this.localizer = localizer;
    }

    public byte[] render(String name, PersonalRank scoped, PersonalRank global, byte[] avatar, boolean privateChat,
                         Locale locale) {
        String template = svg.loadTemplate("rank_card.svg")
                .replace("{{AVATAR}}", svg.avatarSlot(avatar, svg.initials(name), 78, 150, 40, "rank"));
        Map<String, String> values = Map.ofEntries(
                Map.entry("NAME", name),
                Map.entry("RANKLINE", rankline(scoped, privateChat, locale)),
                Map.entry("RANK", global == null
                        ? localizer.get(locale, "card.rankNone")
                        : Long.toString(global.place())),
                Map.entry("POINTS", global == null ? "0" : Long.toString(global.score())),
                Map.entry("LABEL_RANK_TITLE", localizer.get(locale, "card.rankTitle")),
                Map.entry("LABEL_PLACE", localizer.get(locale, "card.globalPlace")),
                Map.entry("LABEL_POINTS", localizer.get(locale, "card.totalPoints")));
        return svg.rasterize(svg.fill(template, values));
    }

    private String rankline(PersonalRank scoped, boolean privateChat, Locale locale) {
        if (privateChat) {
            return scoped == null
                    ? localizer.get(locale, "card.ranklineWeekNone")
                    : localizer.get(locale, "card.rankThisWeek", scoped.place());
        }
        return scoped == null
                ? localizer.get(locale, "card.ranklineNew")
                : localizer.get(locale, "card.rankInChat", scoped.place());
    }
}
