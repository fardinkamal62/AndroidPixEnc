package com.fardinkamal.androidpixenc.util;

import android.content.Context;

import com.google.firebase.FirebaseApp;

public final class FirebaseSetup {

    private FirebaseSetup() {
    }

    public static boolean isConfigured(Context context) {
        try {
            return !FirebaseApp.getApps(context).isEmpty();
        } catch (Throwable ignored) {
            return false;
        }
    }
}
