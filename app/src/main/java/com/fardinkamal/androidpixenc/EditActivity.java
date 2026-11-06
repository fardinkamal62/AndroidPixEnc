package com.fardinkamal.androidpixenc;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.fardinkamal.androidpixenc.data.ImageRecord;
import com.fardinkamal.androidpixenc.data.RecordRepository;
import com.fardinkamal.androidpixenc.databinding.ActivityEditBinding;
import com.fardinkamal.androidpixenc.util.Validators;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class EditActivity extends BaseActivity {

    public static final String EXTRA_RECORD_ID = "record_id";

    private ActivityEditBinding binding;
    private RecordRepository recordRepository;
    private String recordId;
    private ImageRecord record;
    private boolean saving;

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

        binding = ActivityEditBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        applyInsets(binding.getRoot());

        recordRepository = new RecordRepository();
        binding.toolbar.setNavigationOnClickListener(
                view -> getOnBackPressedDispatcher().onBackPressed()
        );
        binding.saveButton.setOnClickListener(view -> save());
        loadRecord();
    }

    private void loadRecord() {
        setLoading(true);
        recordRepository.getRecord(recordId, new RecordRepository.RecordCallback() {
            @Override
            public void onData(ImageRecord loaded) {
                if (binding == null || isFinishing() || isDestroyed()) {
                    return;
                }
                record = loaded;
                binding.titleInput.setText(loaded.getTitle());
                binding.noteInput.setText(loaded.getNote());
                binding.titleInput.setSelection(binding.titleInput.length());
                setLoading(false);
            }

            @Override
            public void onError(Exception error) {
                if (binding == null || isFinishing() || isDestroyed()) {
                    return;
                }
                setLoading(false);
                new MaterialAlertDialogBuilder(EditActivity.this)
                        .setTitle(R.string.load_record_error)
                        .setMessage(R.string.generic_error)
                        .setCancelable(false)
                        .setPositiveButton(R.string.action_ok, (dialog, which) -> finish())
                        .show();
            }
        });
    }

    private void save() {
        if (binding == null || record == null || saving) {
            return;
        }

        binding.titleInput.setError(null);
        String title = Validators.trim(textOf(binding.titleInput.getText()));
        String note = Validators.trim(textOf(binding.noteInput.getText()));

        if (title.isEmpty()) {
            binding.titleInput.setError(getString(R.string.error_title_required));
            binding.titleInput.requestFocus();
            return;
        }

        setSaving(true);
        recordRepository.updateDetails(recordId, title, note, new RecordRepository.Completion() {
            @Override
            public void onSuccess() {
                if (binding == null) {
                    return;
                }
                Toast.makeText(EditActivity.this, R.string.save_success, Toast.LENGTH_SHORT).show();
                finish();
            }

            @Override
            public void onError(Exception error) {
                if (binding == null) {
                    return;
                }
                setSaving(false);
                showMessage(getString(R.string.save_error));
            }
        });
    }

    private void setLoading(boolean loading) {
        binding.loadingIndicator.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.titleInput.setEnabled(!loading);
        binding.noteInput.setEnabled(!loading);
        binding.saveButton.setEnabled(!loading);
    }

    private void setSaving(boolean value) {
        saving = value;
        binding.titleInput.setEnabled(!value);
        binding.noteInput.setEnabled(!value);
        binding.saveButton.setEnabled(!value);
        binding.saveButton.setText(value ? R.string.save_progress : R.string.save_action);
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
