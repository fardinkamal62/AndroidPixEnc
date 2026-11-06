package com.fardinkamal.androidpixenc;

import android.content.Intent;
import android.os.Bundle;
import android.view.inputmethod.EditorInfo;

import com.fardinkamal.androidpixenc.data.AuthRepository;
import com.fardinkamal.androidpixenc.data.SessionPrefs;
import com.fardinkamal.androidpixenc.databinding.ActivityLoginBinding;
import com.fardinkamal.androidpixenc.util.AuthErrors;
import com.fardinkamal.androidpixenc.util.Validators;

public class LoginActivity extends BaseActivity {

    private ActivityLoginBinding binding;
    private AuthRepository authRepository;
    private SessionPrefs sessionPrefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!requireFirebase()) {
            return;
        }

        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        applyInsets(binding.getRoot());

        authRepository = new AuthRepository();
        sessionPrefs = new SessionPrefs(this);

        binding.emailInput.setText(sessionPrefs.getLastEmail());
        binding.loginButton.setOnClickListener(view -> attemptLogin());
        binding.registerLink.setOnClickListener(view -> {
            startActivity(new Intent(this, RegisterActivity.class));
        });
        binding.passwordInput.setOnEditorActionListener((view, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                attemptLogin();
                return true;
            }
            return false;
        });
    }

    private void attemptLogin() {
        if (binding == null || !binding.loginButton.isEnabled()) {
            return;
        }

        binding.authErrorText.setVisibility(android.view.View.GONE);
        binding.emailInput.setError(null);
        binding.passwordInput.setError(null);

        String email = Validators.trim(textOf(binding.emailInput.getText()));
        String password = textOf(binding.passwordInput.getText());

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

        setLoading(true);
        authRepository.login(email, password, new AuthRepository.Callback() {
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
        binding.loginButton.setEnabled(!loading);
        binding.registerLink.setEnabled(!loading);
        binding.emailInput.setEnabled(!loading);
        binding.passwordInput.setEnabled(!loading);
        binding.loginButton.setText(
                loading ? R.string.login_in_progress : R.string.login_action
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
