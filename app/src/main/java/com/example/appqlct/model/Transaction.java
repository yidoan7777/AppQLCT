package com.example.appqlct.model;

import java.io.Serializable;
import java.util.Date;

/**
 * Model class cho Transaction trong Firestore
 * Collection: transactions
 */
public class Transaction implements Serializable {
    private static final long serialVersionUID = 1L;
    private String id;
    private String userId;
    private double amount;
    private String category;
    private String note;
    private Date date;
    private String type; // "income" hoặc "expense"
    private boolean isRecurring; // Chi tiêu định kỳ
    private Date recurringStartMonth; // Tháng bắt đầu định kỳ
    private Date recurringEndMonth; // Tháng kết thúc định kỳ
    private String recurringTransactionId; // ID của giao dịch định kỳ gốc (nếu giao dịch này được tạo từ định kỳ)

    // Constructor mặc định (cần thiết cho Firestore)
    public Transaction() {
        this.isRecurring = false; // Mặc định là false
    }

    // Constructor đầy đủ
    public Transaction(String id, String userId, double amount, String category, 
                      String note, Date date, String type) {
        this.id = id;
        this.userId = userId;
        this.amount = amount;
        this.category = category;
        this.note = note;
        this.date = date;
        this.type = type;
        this.isRecurring = false; // Mặc định là false
    }
    
    // Constructor đầy đủ với isRecurring
    public Transaction(String id, String userId, double amount, String category, 
                      String note, Date date, String type, boolean isRecurring) {
        this.id = id;
        this.userId = userId;
        this.amount = amount;
        this.category = category;
        this.note = note;
        this.date = date;
        this.type = type;
        this.isRecurring = isRecurring;
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

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    // Kiểm tra xem có phải thu nhập không
    public boolean isIncome() {
        return "income".equals(type);
    }

    public boolean isRecurring() {
        return isRecurring;
    }

    public void setRecurring(boolean recurring) {
        isRecurring = recurring;
    }

    public Date getRecurringStartMonth() {
        return recurringStartMonth;
    }

    public void setRecurringStartMonth(Date recurringStartMonth) {
        this.recurringStartMonth = recurringStartMonth;
    }

    public Date getRecurringEndMonth() {
        return recurringEndMonth;
    }

    public void setRecurringEndMonth(Date recurringEndMonth) {
        this.recurringEndMonth = recurringEndMonth;
    }

    public String getRecurringTransactionId() {
        return recurringTransactionId;
    }

    public void setRecurringTransactionId(String recurringTransactionId) {
        this.recurringTransactionId = recurringTransactionId;
    }
}

