package com.example.appqlct.model;

/**
 * Model class cho Category trong Firestore
 * Collection: categories
 */
public class Category {
    private String id;
    private String name;
    private String icon; // Tên icon resource hoặc emoji
    private String type; // "income" hoặc "expense"

    // Constructor mặc định (cần thiết cho Firestore)
    public Category() {
    }

    // Constructor đơn giản (chỉ id và name)
    public Category(String id, String name) {
        this.id = id;
        this.name = name;
        this.icon = "";
        this.type = "expense";
    }

    // Constructor đầy đủ
    public Category(String id, String name, String icon, String type) {
        this.id = id;
        this.name = name;
        this.icon = icon;
        this.type = type;
    }

    // Getters và Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}

