package com.fardinkamal.androidpixenc.util;

import com.fardinkamal.androidpixenc.R;
import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.FirebaseTooManyRequestsException;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;

public final class AuthErrors {

    private AuthErrors() {
    }

    public static int messageRes(Exception error) {
        if (error instanceof FirebaseAuthUserCollisionException) {
            return R.string.auth_error_email_in_use;
        }
        if (error instanceof FirebaseAuthWeakPasswordException) {
            return R.string.auth_error_weak_password;
        }
        if (error instanceof FirebaseAuthInvalidCredentialsException
                || error instanceof FirebaseAuthInvalidUserException) {
            return R.string.auth_error_invalid_credentials;
        }
        if (error instanceof FirebaseNetworkException
                || error instanceof FirebaseTooManyRequestsException) {
            return R.string.auth_error_network;
        }
        if (error instanceof FirebaseAuthException) {
            String code = ((FirebaseAuthException) error).getErrorCode();
            if ("ERROR_CONFIGURATION_NOT_FOUND".equals(code)) {
                return R.string.auth_error_configuration_not_found;
            }
            if ("ERROR_OPERATION_NOT_ALLOWED".equals(code)) {
                return R.string.auth_error_provider_disabled;
            }
            if ("ERROR_INVALID_API_KEY".equals(code)
                    || "ERROR_API_KEY_EXPIRED".equals(code)) {
                return R.string.auth_error_invalid_api_key;
            }
            if ("ERROR_API_NOT_AVAILABLE".equals(code)) {
                return R.string.auth_error_api_unavailable;
            }
        }
        return R.string.auth_error_generic;
    }
}
