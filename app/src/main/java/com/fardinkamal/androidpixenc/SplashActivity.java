package com.fardinkamal.androidpixenc;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import com.fardinkamal.androidpixenc.databinding.ActivitySplashBinding;
import com.fardinkamal.androidpixenc.util.FirebaseSetup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class SplashActivity extends BaseActivity {

    private static final long SPLASH_DELAY_MS = 650L;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable routeRunnable = this::route;
    private ActivitySplashBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySplashBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        applyInsets(binding.getRoot());
        setLightSystemBars(false);
        handler.postDelayed(routeRunnable, SPLASH_DELAY_MS);
    }

    private void route() {
        if (isFinishing() || isDestroyed()) {
            return;
        }

        if (!FirebaseSetup.isConfigured(this)) {
            new MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.firebase_missing_title)
                    .setMessage(R.string.firebase_missing_message)
                    .setCancelable(false)
                    .setPositiveButton(R.string.firebase_missing_positive, (dialog, which) -> finish())
                    .show();
            return;
        }

        FirebaseUser user;
        try {
            user = FirebaseAuth.getInstance().getCurrentUser();
        } catch (Throwable error) {
            showMessage(getString(R.string.firebase_missing_message));
            return;
        }

        Intent intent = new Intent(
                this,
                user == null ? LoginActivity.class : HomeActivity.class
        );
        startActivity(intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacks(routeRunnable);
        binding = null;
        super.onDestroy();
    }
}
