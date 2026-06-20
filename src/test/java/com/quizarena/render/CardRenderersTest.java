package com.quizarena.render;

import com.quizarena.config.BrandProperties;
import com.quizarena.domain.DuelResult;
import com.quizarena.domain.PersonalRank;
import com.quizarena.domain.Profile;
import com.quizarena.domain.Standing;
import com.quizarena.domain.TopScope;
import com.quizarena.i18n.Localizer;
import com.quizarena.i18n.Plurals;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.ResourceBundleMessageSource;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CardRenderersTest {

    private static final Locale RU = Locale.of("ru");
    private static final Locale EN = Locale.of("en");

    private final SvgCardRenderer svg = new SvgCardRenderer(new BrandProperties("QUIZARENA", "@QuizArenaBot"));
    private final Localizer localizer = new Localizer(messageSource(), new Plurals());

    @Test
    void rendersResultCard() {
        byte[] png = new ResultCardRenderer(svg, localizer).render("computers", "Иван", 320, 85, 17, 3L, RU);
        assertPng(png);
    }

    @Test
    void cornerIsOpaqueCardColourNotWhite() throws Exception {
        byte[] png = new ResultCardRenderer(svg, localizer).render("computers", "Иван", 320, 85, 17, 3L, RU);
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(png));
        int argb = image.getRGB(1, 1);
        assertEquals(0xFF, (argb >>> 24) & 0xFF, "corner must be opaque");
        assertEquals(0x14182E, argb & 0xFFFFFF, "corner must be card background, not white");
    }

    @Test
    void rendersProfileCard() {
        byte[] png = new ProfileCardRenderer(svg, localizer)
                .render(new Profile(42, 200, 150, 75, 1234, 7L, 1024), "Анна", RU);
        assertPng(png);
    }

    @Test
    void rendersLeaderboardWithDynamicRows() {
        List<Standing> top = List.of(
                new Standing("Аня", 500),
                new Standing("Боря & <co>", 420),
                new Standing("Витя", 300));
        byte[] png = new LeaderboardCardRenderer(svg, localizer).render(TopScope.WEEK, top, RU);
        assertPng(png);
    }

    @Test
    void rendersDuelCardWithLongNameTruncated() {
        DuelResult result = new DuelResult("science",
                "Александр Длинноеимякотороеточноневлезетвпанель", 320,
                "Боб", 150, DuelResult.Outcome.A_WINS, 1012, 12, 988, -12);
        byte[] png = new DuelResultCardRenderer(svg, localizer).render(result, RU);
        assertPng(png);
    }

    @Test
    void rendersEmptyLeaderboard() {
        byte[] png = new LeaderboardCardRenderer(svg, localizer).render(TopScope.GLOBAL, List.of(), RU);
        assertPng(png);
    }

    @Test
    void rendersBannersForBothLocales() {
        BannerRenderer banners = new BannerRenderer(svg, localizer);
        for (Locale locale : new Locale[]{RU, EN}) {
            assertPng(banners.render("welcome_banner.svg", locale));
            assertPng(banners.render("game_banner.svg", locale));
            assertPng(banners.render("duel_banner.svg", locale));
        }
    }

    @Test
    void rendersRankCard() {
        byte[] png = new RankCardRenderer(svg, localizer)
                .render("Иван", new PersonalRank(800, 3), new PersonalRank(1234, 42), RU);
        assertPng(png);
    }

    @Test
    void rendersRankCardForNewcomer() {
        byte[] png = new RankCardRenderer(svg, localizer).render("Bob", null, null, EN);
        assertPng(png);
    }

    private static ResourceBundleMessageSource messageSource() {
        ResourceBundleMessageSource source = new ResourceBundleMessageSource();
        source.setBasename("messages");
        source.setDefaultEncoding("UTF-8");
        return source;
    }

    private static void assertPng(byte[] png) {
        assertTrue(png.length > 1000, "PNG too small: " + png.length);
        assertEquals((byte) 0x89, png[0]);
        assertEquals((byte) 0x50, png[1]);
    }
}
