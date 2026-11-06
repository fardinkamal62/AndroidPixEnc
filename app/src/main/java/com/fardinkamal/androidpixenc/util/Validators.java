package com.fardinkamal.androidpixenc.util;

import android.text.TextUtils;
import android.util.Patterns;

public final class Validators {

    public static final int MIN_PASSWORD_LENGTH = 6;

    private Validators() {
    }

    public static String trim(String value) {
        return value == null ? "" : value.trim();
    }

    public static boolean isValidEmail(String email) {
        String value = trim(email);
        return !TextUtils.isEmpty(value) && Patterns.EMAIL_ADDRESS.matcher(value).matches();
    }
}
