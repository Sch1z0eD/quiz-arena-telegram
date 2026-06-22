package com.quizarena.render;

import com.quizarena.config.BrandProperties;
import com.quizarena.domain.Matchup;
import com.quizarena.i18n.Localizer;
import com.quizarena.i18n.Plurals;
import com.quizarena.service.CategoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.support.ResourceBundleMessageSource;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

class DuelMatchupCardRendererTest {

    private static final Locale RU = Locale.of("ru");

    private final SvgCardRenderer svg = new SvgCardRenderer(new BrandProperties("QUIZARENA", "@QuizArenaBot"));
    private final Localizer localizer = new Localizer(messageSource(), new Plurals());
    private final CategoryService categoryService = mock(CategoryService.class);
    private final DuelMatchupCardRenderer renderer = new DuelMatchupCardRenderer(svg, localizer, categoryService);

    @BeforeEach
    void stubCategoryNames() {
        when(categoryService.displayName(anyString(), any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void rendersWithRealAvatarsToPng() throws Exception {
        byte[] avatar = samplePng();
        Matchup m = new Matchup("Иван", 1200, avatar, "Анна & <co>", 1310, avatar, "science", "hard");
        assertPng(renderer.render(m, RU)); // proves Batik renders the base64 <image> + clipPath
    }

    @Test
    void rendersInitialsFallbackWhenNoAvatar() {
        Matchup m = new Matchup("🤝Боб", 1000, null, "@alice", 1000, null, "", "");
        assertPng(renderer.render(m, Locale.of("en"))); // null avatars -> escaped initials, any/any labels
    }

    @Test
    void categoryNameFromDbRendersOnCard() {
        when(categoryService.displayName("science", RU)).thenReturn("Наука-из-БД");
        SvgCardRenderer spy = spy(svg);
        ArgumentCaptor<String> renderedSvg = ArgumentCaptor.forClass(String.class);
        doReturn(new byte[]{(byte) 0x89, (byte) 0x50}).when(spy).rasterize(renderedSvg.capture());
        Matchup m = new Matchup("Иван", 1200, null, "Боб", 1000, null, "science", "hard");
        new DuelMatchupCardRenderer(spy, localizer, categoryService).render(m, RU);
        assertTrue(renderedSvg.getValue().contains("Наука-из-БД"), "matchup SVG should contain the DB category name");
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
    }

    private static ResourceBundleMessageSource messageSource() {
        ResourceBundleMessageSource source = new ResourceBundleMessageSource();
        source.setBasename("messages");
        source.setDefaultEncoding("UTF-8");
        source.setFallbackToSystemLocale(false);
        return source;
    }
}
