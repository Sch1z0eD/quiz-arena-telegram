package com.quizarena.handler;

import com.quizarena.config.BrandProperties;
import com.quizarena.domain.Category;
import com.quizarena.domain.Profile;
import com.quizarena.domain.TopScope;
import com.quizarena.i18n.Localizer;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class UiTexts {

    private final Localizer localizer;
    private final BrandProperties brand;

    public UiTexts(Localizer localizer, BrandProperties brand) {
        this.localizer = localizer;
        this.brand = brand;
    }

    public String menuHome(Locale locale) {
        return localizer.get(locale, "menu.home", brand.name());
    }

    public String groupHint(Locale locale) {
        return localizer.get(locale, "menu.groupHint");
    }

    public String categoriesTitle(Locale locale) {
        return localizer.get(locale, "menu.categoriesTitle");
    }

    public String difficultyTitle(Locale locale, String categoryLabel) {
        return localizer.get(locale, "menu.difficultyTitle", categoryLabel);
    }

    public String starting(Locale locale, String categoryLabel, String difficultyLabel) {
        return localizer.get(locale, "menu.starting", categoryLabel, difficultyLabel);
    }

    public String languageTitle(Locale locale) {
        return localizer.get(locale, "menu.languageTitle");
    }

    public String notEnoughQuestions(Locale locale) {
        return localizer.get(locale, "notEnoughQuestions");
    }

    public String gameAlreadyRunning(Locale locale) {
        return localizer.get(locale, "gameAlreadyRunning");
    }

    public String profile(Locale locale, Profile profile) {
        String place = profile.place() == null ? localizer.get(locale, "rank.none") : Long.toString(profile.place());
        return localizer.get(locale, "profile.text",
                localizer.plural(locale, "noun.games", profile.games()),
                profile.answered(), profile.correct(), profile.accuracyPercent(), profile.points(), place,
                profile.elo());
    }

    public String rules(Locale locale) {
        return localizer.get(locale, "rules.text");
    }

    public String btnPlay(Locale locale) { return localizer.get(locale, "btn.play"); }
    public String btnDuel(Locale locale) { return localizer.get(locale, "btn.duel"); }
    public String btnProfile(Locale locale) { return localizer.get(locale, "btn.profile"); }
    public String btnLeaderboard(Locale locale) { return localizer.get(locale, "btn.leaderboard"); }
    public String btnRules(Locale locale) { return localizer.get(locale, "btn.rules"); }
    public String btnLanguage(Locale locale) { return localizer.get(locale, "btn.language"); }
    public String btnBack(Locale locale) { return localizer.get(locale, "btn.back"); }
    public String btnAnyCategory(Locale locale) { return localizer.get(locale, "btn.anyCategory"); }
    public String btnAnyDifficulty(Locale locale) { return localizer.get(locale, "btn.anyDifficulty"); }
    public String btnCancelSearch(Locale locale) { return localizer.get(locale, "btn.cancelSearch"); }

    public String duelSearching(Locale locale) { return localizer.get(locale, "duel.searching"); }
    public String duelSearchFound(Locale locale) { return localizer.get(locale, "duel.searchFound"); }
    public String duelSearchFailed(Locale locale) { return localizer.get(locale, "duel.searchFailed"); }
    public String duelSearchCancelled(Locale locale) { return localizer.get(locale, "duel.searchCancelled"); }
    public String duelBusy(Locale locale) { return localizer.get(locale, "duel.busy"); }
    public String duelModeTitle(Locale locale) { return localizer.get(locale, "duel.mode.title"); }
    public String btnDuelRandom(Locale locale) { return localizer.get(locale, "btn.duelRandom"); }
    public String btnDuelInvite(Locale locale) { return localizer.get(locale, "btn.duelInvite"); }
    public String btnCancelInvite(Locale locale) { return localizer.get(locale, "btn.cancelInvite"); }
    public String inviteShare(Locale locale, String link) { return localizer.get(locale, "invite.share", link); }

    public String inlineDuelTitle(Locale locale) { return localizer.get(locale, "inline.duel.title"); }
    public String inlineDuelDescription(Locale locale) { return localizer.get(locale, "inline.duel.description"); }
    public String inlineDuelMessage(Locale locale, String name) { return localizer.get(locale, "inline.duel.message", name); }
    public String inlineDuelButton(Locale locale) { return localizer.get(locale, "inline.duel.button"); }
    public String inlinePlayTitle(Locale locale) { return localizer.get(locale, "inline.play.title"); }
    public String inlinePlayDescription(Locale locale) { return localizer.get(locale, "inline.play.description"); }
    public String inlinePlayMessage(Locale locale, String name) { return localizer.get(locale, "inline.play.message", name); }
    public String inlinePlayButton(Locale locale) { return localizer.get(locale, "inline.play.button"); }

    public String categoryLabel(Category category, Locale locale) {
        return localizer.get(locale, "category." + category.slug());
    }

    public String difficultyLabel(String value, Locale locale) {
        return switch (value) {
            case "easy", "medium", "hard" -> localizer.get(locale, "difficulty." + value);
            default -> localizer.get(locale, "btn.anyDifficulty");
        };
    }

    public String scopeLabel(TopScope scope, Locale locale) {
        return localizer.get(locale, "scope." + scope.name());
    }

    public String languageName(String code, Locale locale) {
        return localizer.get(locale, "lang." + code);
    }
}
