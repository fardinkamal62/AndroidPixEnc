package com.fardinkamal.androidpixenc.data;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class AuthRepository {

    public interface Callback {
        void onSuccess();

        void onError(Exception error);
    }

    public FirebaseUser currentUser() {
        FirebaseAuth auth = authOrNull();
        return auth == null ? null : auth.getCurrentUser();
    }

    public String currentUserId() {
        FirebaseUser user = currentUser();
        return user == null ? null : user.getUid();
    }

    public void login(String email, String password, Callback callback) {
        FirebaseAuth auth = authOrNull();
        if (auth == null) {
            callback.onError(new IllegalStateException("Firebase is not configured"));
            return;
        }

        auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        callback.onSuccess();
                    } else {
                        callback.onError(asException(task.getException()));
                    }
                });
    }

    public void register(String email, String password, Callback callback) {
        FirebaseAuth auth = authOrNull();
        if (auth == null) {
            callback.onError(new IllegalStateException("Firebase is not configured"));
            return;
        }

        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        callback.onSuccess();
                    } else {
                        callback.onError(asException(task.getException()));
                    }
                });
    }

    public void logout() {
        FirebaseAuth auth = authOrNull();
        if (auth != null) {
            auth.signOut();
        }
    }

    private FirebaseAuth authOrNull() {
        try {
            return FirebaseAuth.getInstance();
        } catch (Throwable ignored) {
            return null;
        }
    }

    private Exception asException(Exception exception) {
        return exception == null
                ? new IllegalStateException("Firebase authentication failed")
                : exception;
    }
}
