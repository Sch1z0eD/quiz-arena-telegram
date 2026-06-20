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

    private static void assertPng(byte[] png) {
        assertTrue(png.length > 1000, "PNG too small: " + png.length);
        assertEquals((byte) 0x89, png[0]);
        assertEquals((byte) 0x50, png[1]);
        assertEquals((byte) 0x4E, png[2]);
        assertEquals((byte) 0x47, png[3]);
    }
}
