package com.fardinkamal.androidpixenc.data;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Small non-sensitive preferences. Passwords and image data never go here.
 */
public class SessionPrefs {

    private static final String PREFS_NAME = "pixenc_session";
    private static final String KEY_LAST_EMAIL = "last_email";

    private final SharedPreferences preferences;

    public SessionPrefs(Context context) {
        preferences = context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public void saveLastEmail(String email) {
        preferences.edit().putString(KEY_LAST_EMAIL, email).apply();
    }

    public String getLastEmail() {
        return preferences.getString(KEY_LAST_EMAIL, "");
    }

    public void clear() {
        preferences.edit().clear().apply();
    }
}
