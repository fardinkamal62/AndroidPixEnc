package com.fardinkamal.androidpixenc.crypto;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;

/**
 * Password-seeded Fisher-Yates pixel permutation.
 *
 * <p>This is intentionally an educational obfuscation scheme, not modern
 * authenticated encryption. The same password and salt always produce the same
 * permutation; a different password produces a different one.</p>
 */
public final class PixelCipher {

    private PixelCipher() {
    }

    public static int[] encrypt(int[] source, String password, byte[] salt) {
        int[] permutation = permutationFor(source.length, password, salt);
        int[] result = new int[source.length];
        for (int i = 0; i < source.length; i++) {
            result[i] = source[permutation[i]];
        }
        return result;
    }

    public static int[] decrypt(int[] encrypted, String password, byte[] salt) {
        int[] permutation = permutationFor(encrypted.length, password, salt);
        int[] result = new int[encrypted.length];
        for (int i = 0; i < encrypted.length; i++) {
            result[permutation[i]] = encrypted[i];
        }
        return result;
    }

    static int[] permutationFor(int size, String password, byte[] salt) {
        if (size < 0) {
            throw new IllegalArgumentException("Pixel count cannot be negative");
        }
        if (password == null) {
            throw new IllegalArgumentException("Password is required");
        }
        if (salt == null || salt.length == 0) {
            throw new IllegalArgumentException("Salt is required");
        }

        long seed = seedFor(password, salt);
        int[] permutation = new int[size];
        for (int i = 0; i < size; i++) {
            permutation[i] = i;
        }

        Random random = new Random(seed);
        for (int i = size - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            int tmp = permutation[i];
            permutation[i] = permutation[j];
            permutation[j] = tmp;
        }
        return permutation;
    }

    static long seedFor(String password, byte[] salt) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] passwordBytes = password.getBytes(StandardCharsets.UTF_8);
            digest.update(passwordBytes);
            digest.update(salt);
            byte[] hash = digest.digest();
            return ByteBuffer.wrap(hash, 0, Long.BYTES).getLong();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }
}
