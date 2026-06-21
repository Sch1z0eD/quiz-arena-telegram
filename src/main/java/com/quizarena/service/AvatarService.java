package com.quizarena.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.methods.GetUserProfilePhotos;
import org.telegram.telegrambots.meta.api.objects.File;
import org.telegram.telegrambots.meta.api.objects.UserProfilePhotos;
import org.telegram.telegrambots.meta.api.objects.photo.PhotoSize;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.imageio.ImageIO;

/**
 * Fetches a user's Telegram profile photo, resized to the avatar slot, for use in cards.
 * Cached in Redis so a render does not hit the network each time; absence is cached too (the common
 * case: no photo or privacy). Any failure returns null and the caller falls back to initials.
 */
@Service
public class AvatarService {

    private static final Logger log = LoggerFactory.getLogger(AvatarService.class);

    private static final int SLOT_PX = 96;
    private static final int FETCH_TIMEOUT_SECONDS = 3;
    private static final Duration TTL = Duration.ofHours(24);
    private static final Duration NEGATIVE_TTL = Duration.ofHours(6);
    private static final String NEGATIVE = "-";

    private final TelegramClient telegramClient;
    private final StringRedisTemplate redis;
    private final ExecutorService fetchPool = Executors.newVirtualThreadPerTaskExecutor();

    public AvatarService(TelegramClient telegramClient, StringRedisTemplate redis) {
        this.telegramClient = telegramClient;
        this.redis = redis;
    }

    public byte[] get(long userId) {
        String cached = redis.opsForValue().get(key(userId));
        if (cached != null) {
            return NEGATIVE.equals(cached) ? null : Base64.getDecoder().decode(cached);
        }
        // Hard timeout: a slow Telegram file download must not stall a duel start. try/catch handles
        // crashes; the timeout handles hangs. Either way -> null -> the card renders initials.
        Future<byte[]> future = fetchPool.submit(() -> fetchAndCache(userId));
        try {
            return future.get(FETCH_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            future.cancel(true);
            log.warn("Avatar fetch timed out for {}", userId);
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    private byte[] fetchAndCache(long userId) {
        try {
            UserProfilePhotos photos =
                    telegramClient.execute(GetUserProfilePhotos.builder().userId(userId).limit(1).build());
            if (photos == null || photos.getPhotos().isEmpty() || photos.getPhotos().get(0).isEmpty()) {
                cacheNegative(userId); // no photo or privacy: stable, worth not re-asking
                return null;
            }
            PhotoSize size = pickSize(photos.getPhotos().get(0));
            File file = telegramClient.execute(GetFile.builder().fileId(size.getFileId()).build());
            byte[] raw;
            try (InputStream in = telegramClient.downloadFileAsStream(file)) {
                raw = in.readAllBytes();
            }
            byte[] png = resize(raw);
            if (png == null) {
                return null; // unreadable image: transient, do not poison the cache
            }
            redis.opsForValue().set(key(userId), Base64.getEncoder().encodeToString(png), TTL);
            return png;
        } catch (Exception e) {
            log.warn("Avatar fetch failed for {}", userId, e);
            return null; // transient error: skip caching so it self-heals next time
        }
    }

    private void cacheNegative(long userId) {
        redis.opsForValue().set(key(userId), NEGATIVE, NEGATIVE_TTL);
    }

    private static PhotoSize pickSize(List<PhotoSize> sizes) {
        for (PhotoSize size : sizes) { // Telegram lists sizes ascending; take the smallest that fills the slot
            if (size.getWidth() != null && size.getWidth() >= SLOT_PX) {
                return size;
            }
        }
        return sizes.get(sizes.size() - 1);
    }

    private static byte[] resize(byte[] raw) throws Exception {
        BufferedImage src = ImageIO.read(new ByteArrayInputStream(raw));
        if (src == null) {
            return null;
        }
        BufferedImage dst = new BufferedImage(SLOT_PX, SLOT_PX, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = dst.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.drawImage(src, 0, 0, SLOT_PX, SLOT_PX, null);
        g.dispose();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(dst, "png", out);
        return out.toByteArray();
    }

    private static String key(long userId) {
        return "avatar:" + userId;
    }
}
