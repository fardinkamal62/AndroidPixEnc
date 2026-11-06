package com.fardinkamal.androidpixenc.ui;

import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.fardinkamal.androidpixenc.R;
import com.fardinkamal.androidpixenc.data.ImageRecord;
import com.fardinkamal.androidpixenc.databinding.ItemRecordBinding;
import com.fardinkamal.androidpixenc.util.ImageUtils;
import com.fardinkamal.androidpixenc.util.UiUtils;

import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RecordAdapter extends ListAdapter<ImageRecord, RecordAdapter.RecordViewHolder> {

    public interface OnRecordClickListener {
        void onRecordClick(ImageRecord record);
    }

    private static final ExecutorService THUMBNAIL_EXECUTOR =
            Executors.newFixedThreadPool(2);
    private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());

    private static final DiffUtil.ItemCallback<ImageRecord> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<ImageRecord>() {
                @Override
                public boolean areItemsTheSame(@NonNull ImageRecord oldItem,
                                               @NonNull ImageRecord newItem) {
                    if (oldItem.getId() != null && newItem.getId() != null) {
                        return oldItem.getId().equals(newItem.getId());
                    }
                    return oldItem.getCreatedAt() == newItem.getCreatedAt();
                }

                @Override
                public boolean areContentsTheSame(@NonNull ImageRecord oldItem,
                                                  @NonNull ImageRecord newItem) {
                    return Objects.equals(oldItem.getTitle(), newItem.getTitle())
                            && Objects.equals(oldItem.getNote(), newItem.getNote())
                            && oldItem.getCreatedAt() == newItem.getCreatedAt();
                }
            };

    private final OnRecordClickListener clickListener;

    public RecordAdapter(OnRecordClickListener clickListener) {
        super(DIFF_CALLBACK);
        this.clickListener = clickListener;
    }

    @NonNull
    @Override
    public RecordViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemRecordBinding binding = ItemRecordBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false
        );
        return new RecordViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull RecordViewHolder holder, int position) {
        ImageRecord record = getItem(position);
        ItemRecordBinding binding = holder.binding;

        String title = TextUtils.isEmpty(record.getTitle())
                ? binding.getRoot().getContext().getString(R.string.app_name)
                : record.getTitle();
        binding.title.setText(title);

        if (TextUtils.isEmpty(record.getNote())) {
            binding.note.setText(R.string.record_no_note);
        } else {
            binding.note.setText(record.getNote());
        }

        binding.date.setText(UiUtils.formatTimestamp(record.getCreatedAt()));
        binding.thumb.setContentDescription(
                binding.getRoot().getContext().getString(
                        R.string.record_thumbnail_desc,
                        title
                )
        );

        String tag = record.getId() != null
                ? record.getId()
                : String.valueOf(record.getCreatedAt());
        binding.thumb.setTag(tag);
        binding.thumb.setImageDrawable(null);

        String imageBase64 = record.getImageBase64();
        if (!TextUtils.isEmpty(imageBase64)) {
            THUMBNAIL_EXECUTOR.execute(() -> {
                Bitmap bitmap = ImageUtils.decodeThumbnail(imageBase64, 240);
                MAIN_HANDLER.post(() -> {
                    if (!tag.equals(binding.thumb.getTag())) {
                        if (bitmap != null) {
                            bitmap.recycle();
                        }
                        return;
                    }
                    if (bitmap != null) {
                        binding.thumb.setImageBitmap(bitmap);
                    }
                });
            });
        }

        binding.card.setOnClickListener(view -> {
            int adapterPosition = holder.getBindingAdapterPosition();
            if (adapterPosition == RecyclerView.NO_POSITION) {
                return;
            }
            clickListener.onRecordClick(getItem(adapterPosition));
        });
    }

    @Override
    public void onViewRecycled(@NonNull RecordViewHolder holder) {
        holder.binding.thumb.setTag(null);
        holder.binding.thumb.setImageDrawable(null);
        super.onViewRecycled(holder);
    }

    static class RecordViewHolder extends RecyclerView.ViewHolder {
        final ItemRecordBinding binding;

        RecordViewHolder(ItemRecordBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
