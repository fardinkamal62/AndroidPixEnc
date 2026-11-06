package com.fardinkamal.androidpixenc;

import android.content.Intent;
import android.os.Bundle;
import android.view.inputmethod.EditorInfo;

import com.fardinkamal.androidpixenc.data.AuthRepository;
import com.fardinkamal.androidpixenc.data.SessionPrefs;
import com.fardinkamal.androidpixenc.databinding.ActivityRegisterBinding;
import com.fardinkamal.androidpixenc.util.AuthErrors;
import com.fardinkamal.androidpixenc.util.Validators;

public class RegisterActivity extends BaseActivity {

    private ActivityRegisterBinding binding;
    private AuthRepository authRepository;
    private SessionPrefs sessionPrefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!requireFirebase()) {
            return;
        }

        binding = ActivityRegisterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        applyInsets(binding.getRoot());

        authRepository = new AuthRepository();
        sessionPrefs = new SessionPrefs(this);

        binding.registerButton.setOnClickListener(view -> attemptRegister());
        binding.loginLink.setOnClickListener(view -> finish());
        binding.confirmPasswordInput.setOnEditorActionListener((view, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                attemptRegister();
                return true;
            }
            return false;
        });
    }

    private void attemptRegister() {
        if (binding == null || !binding.registerButton.isEnabled()) {
            return;
        }

        binding.authErrorText.setVisibility(android.view.View.GONE);
        binding.emailInput.setError(null);
        binding.passwordInput.setError(null);
        binding.confirmPasswordInput.setError(null);

        String email = Validators.trim(textOf(binding.emailInput.getText()));
        String password = textOf(binding.passwordInput.getText());
        String confirmPassword = textOf(binding.confirmPasswordInput.getText());

        if (!Validators.isValidEmail(email)) {
            binding.emailInput.setError(getString(
                    email.isEmpty() ? R.string.error_email_required : R.string.error_email_invalid
            ));
            binding.emailInput.requestFocus();
            return;
        }
        if (password.isEmpty()) {
            binding.passwordInput.setError(getString(R.string.error_password_required));
            binding.passwordInput.requestFocus();
            return;
        }
        if (password.length() < Validators.MIN_PASSWORD_LENGTH) {
            binding.passwordInput.setError(getString(R.string.error_password_short));
            binding.passwordInput.requestFocus();
            return;
        }
        if (confirmPassword.isEmpty()) {
            binding.confirmPasswordInput.setError(getString(R.string.error_confirm_required));
            binding.confirmPasswordInput.requestFocus();
            return;
        }
        if (!password.equals(confirmPassword)) {
            binding.confirmPasswordInput.setError(getString(R.string.error_password_mismatch));
            binding.confirmPasswordInput.requestFocus();
            return;
        }

        setLoading(true);
        authRepository.register(email, password, new AuthRepository.Callback() {
            @Override
            public void onSuccess() {
                if (binding == null) {
                    return;
                }
                sessionPrefs.saveLastEmail(email);
                goHome();
            }

            @Override
            public void onError(Exception error) {
                if (binding == null) {
                    return;
                }
                setLoading(false);
                showAuthError(getString(AuthErrors.messageRes(error)));
            }
        });
    }

    private void setLoading(boolean loading) {
        binding.registerButton.setEnabled(!loading);
        binding.loginLink.setEnabled(!loading);
        binding.emailInput.setEnabled(!loading);
        binding.passwordInput.setEnabled(!loading);
        binding.confirmPasswordInput.setEnabled(!loading);
        binding.registerButton.setText(
                loading ? R.string.register_in_progress : R.string.register_action
        );
    }

    private void showAuthError(String message) {
        binding.authErrorText.setText(message);
        binding.authErrorText.setVisibility(android.view.View.VISIBLE);
    }

    private void goHome() {
        Intent intent = new Intent(this, HomeActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private String textOf(CharSequence value) {
        return value == null ? "" : value.toString();
    }

    @Override
    protected void onDestroy() {
        binding = null;
        super.onDestroy();
    }
}
