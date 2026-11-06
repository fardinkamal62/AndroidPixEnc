package com.fardinkamal.androidpixenc;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import com.fardinkamal.androidpixenc.crypto.PixelCipher;
import com.fardinkamal.androidpixenc.data.ImageRecord;
import com.fardinkamal.androidpixenc.data.RecordRepository;
import com.fardinkamal.androidpixenc.databinding.ActivityEncryptBinding;
import com.fardinkamal.androidpixenc.util.ImageUtils;
import com.fardinkamal.androidpixenc.util.Validators;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class EncryptActivity extends BaseActivity {

    private static final String STATE_IMAGE_URI = "selected_image_uri";
    private static final String STATE_CAMERA_FILE = "camera_file_path";

    private ActivityEncryptBinding binding;
    private final ExecutorService cryptoExecutor = Executors.newSingleThreadExecutor();
    private final ExecutorService imageExecutor = Executors.newSingleThreadExecutor();
    private RecordRepository recordRepository;

    private Uri selectedImageUri;
    private Bitmap selectedBitmap;
    private File cameraFile;
    private Uri cameraUri;
    private boolean busy;
    private int imageLoadToken;

    private final OnBackPressedCallback busyBackCallback = new OnBackPressedCallback(false) {
        @Override
        public void handleOnBackPressed() {
            // Encryption must finish before leaving the screen.
        }
    };

    private final ActivityResultLauncher<String> galleryLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    loadImage(uri);
                }
            });

    private final ActivityResultLauncher<Uri> cameraLauncher =
            registerForActivityResult(new ActivityResultContracts.TakePicture(), success -> {
                if (success && cameraUri != null) {
                    loadImage(cameraUri);
                } else {
                    ImageUtils.deleteQuietly(cameraFile);
                    cameraFile = null;
                    cameraUri = null;
                    showMessage(getString(R.string.camera_error));
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!requireFirebase()) {
            return;
        }

        binding = ActivityEncryptBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        applyInsets(binding.getRoot());

        recordRepository = new RecordRepository();
        getOnBackPressedDispatcher().addCallback(this, busyBackCallback);
        binding.resizeNotice.setText(getString(
                R.string.image_resize_notice,
                ImageUtils.MAX_IMAGE_DIMENSION
        ));

        binding.toolbar.setNavigationOnClickListener(view -> {
            if (!busy) {
                getOnBackPressedDispatcher().onBackPressed();
            }
        });
        binding.galleryButton.setOnClickListener(view -> openGallery());
        binding.imageCard.setOnClickListener(view -> openGallery());
        binding.cameraButton.setOnClickListener(view -> openCamera());
        binding.encryptButton.setOnClickListener(view -> attemptEncrypt());
        binding.confirmPasswordInput.setOnEditorActionListener((view, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                attemptEncrypt();
                return true;
            }
            return false;
        });

        if (savedInstanceState != null) {
            String filePath = savedInstanceState.getString(STATE_CAMERA_FILE);
            if (filePath != null) {
                cameraFile = new File(filePath);
                try {
                    cameraUri = ImageUtils.uriForFile(this, cameraFile);
                } catch (RuntimeException ignored) {
                    cameraFile = null;
                }
            }
            String uriString = savedInstanceState.getString(STATE_IMAGE_URI);
            if (uriString != null) {
                loadImage(Uri.parse(uriString));
            }
        }
    }

    private void openGallery() {
        if (busy) {
            return;
        }
        galleryLauncher.launch("image/*");
    }

    private void openCamera() {
        if (busy) {
            return;
        }
        try {
            ImageUtils.deleteQuietly(cameraFile);
            cameraFile = ImageUtils.createCameraFile(this);
            cameraUri = ImageUtils.uriForFile(this, cameraFile);
            cameraLauncher.launch(cameraUri);
        } catch (ActivityNotFoundException error) {
            ImageUtils.deleteQuietly(cameraFile);
            cameraFile = null;
            cameraUri = null;
            showMessage(getString(R.string.camera_unavailable));
        } catch (Exception error) {
            ImageUtils.deleteQuietly(cameraFile);
            cameraFile = null;
            cameraUri = null;
            showMessage(getString(R.string.camera_error));
        }
    }

    private void loadImage(Uri uri) {
        if (binding == null || busy) {
            return;
        }

        imageLoadToken++;
        int token = imageLoadToken;
        selectedImageUri = uri;
        if (selectedBitmap != null && !selectedBitmap.isRecycled()) {
            selectedBitmap.recycle();
        }
        selectedBitmap = null;

        binding.previewImage.setImageDrawable(null);
        binding.previewImage.setVisibility(View.GONE);
        binding.imagePlaceholder.setVisibility(View.VISIBLE);
        binding.imageProgress.setVisibility(View.VISIBLE);
        binding.galleryButton.setEnabled(false);
        binding.cameraButton.setEnabled(false);

        imageExecutor.execute(() -> {
            try {
                Bitmap bitmap = ImageUtils.decodeFromUri(this, uri);
                runOnUiThread(() -> {
                    if (token != imageLoadToken || binding == null
                            || isFinishing() || isDestroyed()) {
                        bitmap.recycle();
                        return;
                    }
                    selectedBitmap = bitmap;
                    binding.previewImage.setImageBitmap(bitmap);
                    binding.previewImage.setVisibility(View.VISIBLE);
                    binding.imagePlaceholder.setVisibility(View.GONE);
                    binding.imageProgress.setVisibility(View.GONE);
                    binding.galleryButton.setEnabled(true);
                    binding.cameraButton.setEnabled(true);
                });
            } catch (IOException | RuntimeException | OutOfMemoryError error) {
                runOnUiThread(() -> {
                    if (token != imageLoadToken || binding == null
                            || isFinishing() || isDestroyed()) {
                        return;
                    }
                    selectedImageUri = null;
                    binding.imageProgress.setVisibility(View.GONE);
                    binding.imagePlaceholder.setVisibility(View.VISIBLE);
                    binding.galleryButton.setEnabled(true);
                    binding.cameraButton.setEnabled(true);
                    showMessage(getString(R.string.image_load_error));
                });
            }
        });
    }

    private void attemptEncrypt() {
        if (binding == null || busy) {
            return;
        }

        binding.titleInput.setError(null);
        binding.passwordInput.setError(null);
        binding.confirmPasswordInput.setError(null);

        String title = Validators.trim(textOf(binding.titleInput.getText()));
        String note = Validators.trim(textOf(binding.noteInput.getText()));
        String password = textOf(binding.passwordInput.getText());
        String confirmPassword = textOf(binding.confirmPasswordInput.getText());

        if (selectedBitmap == null || selectedBitmap.isRecycled()) {
            showMessage(getString(R.string.error_image_required));
            return;
        }
        if (title.isEmpty()) {
            binding.titleInput.setError(getString(R.string.error_title_required));
            binding.titleInput.requestFocus();
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

        Bitmap source = selectedBitmap;
        setBusy(true);

        cryptoExecutor.execute(() -> {
            try {
                int width = source.getWidth();
                int height = source.getHeight();
                int[] original = ImageUtils.toPixels(source);
                byte[] salt = ImageUtils.generateSalt();
                int[] encrypted = PixelCipher.encrypt(original, password, salt);
                Bitmap encryptedBitmap = ImageUtils.toBitmap(encrypted, width, height);
                byte[] png;
                try {
                    png = ImageUtils.encodePng(encryptedBitmap);
                } finally {
                    encryptedBitmap.recycle();
                }

                ImageRecord record = new ImageRecord(
                        title,
                        note,
                        System.currentTimeMillis(),
                        ImageUtils.toBase64(salt),
                        ImageUtils.toBase64(png),
                        width,
                        height
                );

                runOnUiThread(() -> {
                    if (binding == null || isFinishing() || isDestroyed()) {
                        return;
                    }
                    recordRepository.createRecord(record, new RecordRepository.Completion() {
                        @Override
                        public void onSuccess() {
                            if (binding == null) {
                                return;
                            }
                            setBusy(false);
                            Toast.makeText(
                                    EncryptActivity.this,
                                    R.string.encrypt_success,
                                    Toast.LENGTH_SHORT
                            ).show();
                            finish();
                        }

                        @Override
                        public void onError(Exception error) {
                            if (binding == null) {
                                return;
                            }
                            setBusy(false);
                            showMessage(getString(R.string.encrypt_save_error));
                        }
                    });
                });
            } catch (IOException | RuntimeException | OutOfMemoryError error) {
                runOnUiThread(() -> {
                    if (binding == null) {
                        return;
                    }
                    setBusy(false);
                    showMessage(getString(R.string.encrypt_error));
                });
            }
        });
    }

    private void setBusy(boolean value) {
        busy = value;
        busyBackCallback.setEnabled(value);
        binding.progressOverlay.setVisibility(value ? View.VISIBLE : View.GONE);
        binding.encryptButton.setEnabled(!value);
        binding.galleryButton.setEnabled(!value);
        binding.cameraButton.setEnabled(!value);
        binding.titleInput.setEnabled(!value);
        binding.noteInput.setEnabled(!value);
        binding.passwordInput.setEnabled(!value);
        binding.confirmPasswordInput.setEnabled(!value);
        binding.progressText.setText(R.string.encrypt_progress);
    }

    private String textOf(CharSequence value) {
        return value == null ? "" : value.toString();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (selectedImageUri != null) {
            outState.putString(STATE_IMAGE_URI, selectedImageUri.toString());
        }
        if (cameraFile != null) {
            outState.putString(STATE_CAMERA_FILE, cameraFile.getAbsolutePath());
        }
    }

    @Override
    protected void onDestroy() {
        cryptoExecutor.shutdown();
        imageExecutor.shutdown();
        if (binding != null) {
            binding.previewImage.setImageDrawable(null);
        }
        if (selectedBitmap != null && !selectedBitmap.isRecycled()) {
            selectedBitmap.recycle();
        }
        selectedBitmap = null;
        if (!isChangingConfigurations()) {
            ImageUtils.deleteQuietly(cameraFile);
        }
        binding = null;
        super.onDestroy();
    }
}
