package com.quizarena.render;

import com.quizarena.config.BrandProperties;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.image.ImageTranscoder;
import org.apache.batik.transcoder.image.PNGTranscoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.font.FontRenderContext;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class SvgCardRenderer {

    private static final Logger log = LoggerFactory.getLogger(SvgCardRenderer.class);

    private final ConcurrentHashMap<String, String> templates = new ConcurrentHashMap<>();
    private final BrandProperties brand;
    private Font baseFont;

    public SvgCardRenderer(BrandProperties brand) {
        this.brand = brand;
        registerFont();
    }

    public String truncateToWidth(String text, float fontSize, double maxWidth) {
        if (text == null) {
            return "";
        }
        if (baseFont == null) {
            return text;
        }
        Font sized = baseFont.deriveFont(fontSize);
        FontRenderContext frc = new FontRenderContext(null, true, true);
        if (sized.getStringBounds(text, frc).getWidth() <= maxWidth) {
            return text;
        }
        for (int len = text.length() - 1; len >= 1; len--) {
            String candidate = text.substring(0, len) + "…";
            if (sized.getStringBounds(candidate, frc).getWidth() <= maxWidth) {
                return candidate;
            }
        }
        return "…";
    }

    public String loadTemplate(String name) {
        return templates.computeIfAbsent(name, this::readTemplate);
    }

    public String fill(String template, Map<String, String> values) {
        String result = template;
        for (Map.Entry<String, String> entry : values.entrySet()) {
            result = result.replace("{{" + entry.getKey() + "}}", xmlEscape(entry.getValue()));
        }
        return result;
    }

    // Telegram re-encodes photos to JPEG and flattens alpha to white, so the rounded-corner area
    // outside the card would show white. Render onto the card colour instead of transparency.
    private static final Color CARD_BACKGROUND = new Color(0x14, 0x18, 0x2e);

    public byte[] rasterize(String svg) {
        PNGTranscoder transcoder = new PNGTranscoder();
        transcoder.addTranscodingHint(ImageTranscoder.KEY_BACKGROUND_COLOR, CARD_BACKGROUND);
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            transcoder.transcode(new TranscoderInput(new StringReader(svg)), new TranscoderOutput(out));
            return out.toByteArray();
        } catch (Exception e) {
            throw new CardRenderException("SVG rasterization failed", e);
        }
    }

    public String initials(String name) {
        if (name == null) {
            return "?";
        }
        StringBuilder sb = new StringBuilder();
        for (String part : name.trim().split("\\s+")) {
            if (part.isEmpty()) {
                continue;
            }
            char first = part.charAt(0);
            if (first == '@' && part.length() > 1) {
                first = part.charAt(1);
            }
            sb.append(Character.toUpperCase(first));
            if (sb.length() == 2) {
                break;
            }
        }
        return sb.isEmpty() ? "?" : sb.toString();
    }

    public String xmlEscape(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }

    private String readTemplate(String name) {
        try (InputStream is = getClass().getResourceAsStream("/cards/" + name)) {
            if (is == null) {
                throw new CardRenderException("Template not found: " + name, null);
            }
            return new String(is.readAllBytes(), StandardCharsets.UTF_8)
                    .replace("{{BRAND}}", xmlEscape(brand.name()))
                    .replace("{{BOT_HANDLE}}", xmlEscape(brand.handle()));
        } catch (CardRenderException e) {
            throw e;
        } catch (Exception e) {
            throw new CardRenderException("Failed to load template: " + name, e);
        }
    }

    private void registerFont() {
        try (InputStream is = getClass().getResourceAsStream("/fonts/DejaVuSans.ttf")) {
            if (is == null) {
                log.warn("DejaVuSans.ttf not found on classpath; Cyrillic in cards may render as boxes");
                return;
            }
            Font font = Font.createFont(Font.TRUETYPE_FONT, is);
            GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(font);
            this.baseFont = font;
        } catch (Exception e) {
            log.warn("Failed to register DejaVu Sans for Batik", e);
        }
    }
}
