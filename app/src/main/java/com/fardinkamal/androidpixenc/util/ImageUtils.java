package com.fardinkamal.androidpixenc.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.util.Base64;

import androidx.core.content.FileProvider;
import androidx.exifinterface.media.ExifInterface;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.security.SecureRandom;

public final class ImageUtils {

    public static final int MAX_IMAGE_DIMENSION = 1024;
    public static final int SALT_BYTES = 16;
    private static final int THUMBNAIL_DIMENSION = 240;

    private ImageUtils() {
    }

    public static Bitmap decodeFromUri(Context context, Uri uri) throws IOException {
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        try (InputStream input = openStream(context, uri)) {
            BitmapFactory.decodeStream(input, null, bounds);
        }

        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            throw new IOException("Unable to read image dimensions");
        }

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        options.inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight, MAX_IMAGE_DIMENSION);

        Bitmap bitmap;
        try (InputStream input = openStream(context, uri)) {
            bitmap = BitmapFactory.decodeStream(input, null, options);
        } catch (OutOfMemoryError error) {
            throw new IOException("Image is too large to decode", error);
        }

        if (bitmap == null) {
            throw new IOException("Unable to decode image");
        }

        bitmap = orientBitmap(context, uri, bitmap);
        bitmap = scaleDown(bitmap, MAX_IMAGE_DIMENSION);
        return ensureArgb(bitmap);
    }

    public static int[] toPixels(Bitmap bitmap) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int[] pixels = new int[width * height];
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height);
        return pixels;
    }

    public static Bitmap toBitmap(int[] pixels, int width, int height) {
        if (pixels.length != width * height) {
            throw new IllegalArgumentException("Pixel count does not match dimensions");
        }
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height);
        return bitmap;
    }

    public static byte[] encodePng(Bitmap bitmap) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)) {
            throw new IOException("Could not encode PNG");
        }
        return output.toByteArray();
    }

    public static String toBase64(byte[] data) {
        return Base64.encodeToString(data, Base64.NO_WRAP);
    }

    public static byte[] fromBase64(String data) {
        return Base64.decode(data, Base64.NO_WRAP);
    }

    public static byte[] generateSalt() {
        byte[] salt = new byte[SALT_BYTES];
        new SecureRandom().nextBytes(salt);
        return salt;
    }

    public static Bitmap decodeFromBase64(String base64) throws IOException {
        byte[] bytes;
        try {
            bytes = fromBase64(base64);
        } catch (IllegalArgumentException error) {
            throw new IOException("Invalid image data", error);
        }

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length, options);
        if (bitmap == null) {
            throw new IOException("Unable to decode stored image");
        }
        return ensureArgb(bitmap);
    }

    public static Bitmap decodeThumbnail(String base64, int targetDimension) {
        try {
            byte[] bytes = fromBase64(base64);

            BitmapFactory.Options bounds = new BitmapFactory.Options();
            bounds.inJustDecodeBounds = true;
            BitmapFactory.decodeByteArray(bytes, 0, bytes.length, bounds);
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
                return null;
            }

            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inPreferredConfig = Bitmap.Config.ARGB_8888;
            options.inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight, targetDimension);
            return BitmapFactory.decodeByteArray(bytes, 0, bytes.length, options);
        } catch (IllegalArgumentException | OutOfMemoryError error) {
            return null;
        }
    }

    public static File createCameraFile(Context context) throws IOException {
        File directory = new File(context.getCacheDir(), "images");
        if (!directory.exists() && !directory.mkdirs()) {
            throw new IOException("Could not create camera cache directory");
        }
        return File.createTempFile("capture_", ".jpg", directory);
    }

    public static Uri uriForFile(Context context, File file) {
        return FileProvider.getUriForFile(
                context,
                context.getPackageName() + ".fileprovider",
                file
        );
    }

    public static void deleteQuietly(File file) {
        if (file == null) {
            return;
        }
        try {
            if (file.exists()) {
                //noinspection ResultOfMethodCallIgnored
                file.delete();
            }
        } catch (Throwable ignored) {
            // Cache cleanup is best-effort.
        }
    }

    private static InputStream openStream(Context context, Uri uri) throws IOException {
        InputStream input = context.getContentResolver().openInputStream(uri);
        if (input == null) {
            throw new IOException("Unable to open image stream");
        }
        return input;
    }

    private static int sampleSize(int width, int height, int maxDimension) {
        int sample = 1;
        while (true) {
            int next = sample * 2;
            double nextMax = Math.max(width / (double) next, height / (double) next);
            if (nextMax < maxDimension) {
                return sample;
            }
            sample = next;
        }
    }

    private static Bitmap orientBitmap(Context context, Uri uri, Bitmap bitmap) {
        int orientation = ExifInterface.ORIENTATION_NORMAL;
        try (InputStream input = openStream(context, uri)) {
            ExifInterface exif = new ExifInterface(input);
            orientation = exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
            );
        } catch (Exception ignored) {
            // EXIF is optional; continue with the decoded orientation.
        }

        Matrix matrix = new Matrix();
        switch (orientation) {
            case ExifInterface.ORIENTATION_FLIP_HORIZONTAL:
                matrix.postScale(-1f, 1f);
                break;
            case ExifInterface.ORIENTATION_ROTATE_180:
                matrix.postRotate(180f);
                break;
            case ExifInterface.ORIENTATION_FLIP_VERTICAL:
                matrix.postScale(1f, -1f);
                break;
            case ExifInterface.ORIENTATION_TRANSPOSE:
                matrix.postRotate(90f);
                matrix.postScale(-1f, 1f);
                break;
            case ExifInterface.ORIENTATION_ROTATE_90:
                matrix.postRotate(90f);
                break;
            case ExifInterface.ORIENTATION_TRANSVERSE:
                matrix.postRotate(-90f);
                matrix.postScale(-1f, 1f);
                break;
            case ExifInterface.ORIENTATION_ROTATE_270:
                matrix.postRotate(-90f);
                break;
            case ExifInterface.ORIENTATION_NORMAL:
            case ExifInterface.ORIENTATION_UNDEFINED:
            default:
                return bitmap;
        }

        try {
            Bitmap transformed = Bitmap.createBitmap(
                    bitmap,
                    0,
                    0,
                    bitmap.getWidth(),
                    bitmap.getHeight(),
                    matrix,
                    true
            );
            if (transformed != bitmap) {
                bitmap.recycle();
            }
            return transformed;
        } catch (OutOfMemoryError error) {
            return bitmap;
        }
    }

    private static Bitmap scaleDown(Bitmap bitmap, int maxDimension) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int largest = Math.max(width, height);
        if (largest <= maxDimension) {
            return bitmap;
        }

        float ratio = maxDimension / (float) largest;
        int targetWidth = Math.max(1, Math.round(width * ratio));
        int targetHeight = Math.max(1, Math.round(height * ratio));
        Bitmap scaled = Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true);
        if (scaled != bitmap) {
            bitmap.recycle();
        }
        return scaled;
    }

    private static Bitmap ensureArgb(Bitmap bitmap) {
        if (bitmap.getConfig() == Bitmap.Config.ARGB_8888) {
            return bitmap;
        }
        Bitmap copy = bitmap.copy(Bitmap.Config.ARGB_8888, true);
        if (copy != null && copy != bitmap) {
            bitmap.recycle();
            return copy;
        }
        return bitmap;
    }
}
