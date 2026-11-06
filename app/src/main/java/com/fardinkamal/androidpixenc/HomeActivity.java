package com.fardinkamal.androidpixenc;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.fardinkamal.androidpixenc.data.AuthRepository;
import com.fardinkamal.androidpixenc.data.ImageRecord;
import com.fardinkamal.androidpixenc.data.RecordRepository;
import com.fardinkamal.androidpixenc.databinding.ActivityHomeBinding;
import com.fardinkamal.androidpixenc.ui.RecordAdapter;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseUser;

import java.util.List;

public class HomeActivity extends BaseActivity {

    private ActivityHomeBinding binding;
    private RecordRepository recordRepository;
    private AuthRepository authRepository;
    private RecordAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!requireFirebase()) {
            return;
        }

        authRepository = new AuthRepository();
        FirebaseUser user = authRepository.currentUser();
        if (user == null) {
            goToLogin();
            return;
        }

        binding = ActivityHomeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        applyInsets(binding.getRoot());

        recordRepository = new RecordRepository();
        adapter = new RecordAdapter(this::openRecord);

        binding.toolbar.setSubtitle(user.getEmail());
        binding.recordsList.setAdapter(adapter);
        binding.addFab.setOnClickListener(view -> openEncrypt());
        binding.emptyActionButton.setOnClickListener(view -> openEncrypt());
        binding.toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_logout) {
                confirmLogout();
                return true;
            }
            return false;
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (binding == null || recordRepository == null) {
            return;
        }
        if (adapter.getItemCount() == 0) {
            setLoading(true);
        }
        recordRepository.observeRecords(new RecordRepository.RecordsCallback() {
            @Override
            public void onData(List<ImageRecord> records) {
                if (binding == null || isFinishing() || isDestroyed()) {
                    return;
                }
                setLoading(false);
                adapter.submitList(records);
                boolean empty = records.isEmpty();
                binding.emptyState.setVisibility(empty ? View.VISIBLE : View.GONE);
                binding.recordsList.setVisibility(empty ? View.GONE : View.VISIBLE);
            }

            @Override
            public void onError(Exception error) {
                if (binding == null || isFinishing() || isDestroyed()) {
                    return;
                }
                setLoading(false);
                showMessage(getString(R.string.home_load_error));
                if (adapter.getItemCount() == 0) {
                    binding.emptyState.setVisibility(View.VISIBLE);
                    binding.recordsList.setVisibility(View.GONE);
                }
            }
        });
    }

    @Override
    protected void onStop() {
        if (recordRepository != null) {
            recordRepository.stopObserving();
        }
        super.onStop();
    }

    private void setLoading(boolean loading) {
        binding.loadingIndicator.setVisibility(loading ? View.VISIBLE : View.GONE);
    }

    private void openRecord(ImageRecord record) {
        if (record.getId() == null) {
            return;
        }
        Intent intent = new Intent(this, DetailActivity.class);
        intent.putExtra(DetailActivity.EXTRA_RECORD_ID, record.getId());
        startActivity(intent);
    }

    private void openEncrypt() {
        startActivity(new Intent(this, EncryptActivity.class));
    }

    private void confirmLogout() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.logout_confirm_title)
                .setMessage(R.string.logout_confirm_message)
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton(R.string.home_logout, (dialog, which) -> logout())
                .show();
    }

    private void logout() {
        authRepository.logout();
        goToLogin();
    }

    private void goToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        if (recordRepository != null) {
            recordRepository.stopObserving();
        }
        binding = null;
        super.onDestroy();
    }
}
