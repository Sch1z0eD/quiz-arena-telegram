package com.quizarena.render;

import com.quizarena.config.BrandProperties;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SvgCardRendererTest {

    private static final BrandProperties BRAND = new BrandProperties("QUIZARENA", "@QuizArenaBot");

    @Test
    void rendersResultCardToPng() {
        SvgCardRenderer renderer = new SvgCardRenderer(BRAND);
        String svg = renderer.fill(renderer.loadTemplate("result_card.svg"), Map.of(
                "CATEGORY", "Наука",
                "INITIALS", "ИП",
                "NAME", "Иван <Петров> & Co",
                "PLACE", "1 место",
                "SCORE", "320",
                "ACCURACY", "85%",
                "CORRECT", "17"));
        assertPng(renderer.rasterize(svg));
    }

    @Test
    void escapesXmlSpecialChars() {
        SvgCardRenderer renderer = new SvgCardRenderer(BRAND);
        assertEquals("&lt;b&gt;&amp;&quot;&apos;", renderer.xmlEscape("<b>&\"'"));
    }

    @Test
    void rendersNameWithEmojiWithoutCrashing() {
        SvgCardRenderer renderer = new SvgCardRenderer(BRAND);
        String name = "🤝Боб"; // a handshake emoji + Cyrillic, as seen live
        String svg = renderer.fill(renderer.loadTemplate("result_card.svg"), Map.of(
                "CATEGORY", "Наука",
                "INITIALS", renderer.initials(name),
                "NAME", renderer.truncateToWidth(name, 24f, 258),
                "PLACE", "1 место",
                "SCORE", "320",
                "ACCURACY", "85%",
                "CORRECT", "17"));
        assertPng(renderer.rasterize(svg)); // a lone surrogate in the initials crashed Batik before the fix
    }

    @Test
    void initialsSkipEmojiAtAndPunctuation() {
        SvgCardRenderer renderer = new SvgCardRenderer(BRAND);
        assertEquals("Б", renderer.initials("🤝Боб"));
        assertEquals("ИП", renderer.initials("Иван Петров"));
        assertEquals("B", renderer.initials("@bob"));
        assertEquals("?", renderer.initials("🤝🤝"));
    }

    @Test
    void xmlEscapeDropsLoneSurrogatesKeepsValidPairs() {
        SvgCardRenderer renderer = new SvgCardRenderer(BRAND);
        assertEquals("ab", renderer.xmlEscape("a\uD83Eb"));                   // lone high surrogate dropped
        assertEquals("a🤝b", renderer.xmlEscape("a🤝b")); // valid emoji pair kept
    }

    private static void assertPng(byte[] png) {
        assertTrue(png.length > 1000, "PNG too small: " + png.length);
        assertEquals((byte) 0x89, png[0]);
        assertEquals((byte) 0x50, png[1]);
        assertEquals((byte) 0x4E, png[2]);
        assertEquals((byte) 0x47, png[3]);
    }
}
