package com.smxy.myapplication.model;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;

public class LedgerBook implements Serializable {
    private int id;
    private int userId;
    private String name;
    private String icon;
    private String color;
    private boolean isDefault;
    private String createdAt;
    private String updatedAt;

    public LedgerBook() {}

    public LedgerBook(String name, String color) {
        this.name = name;
        this.color = color;
        this.isDefault = false;
    }

    // Getters
    public int getId() { return id; }
    public int getUserId() { return userId; }
    public String getName() { return name; }
    public String getIcon() { return ""; }  // 不再使用icon
    public String getColor() { return color != null && !color.isEmpty() ? color : "#4CAF50"; }
    public boolean isDefault() { return isDefault; }
    public String getCreatedAt() { return createdAt; }
    public String getUpdatedAt() { return updatedAt; }

    // Setters
    public void setId(int id) { this.id = id; }
    public void setUserId(int userId) { this.userId = userId; }
    public void setName(String name) { this.name = name; }
    public void setIcon(String icon) { this.icon = icon; }
    public void setColor(String color) { this.color = color; }
    public void setDefault(boolean aDefault) { isDefault = aDefault; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }

    @Override
    public String toString() {
        return "LedgerBook{" +
                "id=" + id +
                ", userId=" + userId +
                ", name='" + name + '\'' +
                ", color='" + color + '\'' +
                ", isDefault=" + isDefault +
                '}';
    }
}