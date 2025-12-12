package com.example.appqlct.model;

import java.io.Serializable;
import java.util.Date;

/**
 * Model class cho Notification
 * Lưu trữ thông báo của người dùng (ví dụ: cảnh báo ngân sách)
 */
public class Notification implements Serializable {
    private static final long serialVersionUID = 1L;
    private String id;
    private String userId;
    private String title;
    private String message;
    private String type; // "budget_warning", "info", etc.
    private Date createdAt;
    private boolean isRead;

    // Constructor mặc định
    public Notification() {
        this.createdAt = new Date();
        this.isRead = false;
    }

    // Constructor đầy đủ
    public Notification(String id, String userId, String title, String message, String type) {
        this.id = id;
        this.userId = userId;
        this.title = title;
        this.message = message;
        this.type = type;
        this.createdAt = new Date();
        this.isRead = false;
    }

    // Getters và Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public boolean isRead() {
        return isRead;
    }

    public void setRead(boolean read) {
        isRead = read;
    }
}

