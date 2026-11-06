package com.fardinkamal.androidpixenc.util;

import android.view.View;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;

public final class UiUtils {

    private UiUtils() {
    }

    public static void applyInsets(View root) {
        ViewCompat.setOnApplyWindowInsetsListener(root, (view, windowInsets) -> {
            Insets bars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            Insets ime = windowInsets.getInsets(WindowInsetsCompat.Type.ime());
            Insets cutout = windowInsets.getInsets(WindowInsetsCompat.Type.displayCutout());
            view.setPadding(
                    Math.max(bars.left, cutout.left),
                    Math.max(bars.top, cutout.top),
                    Math.max(bars.right, cutout.right),
                    Math.max(bars.bottom, Math.max(ime.bottom, cutout.bottom))
            );
            return windowInsets;
        });
        ViewCompat.requestApplyInsets(root);
    }

    public static String formatTimestamp(long timestamp) {
        if (timestamp <= 0L) {
            return "—";
        }
        DateFormat format = DateFormat.getDateTimeInstance(
                DateFormat.MEDIUM,
                DateFormat.SHORT,
                Locale.getDefault()
        );
        return format.format(new Date(timestamp));
    }
}
