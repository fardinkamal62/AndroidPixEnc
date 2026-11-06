package com.fardinkamal.androidpixenc;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.fardinkamal.androidpixenc.crypto.PixelCipher;
import com.fardinkamal.androidpixenc.data.ImageRecord;
import com.fardinkamal.androidpixenc.data.RecordRepository;
import com.fardinkamal.androidpixenc.databinding.ActivityDetailBinding;
import com.fardinkamal.androidpixenc.databinding.DialogPasswordBinding;
import com.fardinkamal.androidpixenc.util.ImageUtils;
import com.fardinkamal.androidpixenc.util.UiUtils;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DetailActivity extends BaseActivity {

    public static final String EXTRA_RECORD_ID = "record_id";

    private ActivityDetailBinding binding;
    private RecordRepository recordRepository;
    private final ExecutorService imageExecutor = Executors.newSingleThreadExecutor();

    private String recordId;
    private ImageRecord record;
    private Bitmap encryptedBitmap;
    private Bitmap restoredBitmap;
    private int loadToken;
    private boolean progressVisible;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!requireFirebase()) {
            return;
        }

        recordId = getIntent().getStringExtra(EXTRA_RECORD_ID);
        if (TextUtils.isEmpty(recordId)) {
            finish();
            return;
        }

        binding = ActivityDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        applyInsets(binding.getRoot());

        recordRepository = new RecordRepository();
        binding.toolbar.setNavigationOnClickListener(
                view -> getOnBackPressedDispatcher().onBackPressed()
        );
        binding.toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_edit) {
                openEdit();
                return true;
            }
            if (item.getItemId() == R.id.action_delete) {
                confirmDelete();
                return true;
            }
            return false;
        });
        binding.decryptButton.setOnClickListener(view -> {
            if (binding.restoredSection.getVisibility() == View.VISIBLE) {
                hideRestoredImage();
            } else {
                showPasswordDialog();
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (binding != null) {
            loadRecord();
        }
    }

    private void loadRecord() {
        int token = ++loadToken;
        if (record == null) {
            setProgress(true, R.string.loading_record);
        }

        recordRepository.getRecord(recordId, new RecordRepository.RecordCallback() {
            @Override
            public void onData(ImageRecord loaded) {
                if (binding == null || token != loadToken
                        || isFinishing() || isDestroyed()) {
                    return;
                }
                record = loaded;
                setProgress(false, 0);
                renderRecord(loaded);
                loadEncryptedImage(loaded, token);
            }

            @Override
            public void onError(Exception error) {
                if (binding == null || token != loadToken
                        || isFinishing() || isDestroyed()) {
                    return;
                }
                setProgress(false, 0);
                if (record == null) {
                    new MaterialAlertDialogBuilder(DetailActivity.this)
                            .setTitle(R.string.load_record_error)
                            .setMessage(R.string.generic_error)
                            .setCancelable(false)
                            .setPositiveButton(R.string.action_ok, (dialog, which) -> finish())
                            .show();
                } else {
                    showMessage(getString(R.string.load_record_error));
                }
            }
        });
    }

    private void renderRecord(ImageRecord loaded) {
        String title = TextUtils.isEmpty(loaded.getTitle())
                ? getString(R.string.detail_title)
                : loaded.getTitle();
        binding.recordTitle.setText(title);
        binding.recordNote.setText(
                TextUtils.isEmpty(loaded.getNote())
                        ? getString(R.string.detail_no_note)
                        : loaded.getNote()
        );
        binding.recordDate.setText(UiUtils.formatTimestamp(loaded.getCreatedAt()));
        binding.recordDimensions.setText(getString(
                R.string.detail_dimensions_value,
                loaded.getWidth(),
                loaded.getHeight()
        ));
    }

    private void loadEncryptedImage(ImageRecord loaded, int token) {
        String base64 = loaded.getImageBase64();
        if (TextUtils.isEmpty(base64)) {
            return;
        }

        imageExecutor.execute(() -> {
            try {
                Bitmap bitmap = ImageUtils.decodeFromBase64(base64);
                runOnUiThread(() -> {
                    if (binding == null || token != loadToken
                            || isFinishing() || isDestroyed()) {
                        bitmap.recycle();
                        return;
                    }
                    if (encryptedBitmap != null && !encryptedBitmap.isRecycled()) {
                        encryptedBitmap.recycle();
                    }
                    encryptedBitmap = bitmap;
                    binding.encryptedImage.setImageBitmap(bitmap);
                });
            } catch (IOException | RuntimeException error) {
                // The metadata is still useful; the image area keeps its placeholder.
            }
        });
    }

    private void showPasswordDialog() {
        if (record == null) {
            showMessage(getString(R.string.loading_record));
            return;
        }

        DialogPasswordBinding dialogBinding = DialogPasswordBinding.inflate(getLayoutInflater());
        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.password_dialog_title)
                .setMessage(R.string.password_dialog_message)
                .setView(dialogBinding.getRoot())
                .setNegativeButton(R.string.password_dialog_negative, null)
                .setPositiveButton(R.string.password_dialog_positive, null)
                .create();

        dialog.setOnShowListener(ignored -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(view -> {
                String password = textOf(dialogBinding.passwordInput.getText());
                if (password.isEmpty()) {
                    dialogBinding.passwordInput.setError(
                            getString(R.string.password_dialog_error)
                    );
                    dialogBinding.passwordInput.requestFocus();
                    return;
                }
                dialog.dismiss();
                decrypt(password);
            });
        });
        dialog.show();
    }

    private void decrypt(String password) {
        if (record == null || progressVisible) {
            return;
        }

        setProgress(true, R.string.decrypt_progress);
        ImageRecord current = record;
        imageExecutor.execute(() -> {
            try {
                Bitmap encrypted = ImageUtils.decodeFromBase64(current.getImageBase64());
                int width = current.getWidth();
                int height = current.getHeight();
                if (encrypted.getWidth() != width || encrypted.getHeight() != height) {
                    encrypted.recycle();
                    throw new IOException("Stored dimensions do not match image");
                }
                int[] encryptedPixels = ImageUtils.toPixels(encrypted);
                encrypted.recycle();

                byte[] salt = ImageUtils.fromBase64(current.getSalt());
                int[] restoredPixels = PixelCipher.decrypt(encryptedPixels, password, salt);
                Bitmap decoded = ImageUtils.toBitmap(restoredPixels, width, height);

                runOnUiThread(() -> {
                    if (binding == null || isFinishing() || isDestroyed()) {
                        decoded.recycle();
                        return;
                    }
                    setProgress(false, 0);
                    if (restoredBitmap != null && !restoredBitmap.isRecycled()) {
                        restoredBitmap.recycle();
                    }
                    restoredBitmap = decoded;
                    binding.restoredImage.setImageBitmap(decoded);
                    binding.restoredSection.setVisibility(View.VISIBLE);
                    binding.decryptButton.setText(R.string.detail_hide);
                });
            } catch (IOException | RuntimeException | OutOfMemoryError error) {
                runOnUiThread(() -> {
                    if (binding == null) {
                        return;
                    }
                    setProgress(false, 0);
                    showMessage(getString(R.string.decrypt_error));
                });
            }
        });
    }

    private void hideRestoredImage() {
        binding.restoredSection.setVisibility(View.GONE);
        binding.decryptButton.setText(R.string.detail_decrypt);
    }

    private void openEdit() {
        if (record == null) {
            showMessage(getString(R.string.loading_record));
            return;
        }
        Intent intent = new Intent(this, EditActivity.class);
        intent.putExtra(EditActivity.EXTRA_RECORD_ID, recordId);
        startActivity(intent);
    }

    private void confirmDelete() {
        if (record == null) {
            showMessage(getString(R.string.loading_record));
            return;
        }
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.delete_title)
                .setMessage(R.string.delete_message)
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton(R.string.delete_action, (dialog, which) -> deleteRecord())
                .show();
    }

    private void deleteRecord() {
        setProgress(true, R.string.delete_progress);
        recordRepository.deleteRecord(recordId, new RecordRepository.Completion() {
            @Override
            public void onSuccess() {
                if (binding == null) {
                    return;
                }
                setProgress(false, 0);
                Toast.makeText(
                        DetailActivity.this,
                        R.string.delete_success,
                        Toast.LENGTH_SHORT
                ).show();
                finish();
            }

            @Override
            public void onError(Exception error) {
                if (binding == null) {
                    return;
                }
                setProgress(false, 0);
                showMessage(getString(R.string.delete_error));
            }
        });
    }

    private void setProgress(boolean visible, int textRes) {
        progressVisible = visible;
        if (binding == null) {
            return;
        }
        binding.progressOverlay.setVisibility(visible ? View.VISIBLE : View.GONE);
        if (visible && textRes != 0) {
            binding.progressText.setText(textRes);
        }
    }

    private String textOf(CharSequence value) {
        return value == null ? "" : value.toString();
    }

    @Override
    protected void onDestroy() {
        imageExecutor.shutdown();
        if (binding != null) {
            binding.encryptedImage.setImageDrawable(null);
            binding.restoredImage.setImageDrawable(null);
        }
        if (encryptedBitmap != null && !encryptedBitmap.isRecycled()) {
            encryptedBitmap.recycle();
        }
        if (restoredBitmap != null && !restoredBitmap.isRecycled()) {
            restoredBitmap.recycle();
        }
        encryptedBitmap = null;
        restoredBitmap = null;
        binding = null;
        super.onDestroy();
    }
}
