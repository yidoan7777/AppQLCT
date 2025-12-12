package com.example.appqlct.model;

/**
 * Model class cho User trong Firestore
 * Collection: users
 */
public class User {
    private String uid;
    private String email;
    private String name;
    private String phone;
    private String avatarUrl;
    private String role; // "admin" hoặc "user"
    private double budgetLimit;
    private String gender; // "Nam", "Nữ", "Khác"
    private String dateOfBirth; // Format: "dd/MM/yyyy"

    // Constructor mặc định (cần thiết cho Firestore)
    public User() {
    }

    // Constructor đầy đủ
    public User(String uid, String email, String name, String role, double budgetLimit) {
        this.uid = uid;
        this.email = email;
        this.name = name;
        this.role = role;
        this.budgetLimit = budgetLimit;
        this.phone = "";
        this.avatarUrl = "";
        this.gender = "";
        this.dateOfBirth = "";
    }
    
    // Constructor với giới tính và ngày sinh
    public User(String uid, String email, String name, String role, double budgetLimit, String gender, String dateOfBirth) {
        this.uid = uid;
        this.email = email;
        this.name = name;
        this.role = role;
        this.budgetLimit = budgetLimit;
        this.phone = "";
        this.avatarUrl = "";
        this.gender = gender != null ? gender : "";
        this.dateOfBirth = dateOfBirth != null ? dateOfBirth : "";
    }

    // Getters và Setters
    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public double getBudgetLimit() {
        return budgetLimit;
    }

    public void setBudgetLimit(double budgetLimit) {
        this.budgetLimit = budgetLimit;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(String dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    // Kiểm tra xem user có phải admin không
    public boolean isAdmin() {
        return "admin".equals(role);
    }
}

