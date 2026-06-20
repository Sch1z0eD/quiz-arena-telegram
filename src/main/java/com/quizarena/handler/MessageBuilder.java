package com.quizarena.handler;

import com.quizarena.domain.DuelResult;
import com.quizarena.domain.JoinResult;
import com.quizarena.domain.PersonalRank;
import com.quizarena.domain.Question;
import com.quizarena.domain.RecordResult;
import com.quizarena.domain.Standing;
import com.quizarena.domain.TopScope;
import com.quizarena.i18n.Localizer;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;

import java.util.List;
import java.util.Locale;
import java.util.function.IntFunction;

@Component
public class MessageBuilder {

    private static final char[] LABELS = {'A', 'B', 'C', 'D'};
    private static final int OPTION_COUNT = 4;
    private static final int SHORT_OPTION_MAX = 18;

    private final Localizer localizer;

    public MessageBuilder(Localizer localizer) {
        this.localizer = localizer;
    }

    public String lobbyText(Locale locale, int participants, int seconds) {
        return localizer.get(locale, "lobby.text", seconds, participants);
    }

    public InlineKeyboardMarkup joinKeyboard(Locale locale) {
        return InlineKeyboardMarkup.builder()
                .keyboardRow(new InlineKeyboardRow(InlineKeyboardButton.builder()
                        .text(localizer.get(locale, "lobby.joinButton"))
                        .callbackData("join")
                        .build()))
                .build();
    }

    public String lobbyCancelledText(Locale locale) {
        return localizer.get(locale, "lobby.cancelled");
    }

    public String lobbyStartedText(Locale locale, int participants) {
        return localizer.get(locale, "lobby.started", participants);
    }

    public String questionText(Locale locale, Question question, int index, int total) {
        return localizer.get(locale, "question.title", index + 1, total, escape(question.getText()));
    }

    public InlineKeyboardMarkup answerKeyboard(Question question, long token) {
        return answerKeyboard(question, option -> "a:" + token + ":" + option);
    }

    public InlineKeyboardMarkup duelAnswerKeyboard(Question question, long duelId, long token) {
        return answerKeyboard(question, option -> "d:" + duelId + ":" + token + ":" + option);
    }

    private InlineKeyboardMarkup answerKeyboard(Question question, IntFunction<String> callback) {
        if (allOptionsShort(question)) {
            return InlineKeyboardMarkup.builder()
                    .keyboardRow(new InlineKeyboardRow(button(question, callback, 0), button(question, callback, 1)))
                    .keyboardRow(new InlineKeyboardRow(button(question, callback, 2), button(question, callback, 3)))
                    .build();
        }
        return InlineKeyboardMarkup.builder()
                .keyboardRow(new InlineKeyboardRow(button(question, callback, 0)))
                .keyboardRow(new InlineKeyboardRow(button(question, callback, 1)))
                .keyboardRow(new InlineKeyboardRow(button(question, callback, 2)))
                .keyboardRow(new InlineKeyboardRow(button(question, callback, 3)))
                .build();
    }

    public String revealText(Locale locale, Question question) {
        StringBuilder sb = new StringBuilder(escape(question.getText())).append("\n\n");
        for (int i = 0; i < OPTION_COUNT; i++) {
            sb.append(LABELS[i]).append(". ").append(escape(question.getOptionByIndex(i)));
            if (i == question.getCorrectOption()) {
                sb.append(localizer.get(locale, "reveal.correctMark"));
            }
            sb.append("\n");
        }
        return sb.append("\n<b>").append(localizer.get(locale, "reveal.answer")).append(": ")
                .append(LABELS[question.getCorrectOption()]).append("</b>").toString();
    }

    public String scoreboardText(Locale locale, List<Standing> standings) {
        String title = "<b>" + localizer.get(locale, "scoreboard.title") + "</b>";
        if (standings.isEmpty()) {
            return title + "\n" + localizer.get(locale, "scoreboard.empty");
        }
        StringBuilder sb = new StringBuilder(title);
        int place = 1;
        for (Standing standing : standings) {
            sb.append("\n").append(place++).append(". ").append(escape(standing.name())).append(" — ").append(standing.score());
        }
        return sb.toString();
    }

    public String topText(Locale locale, TopScope scope, List<Standing> top, PersonalRank personal) {
        StringBuilder sb = new StringBuilder("<b>").append(localizer.get(locale, "top.title." + scope.name())).append("</b>");
        if (top.isEmpty()) {
            sb.append("\n").append(localizer.get(locale, "top.empty"));
        } else {
            int place = 1;
            for (Standing standing : top) {
                sb.append("\n").append(place++).append(". ").append(escape(standing.name())).append(" — ").append(standing.score());
            }
        }
        sb.append("\n\n");
        sb.append(personal == null
                ? localizer.get(locale, "top.notRanked")
                : localizer.get(locale, "top.yourPlace", personal.place(), localizer.plural(locale, "noun.points", personal.score())));
        return sb.toString();
    }

    public String rankText(Locale locale, PersonalRank group, PersonalRank global, int elo) {
        return "<b>" + localizer.get(locale, "rank.title") + "</b>\n"
                + localizer.get(locale, "rank.inChat", formatRank(locale, group)) + "\n"
                + localizer.get(locale, "rank.global", formatRank(locale, global)) + "\n"
                + eloCaption(locale, elo);
    }

    public String duelResultText(Locale locale, DuelResult result) {
        String summary = switch (result.outcome()) {
            case A_WINS -> localizer.get(locale, "duel.winner", escape(result.nameA()));
            case B_WINS -> localizer.get(locale, "duel.winner", escape(result.nameB()));
            case DRAW -> localizer.get(locale, "duel.draw");
        };
        return localizer.get(locale, "duel.resultText",
                escape(result.nameA()), result.scoreA(), escape(result.nameB()), result.scoreB(), summary)
                + "\n\n" + duelEloCaption(locale, result);
    }

    public String duelEloCaption(Locale locale, DuelResult result) {
        return localizer.get(locale, "duel.eloChange",
                escape(result.nameA()), Integer.toString(result.eloA()), signed(result.eloDeltaA()),
                escape(result.nameB()), Integer.toString(result.eloB()), signed(result.eloDeltaB()));
    }

    public String eloCaption(Locale locale, int elo) {
        return localizer.get(locale, "elo.caption", Integer.toString(elo));
    }

    private static String signed(int delta) {
        return delta > 0 ? "+" + delta : Integer.toString(delta);
    }

    public String answerToast(Locale locale, RecordResult result) {
        return switch (result.status()) {
            case NO_GAME -> localizer.get(locale, "toast.noGame");
            case NOT_PARTICIPANT -> localizer.get(locale, "toast.notParticipant");
            case LATE -> localizer.get(locale, "toast.late");
            case DUPLICATE -> localizer.get(locale, "toast.duplicate");
            case ANSWERED -> result.correct()
                    ? localizer.get(locale, "toast.correct", result.points())
                    : localizer.get(locale, "toast.wrong");
        };
    }

    public String joinToast(Locale locale, JoinResult result) {
        return switch (result) {
            case JOINED -> localizer.get(locale, "join.joined");
            case ALREADY -> localizer.get(locale, "join.already");
            case CLOSED -> localizer.get(locale, "join.closed");
        };
    }

    public String errorGeneric(Locale locale) {
        return localizer.get(locale, "error.generic");
    }

    private String formatRank(Locale locale, PersonalRank rank) {
        return rank == null
                ? localizer.get(locale, "rank.none")
                : localizer.get(locale, "rank.value", localizer.plural(locale, "noun.points", rank.score()), rank.place());
    }

    private boolean allOptionsShort(Question question) {
        for (int i = 0; i < OPTION_COUNT; i++) {
            if (question.getOptionByIndex(i).length() > SHORT_OPTION_MAX) {
                return false;
            }
        }
        return true;
    }

    private InlineKeyboardButton button(Question question, IntFunction<String> callback, int optionIndex) {
        return InlineKeyboardButton.builder()
                .text(question.getOptionByIndex(optionIndex))
                .callbackData(callback.apply(optionIndex))
                .build();
    }

    private static String escape(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
