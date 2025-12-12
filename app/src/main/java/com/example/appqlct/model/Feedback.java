package com.example.appqlct.model;

import java.util.Date;

/**
 * Model class cho Feedback trong Firestore
 * Collection: feedback
 */
public class Feedback {
    private String id;
    private String userId;
    private float rating;
    private String content;
    private Date date;
    private String status; // "pending", "read", "resolved"

    // Constructor mặc định (cần thiết cho Firestore)
    public Feedback() {
    }

    // Constructor đầy đủ
    public Feedback(String id, String userId, float rating, String content, Date date) {
        this.id = id;
        this.userId = userId;
        this.rating = rating;
        this.content = content;
        this.date = date;
        this.status = "pending";
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

    public float getRating() {
        return rating;
    }

    public void setRating(float rating) {
        this.rating = rating;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}

