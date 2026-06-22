package com.quizarena.render;

import com.quizarena.config.BrandProperties;
import com.quizarena.domain.DuelResult;
import com.quizarena.domain.PersonalRank;
import com.quizarena.domain.Profile;
import com.quizarena.domain.Standing;
import com.quizarena.domain.TopScope;
import com.quizarena.i18n.Localizer;
import com.quizarena.i18n.Plurals;
import com.quizarena.service.CategoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.support.ResourceBundleMessageSource;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

class CardRenderersTest {

    private static final Locale RU = Locale.of("ru");
    private static final Locale EN = Locale.of("en");

    private final SvgCardRenderer svg = new SvgCardRenderer(new BrandProperties("QUIZARENA", "@QuizArenaBot"));
    private final Localizer localizer = new Localizer(messageSource(), new Plurals());
    private final CategoryService categoryService = mock(CategoryService.class);

    @BeforeEach
    void stubCategoryNames() {
        when(categoryService.displayName(anyString(), any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void rendersResultCard() {
        byte[] png = new ResultCardRenderer(svg, localizer, categoryService)
                .render("computers", "Иван", 320, 85, 17, 3L, null, RU);
        assertPng(png);
    }

    @Test
    void cornerIsOpaqueCardColourNotWhite() throws Exception {
        byte[] png = new ResultCardRenderer(svg, localizer, categoryService)
                .render("computers", "Иван", 320, 85, 17, 3L, null, RU);
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(png));
        int argb = image.getRGB(1, 1);
        assertEquals(0xFF, (argb >>> 24) & 0xFF, "corner must be opaque");
        assertEquals(0x14182E, argb & 0xFFFFFF, "corner must be card background, not white");
    }

    @Test
    void rendersProfileCard() throws Exception {
        byte[] png = new ProfileCardRenderer(svg, localizer)
                .render(new Profile(42, 200, 150, 75, 1234, 7L, 1024), "Анна", samplePng(), RU);
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
                "Боб", 150, DuelResult.Outcome.A_WINS, 1012, 12, 988, -12, null, null);
        byte[] png = new DuelResultCardRenderer(svg, localizer, categoryService).render(result, RU);
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
            assertPng(banners.render("rules_banner.svg", locale));
        }
    }

    @Test
    void rendersRankCard() throws Exception {
        byte[] png = new RankCardRenderer(svg, localizer)
                .render("Иван", new PersonalRank(800, 3), new PersonalRank(1234, 42), samplePng(), false, RU);
        assertPng(png);
    }

    @Test
    void rendersRankCardWeeklyInPrivate() {
        byte[] png = new RankCardRenderer(svg, localizer)
                .render("Иван", new PersonalRank(120, 5), new PersonalRank(1234, 42), null, true, EN);
        assertPng(png);
    }

    @Test
    void rendersRankCardForNewcomer() {
        byte[] png = new RankCardRenderer(svg, localizer).render("Bob", null, null, null, false, EN);
        assertPng(png);
    }

    @Test
    void rendersDuelCardWithAvatars() throws Exception {
        byte[] avatar = samplePng();
        DuelResult result = new DuelResult("science", "Иван", 320, "Боб", 150,
                DuelResult.Outcome.A_WINS, 1012, 12, 988, -12, avatar, avatar);
        assertPng(new DuelResultCardRenderer(svg, localizer, categoryService).render(result, RU));
    }

    @Test
    void rendersResultCardWithAvatar() throws Exception {
        byte[] png = new ResultCardRenderer(svg, localizer, categoryService)
                .render("computers", "Иван", 320, 85, 17, 3L, samplePng(), RU);
        assertPng(png);
    }

    @Test
    void categoryNameFromDbRendersOnResultCard() {
        when(categoryService.displayName("computers", RU)).thenReturn("Компьютеры-из-БД");
        assertSvgContains(
                spy -> new ResultCardRenderer(spy, localizer, categoryService)
                        .render("computers", "Иван", 320, 85, 17, 3L, null, RU),
                "Компьютеры-из-БД");
    }

    @Test
    void categoryNameFromDbRendersOnDuelResultCard() {
        when(categoryService.displayName("science", RU)).thenReturn("Наука-из-БД");
        DuelResult result = new DuelResult("science", "Иван", 320, "Боб", 150,
                DuelResult.Outcome.A_WINS, 1012, 12, 988, -12, null, null);
        assertSvgContains(
                spy -> new DuelResultCardRenderer(spy, localizer, categoryService).render(result, RU),
                "Наука-из-БД");
    }

    // Render through a spy that captures the filled SVG (skipping rasterization) and assert the text is present.
    private void assertSvgContains(Consumer<SvgCardRenderer> render, String expected) {
        SvgCardRenderer spy = spy(svg);
        ArgumentCaptor<String> renderedSvg = ArgumentCaptor.forClass(String.class);
        doReturn(new byte[]{(byte) 0x89, (byte) 0x50}).when(spy).rasterize(renderedSvg.capture());
        render.accept(spy);
        assertTrue(renderedSvg.getValue().contains(expected), "SVG should contain the DB category name");
    }

    private static ResourceBundleMessageSource messageSource() {
        ResourceBundleMessageSource source = new ResourceBundleMessageSource();
        source.setBasename("messages");
        source.setDefaultEncoding("UTF-8");
        return source;
    }

    private static byte[] samplePng() throws Exception {
        BufferedImage img = new BufferedImage(96, 96, BufferedImage.TYPE_INT_ARGB);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(img, "png", out);
        return out.toByteArray();
    }

    private static void assertPng(byte[] png) {
        assertTrue(png.length > 1000, "PNG too small: " + png.length);
        assertEquals((byte) 0x89, png[0]);
        assertEquals((byte) 0x50, png[1]);
    }
}
