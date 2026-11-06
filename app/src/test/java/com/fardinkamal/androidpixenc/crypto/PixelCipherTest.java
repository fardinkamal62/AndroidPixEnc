package com.fardinkamal.androidpixenc.crypto;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;

import org.junit.Test;

import java.util.Arrays;

public class PixelCipherTest {

    private static final byte[] SALT = new byte[]{
            1, 2, 3, 4, 5, 6, 7, 8,
            9, 10, 11, 12, 13, 14, 15, 16
    };

    @Test
    public void encryptThenDecryptRestoresPixels() {
        int[] original = samplePixels(64 * 48);
        int[] encrypted = PixelCipher.encrypt(original, "correct horse", SALT);
        int[] restored = PixelCipher.decrypt(encrypted, "correct horse", SALT);

        assertFalse(Arrays.equals(original, encrypted));
        assertArrayEquals(original, restored);
    }

    @Test
    public void samePasswordAndSaltProducesSamePermutation() {
        int[] original = samplePixels(128);

        int[] first = PixelCipher.encrypt(original, "secret", SALT);
        int[] second = PixelCipher.encrypt(original, "secret", SALT);

        assertArrayEquals(first, second);
    }

    @Test
    public void differentPasswordProducesDifferentPixels() {
        int[] original = samplePixels(256);

        int[] first = PixelCipher.encrypt(original, "secret", SALT);
        int[] second = PixelCipher.encrypt(original, "Secret", SALT);

        assertFalse(Arrays.equals(first, second));
    }

    @Test
    public void onePixelImageWorks() {
        int[] original = new int[]{0xFF123456};

        int[] encrypted = PixelCipher.encrypt(original, "password", SALT);
        int[] restored = PixelCipher.decrypt(encrypted, "password", SALT);

        assertArrayEquals(original, encrypted);
        assertArrayEquals(original, restored);
    }

    @Test
    public void nonSquareImageWorks() {
        int[] original = samplePixels(7 * 5);
        int[] restored = PixelCipher.decrypt(
                PixelCipher.encrypt(original, "password", SALT),
                "password",
                SALT
        );

        assertArrayEquals(original, restored);
    }

    @Test
    public void saltIsRequired() {
        assertThrows(IllegalArgumentException.class,
                () -> PixelCipher.encrypt(new int[]{1}, "password", new byte[0]));
    }

    @Test
    public void emptyImageIsHandled() {
        int[] result = PixelCipher.encrypt(new int[0], "password", SALT);
        assertEquals(0, result.length);
    }

    private int[] samplePixels(int count) {
        int[] pixels = new int[count];
        for (int i = 0; i < count; i++) {
            pixels[i] = 0xFF000000 | (i * 2654435761L > 0
                    ? (int) ((i * 2654435761L) & 0xFFFFFF)
                    : i);
        }
        return pixels;
    }
}
