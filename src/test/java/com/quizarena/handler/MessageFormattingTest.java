package com.quizarena.handler;

import com.quizarena.config.BrandProperties;
import com.quizarena.domain.OptionOrder;
import com.quizarena.domain.Question;
import com.quizarena.domain.Standing;
import com.quizarena.i18n.Localizer;
import com.quizarena.i18n.Plurals;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.ResourceBundleMessageSource;

import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MessageFormattingTest {

    private static final Locale EN = Locale.of("en");
    private static final Locale RU = Locale.of("ru");

    private final Localizer localizer = localizer();
    private final MessageBuilder builder = new MessageBuilder(localizer);
    private final UiTexts texts = new UiTexts(localizer, new BrandProperties("Q&A <Bot>", "@bot"));

    @Test
    void questionTextEscapesSpecialsAndWrapsBlockquote() {
        Question q = question("1 < 2 & 3 > 0? It's a trap", "a < b", "c & d", "e > f", "ok");
        String text = builder.questionText(EN, q, 0, 5);
        assertTrue(text.contains("<blockquote>"), text);
        assertTrue(text.contains("</blockquote>"), text);
        assertTrue(text.contains("1 &lt; 2 &amp; 3 &gt; 0? It's a trap"), text);
        assertEquals(1, count(text, "<blockquote"), "single, non-nested blockquote");
        assertFalse(insideBlockquote(text).contains("<"), "no raw < inside the question blockquote");
    }

    @Test
    void revealTextEscapesAndBlockquotesQuestionWithoutNesting() {
        Question q = question("a < b & c", "x < y", "z & w", "p > q", "ok");
        String text = builder.revealText(EN, q, OptionOrder.identity());
        assertTrue(text.startsWith("<blockquote>"), text);
        assertTrue(text.contains("a &lt; b &amp; c"), text);
        assertTrue(text.contains("x &lt; y"), "option text inside reveal is escaped");
        assertEquals(1, count(text, "<blockquote"), "exactly one blockquote, options stay outside");
        assertTrue(text.indexOf("</blockquote>") < text.indexOf("x &lt; y"), "blockquote closes before options");
    }

    @Test
    void revealRendersOptionsInDisplayOrderAndMarksCorrectAtItsSlot() {
        // correct option is storage index 0 ("CORRECT"); order shows storage 1 at slot A and storage 0 at slot B
        Question q = question("Q?", "CORRECT", "second", "third", "fourth");
        String text = builder.revealText(EN, q, OptionOrder.parse("1,0,2,3"));

        assertTrue(text.contains("A. second"), "slot A must show the storage-1 option, " + text);
        assertTrue(text.contains("B. CORRECT"), "the correct option moves to slot B under this order, " + text);
        assertTrue(text.trim().endsWith("B</b>"), "answer footer must name the correct option's display slot, " + text);
    }

    @Test
    void scoreboardEscapesPlayerNames() {
        String text = builder.scoreboardText(EN, List.of(new Standing("<b>Hax</b> & Co", 100)));
        assertTrue(text.contains("&lt;b&gt;Hax&lt;/b&gt; &amp; Co"), text);
        assertFalse(text.contains("<b>Hax"), "raw markup from a player name must not leak");
    }

    @Test
    void menuHomeEscapesBrand() {
        assertTrue(texts.menuHome(EN).contains("Q&amp;A &lt;Bot&gt;"), texts.menuHome(EN));
    }

    @Test
    void difficultyTitleEscapesCategoryLabel() {
        assertTrue(texts.difficultyTitle(EN, "Arts & Culture").contains("Arts &amp; Culture"));
    }

    @Test
    void rulesWrappedInExpandableBlockquoteWithoutRawSpecials() {
        for (Locale locale : new Locale[]{EN, RU}) {
            String rules = texts.rules(locale);
            assertTrue(rules.startsWith("<blockquote expandable>"), rules);
            assertTrue(rules.endsWith("</blockquote>"), rules);
            assertEquals(1, count(rules, "<blockquote"), "single, non-nested blockquote");
            String inner = rules.substring("<blockquote expandable>".length(), rules.length() - "</blockquote>".length());
            assertFalse(inner.contains("<"), "no raw < inside rules");
            assertFalse(inner.contains(">"), "no raw > inside rules");
            assertFalse(inner.contains("&"), "no raw & inside rules");
        }
    }

    private static Question question(String text, String a, String b, String c, String d) {
        return new Question(text, a, b, c, d, 0, "general", "easy", "en", "hash");
    }

    private static int count(String haystack, String needle) {
        int total = 0;
        for (int i = haystack.indexOf(needle); i >= 0; i = haystack.indexOf(needle, i + 1)) {
            total++;
        }
        return total;
    }

    private static String insideBlockquote(String text) {
        return text.substring(text.indexOf("<blockquote>") + "<blockquote>".length(), text.indexOf("</blockquote>"));
    }

    private static Localizer localizer() {
        ResourceBundleMessageSource source = new ResourceBundleMessageSource();
        source.setBasename("messages");
        source.setDefaultEncoding("UTF-8");
        source.setFallbackToSystemLocale(false);
        return new Localizer(source, new Plurals());
    }
}
