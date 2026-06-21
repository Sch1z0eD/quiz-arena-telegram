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
import java.util.Base64;
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
            if (Character.isHighSurrogate(text.charAt(len - 1))) {
                continue; // never cut between a surrogate pair (would orphan a high surrogate)
            }
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
        int taken = 0;
        for (String part : name.trim().split("\\s+")) {
            int cp = firstLetterOrDigit(part);
            if (cp < 0) {
                continue;
            }
            sb.appendCodePoint(Character.toUpperCase(cp));
            if (++taken == 2) {
                break;
            }
        }
        return sb.isEmpty() ? "?" : sb.toString();
    }

    // First letter/digit as a whole code point, skipping '@', emoji and punctuation. charAt(0) would
    // split a surrogate pair and return a lone surrogate - the exact character that crashed Batik.
    private static int firstLetterOrDigit(String part) {
        int i = 0;
        while (i < part.length()) {
            int cp = part.codePointAt(i);
            if (Character.isLetterOrDigit(cp)) {
                return cp;
            }
            i += Character.charCount(cp);
        }
        return -1;
    }

    // Avatar slot for a circular cx/cy/r position with a matching clipPath in the template's defs:
    // a clipped <image> with the photo as a base64 data URI, or the existing initials circle when null.
    // Initials go through the same XML-escape as all other text (the fragment is inserted raw, past fill()).
    public String avatarSlot(byte[] avatarPng, String initials, int cx, int cy, int r, String clipId) {
        if (avatarPng != null) {
            String dataUri = "data:image/png;base64," + Base64.getEncoder().encodeToString(avatarPng);
            return "<image x=\"" + (cx - r) + "\" y=\"" + (cy - r) + "\" width=\"" + (2 * r)
                    + "\" height=\"" + (2 * r) + "\" preserveAspectRatio=\"xMidYMid slice\" clip-path=\"url(#"
                    + clipId + ")\" xlink:href=\"" + dataUri + "\"/>";
        }
        int fontSize = Math.round(r * 0.8f);
        int textY = cy + Math.round(r / 3.0f);
        return "<circle cx=\"" + cx + "\" cy=\"" + cy + "\" r=\"" + r + "\" fill=\"#6c5ce7\"/>"
                + "<text x=\"" + cx + "\" y=\"" + textY + "\" fill=\"#14182e\" font-size=\"" + fontSize
                + "\" font-weight=\"700\" text-anchor=\"middle\">" + xmlEscape(initials) + "</text>";
    }

    public String xmlEscape(String value) {
        if (value == null) {
            return "";
        }
        return stripInvalidXml(value)
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }

    // Telegram names carry emoji and arbitrary code points. XML 1.0 forbids lone surrogates and most
    // control chars, and Batik aborts the entire render on the first illegal one (seen live with an
    // emoji in a name). Drop the illegal ones; valid surrogate pairs are kept (they render as a
    // missing glyph with DejaVu, which is cosmetic, not a crash).
    private static String stripInvalidXml(String value) {
        StringBuilder sb = new StringBuilder(value.length());
        int i = 0;
        while (i < value.length()) {
            char c = value.charAt(i);
            if (Character.isHighSurrogate(c) && i + 1 < value.length()
                    && Character.isLowSurrogate(value.charAt(i + 1))) {
                sb.append(c).append(value.charAt(i + 1));
                i += 2;
            } else {
                if (isXmlChar(c)) {
                    sb.append(c);
                }
                i++;
            }
        }
        return sb.toString();
    }

    private static boolean isXmlChar(char c) {
        return c == '\t' || c == '\n' || c == '\r'
                || (c >= 0x20 && c <= 0xD7FF)
                || (c >= 0xE000 && c <= 0xFFFD);
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
