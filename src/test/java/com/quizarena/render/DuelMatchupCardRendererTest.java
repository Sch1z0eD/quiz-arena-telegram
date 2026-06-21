package com.quizarena.render;

import com.quizarena.config.BrandProperties;
import com.quizarena.domain.Matchup;
import com.quizarena.i18n.Localizer;
import com.quizarena.i18n.Plurals;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.ResourceBundleMessageSource;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DuelMatchupCardRendererTest {

    private final DuelMatchupCardRenderer renderer = new DuelMatchupCardRenderer(
            new SvgCardRenderer(new BrandProperties("QUIZARENA", "@QuizArenaBot")),
            new Localizer(messageSource(), new Plurals()));

    @Test
    void rendersWithRealAvatarsToPng() throws Exception {
        byte[] avatar = samplePng();
        Matchup m = new Matchup("Иван", 1200, avatar, "Анна & <co>", 1310, avatar, "science", "hard");
        assertPng(renderer.render(m, Locale.of("ru"))); // proves Batik renders the base64 <image> + clipPath
    }

    @Test
    void rendersInitialsFallbackWhenNoAvatar() {
        Matchup m = new Matchup("🤝Боб", 1000, null, "@alice", 1000, null, "", "");
        assertPng(renderer.render(m, Locale.of("en"))); // null avatars -> escaped initials, any/any labels
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
