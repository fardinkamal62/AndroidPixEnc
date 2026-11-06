package com.fardinkamal.androidpixenc.data;

import com.google.firebase.database.Exclude;

/**
 * One encrypted image record stored under users/{uid}/records/{recordId}.
 */
public class ImageRecord {

    private String title;
    private String note;
    private long createdAt;
    private String salt;
    private String imageBase64;
    private int width;
    private int height;

    @Exclude
    private String id;

    public ImageRecord() {
        // Required by Firebase Realtime Database deserialization.
    }

    public ImageRecord(String title, String note, long createdAt, String salt,
                       String imageBase64, int width, int height) {
        this.title = title;
        this.note = note;
        this.createdAt = createdAt;
        this.salt = salt;
        this.imageBase64 = imageBase64;
        this.width = width;
        this.height = height;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public String getSalt() {
        return salt;
    }

    public void setSalt(String salt) {
        this.salt = salt;
    }

    public String getImageBase64() {
        return imageBase64;
    }

    public void setImageBase64(String imageBase64) {
        this.imageBase64 = imageBase64;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    @Exclude
    public String getId() {
        return id;
    }

    @Exclude
    public void setId(String id) {
        this.id = id;
    }
}
