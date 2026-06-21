package com.quizarena.service;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.methods.GetUserProfilePhotos;
import org.telegram.telegrambots.meta.api.objects.File;
import org.telegram.telegrambots.meta.api.objects.UserProfilePhotos;
import org.telegram.telegrambots.meta.api.objects.photo.PhotoSize;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.time.Duration;
import java.util.Base64;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AvatarServiceTest {

    private final TelegramClient client = mock(TelegramClient.class);
    private final StringRedisTemplate redis = mock(StringRedisTemplate.class);
    @SuppressWarnings("unchecked")
    private final ValueOperations<String, String> ops = mock(ValueOperations.class);
    private final AvatarService service = new AvatarService(client, redis);

    AvatarServiceTest() {
        when(redis.opsForValue()).thenReturn(ops);
    }

    @Test
    void cacheHitReturnsBytesWithoutTelegram() throws Exception {
        byte[] png = samplePng();
        when(ops.get("avatar:7")).thenReturn(Base64.getEncoder().encodeToString(png));
        assertArrayEquals(png, service.get(7L));
        verifyNoInteractions(client);
    }

    @Test
    void negativeCacheReturnsNullWithoutTelegram() {
        when(ops.get("avatar:8")).thenReturn("-");
        assertNull(service.get(8L));
        verifyNoInteractions(client);
    }

    @Test
    void missThenNoPhotoCachesNegativeAndReturnsNull() throws Exception {
        when(ops.get("avatar:9")).thenReturn(null);
        UserProfilePhotos empty = mock(UserProfilePhotos.class);
        when(empty.getPhotos()).thenReturn(List.of());
        when(client.execute(any(GetUserProfilePhotos.class))).thenReturn(empty);
        assertNull(service.get(9L));
        verify(ops).set(eq("avatar:9"), eq("-"), any(Duration.class));
    }

    @Test
    void missFetchesResizesAndCaches() throws Exception {
        when(ops.get("avatar:10")).thenReturn(null);
        PhotoSize size = mock(PhotoSize.class);
        when(size.getFileId()).thenReturn("fid");
        when(size.getWidth()).thenReturn(160);
        UserProfilePhotos photos = mock(UserProfilePhotos.class);
        when(photos.getPhotos()).thenReturn(List.of(List.of(size)));
        when(client.execute(any(GetUserProfilePhotos.class))).thenReturn(photos);
        when(client.execute(any(GetFile.class))).thenReturn(mock(File.class));
        when(client.downloadFileAsStream(any(File.class))).thenReturn(new ByteArrayInputStream(samplePng()));

        byte[] result = service.get(10L);

        assertNotNull(result);
        assertEquals((byte) 0x89, result[0]); // PNG magic - resized profile photo
        verify(ops).set(eq("avatar:10"), any(String.class), any(Duration.class));
    }

    private static byte[] samplePng() throws Exception {
        BufferedImage img = new BufferedImage(160, 160, BufferedImage.TYPE_INT_ARGB);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(img, "png", out);
        return out.toByteArray();
    }
}
