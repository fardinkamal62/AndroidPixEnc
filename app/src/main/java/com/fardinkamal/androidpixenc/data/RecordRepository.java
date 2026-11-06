package com.fardinkamal.androidpixenc.data;

import androidx.annotation.NonNull;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RecordRepository {

    public interface Completion {
        void onSuccess();

        void onError(Exception error);
    }

    public interface RecordsCallback {
        void onData(List<ImageRecord> records);

        void onError(Exception error);
    }

    public interface RecordCallback {
        void onData(ImageRecord record);

        void onError(Exception error);
    }

    private Query activeQuery;
    private ValueEventListener activeListener;

    public void createRecord(ImageRecord record, Completion callback) {
        DatabaseReference records = recordsRef();
        if (records == null) {
            callback.onError(new IllegalStateException("Not signed in"));
            return;
        }

        String key = records.push().getKey();
        if (key == null) {
            callback.onError(new IllegalStateException("Could not create record key"));
            return;
        }

        if (record.getCreatedAt() <= 0L) {
            record.setCreatedAt(System.currentTimeMillis());
        }

        records.child(key).setValue(record)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        record.setId(key);
                        callback.onSuccess();
                    } else {
                        callback.onError(asException(task.getException()));
                    }
                });
    }

    public void observeRecords(RecordsCallback callback) {
        stopObserving();

        DatabaseReference records = recordsRef();
        if (records == null) {
            callback.onError(new IllegalStateException("Not signed in"));
            return;
        }

        Query query = records.orderByChild("createdAt");
        ValueEventListener listener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<ImageRecord> result = new ArrayList<>();
                for (DataSnapshot child : snapshot.getChildren()) {
                    try {
                        ImageRecord record = child.getValue(ImageRecord.class);
                        if (record != null) {
                            record.setId(child.getKey());
                            result.add(record);
                        }
                    } catch (RuntimeException ignored) {
                        // Skip a malformed entry rather than breaking the whole list.
                    }
                }
                Collections.sort(result, (left, right) ->
                        Long.compare(right.getCreatedAt(), left.getCreatedAt()));
                callback.onData(result);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                callback.onError(error.toException());
            }
        };

        activeQuery = query;
        activeListener = listener;
        query.addValueEventListener(listener);
    }

    public void stopObserving() {
        if (activeQuery != null && activeListener != null) {
            activeQuery.removeEventListener(activeListener);
        }
        activeQuery = null;
        activeListener = null;
    }

    public void getRecord(String recordId, RecordCallback callback) {
        DatabaseReference records = recordsRef();
        if (records == null) {
            callback.onError(new IllegalStateException("Not signed in"));
            return;
        }

        records.child(recordId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                ImageRecord record = snapshot.getValue(ImageRecord.class);
                if (record == null) {
                    callback.onError(new IllegalStateException("Record not found"));
                    return;
                }
                record.setId(snapshot.getKey());
                callback.onData(record);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                callback.onError(error.toException());
            }
        });
    }

    public void updateDetails(String recordId, String title, String note, Completion callback) {
        DatabaseReference records = recordsRef();
        if (records == null) {
            callback.onError(new IllegalStateException("Not signed in"));
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("title", title);
        updates.put("note", note);
        records.child(recordId).updateChildren(updates)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        callback.onSuccess();
                    } else {
                        callback.onError(asException(task.getException()));
                    }
                });
    }

    public void deleteRecord(String recordId, Completion callback) {
        DatabaseReference records = recordsRef();
        if (records == null) {
            callback.onError(new IllegalStateException("Not signed in"));
            return;
        }

        records.child(recordId).removeValue()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        callback.onSuccess();
                    } else {
                        callback.onError(asException(task.getException()));
                    }
                });
    }

    private DatabaseReference recordsRef() {
        try {
            String uid = FirebaseAuth.getInstance().getCurrentUser() == null
                    ? null
                    : FirebaseAuth.getInstance().getCurrentUser().getUid();
            if (uid == null || uid.isEmpty()) {
                return null;
            }
            return FirebaseDatabase.getInstance()
                    .getReference("users")
                    .child(uid)
                    .child("records");
        } catch (Throwable ignored) {
            return null;
        }
    }

    private Exception asException(Exception exception) {
        return exception == null
                ? new IllegalStateException("Database operation failed")
                : exception;
    }
}
