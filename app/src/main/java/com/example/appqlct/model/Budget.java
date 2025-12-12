package com.example.appqlct.model;

import java.util.Date;

/**
 * Model class cho Budget trong Firestore
 * Collection: budgets
 * Mỗi user có thể có nhiều ngân sách cho các category khác nhau
 */
public class Budget {
    private String id;
    private String userId;
    private String categoryName; // Tên category (hoặc "Tổng thể" cho ngân sách tổng)
    private double amount; // Số tiền ngân sách
    private int month; // Tháng (1-12)
    private int year; // Năm
    private Date createdAt; // Ngày tạo
    private Date updatedAt; // Ngày cập nhật

    // Constructor mặc định (cần thiết cho Firestore)
    public Budget() {
    }

    // Constructor đầy đủ
    public Budget(String id, String userId, String categoryName, double amount, 
                  int month, int year) {
        this.id = id;
        this.userId = userId;
        this.categoryName = categoryName;
        this.amount = amount;
        this.month = month;
        this.year = year;
        this.createdAt = new Date();
        this.updatedAt = new Date();
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

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public int getMonth() {
        return month;
    }

    public void setMonth(int month) {
        this.month = month;
    }

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }

    /**
     * Kiểm tra xem ngân sách có phải cho tháng hiện tại không
     */
    public boolean isCurrentMonth() {
        java.util.Calendar cal = java.util.Calendar.getInstance();
        return this.month == (cal.get(java.util.Calendar.MONTH) + 1) 
            && this.year == cal.get(java.util.Calendar.YEAR);
    }
}

