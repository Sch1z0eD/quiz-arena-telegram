package com.quizarena.render;

import com.quizarena.domain.Profile;
import com.quizarena.i18n.Localizer;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Map;

@Component
public class ProfileCardRenderer {

    private final SvgCardRenderer svg;
    private final Localizer localizer;

    public ProfileCardRenderer(SvgCardRenderer svg, Localizer localizer) {
        this.svg = svg;
        this.localizer = localizer;
    }

    public byte[] render(Profile profile, String name, byte[] avatar, Locale locale) {
        String template = svg.loadTemplate("profile_card.svg")
                .replace("{{AVATAR}}", svg.avatarSlot(avatar, svg.initials(name), 78, 150, 40, "prof"));
        Map<String, String> values = Map.ofEntries(
                Map.entry("NAME", name),
                Map.entry("RANKLINE", profile.place() == null
                        ? localizer.get(locale, "card.ranklineNew")
                        : localizer.get(locale, "card.rankline", profile.place())),
                Map.entry("GAMES", Long.toString(profile.games())),
                Map.entry("POINTS", Long.toString(profile.points())),
                Map.entry("ACCURACY", profile.accuracyPercent() + "%"),
                Map.entry("RANK", profile.place() == null
                        ? localizer.get(locale, "card.rankNone")
                        : Long.toString(profile.place())),
                Map.entry("LABEL_TITLE", localizer.get(locale, "card.profile")),
                Map.entry("LABEL_GAMES", localizer.get(locale, "card.gamesPlayed")),
                Map.entry("LABEL_POINTS", localizer.get(locale, "card.totalPoints")),
                Map.entry("LABEL_ACCURACY", localizer.get(locale, "card.accuracy")),
                Map.entry("LABEL_RANK", localizer.get(locale, "card.rankPlace")));
        return svg.rasterize(svg.fill(template, values));
    }
}
