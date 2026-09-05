package com.smxy.myapplication.model;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

public class AccountType implements Serializable {
    private int id;

    @SerializedName("type_name")
    private String typeName;

    private String category; // "income" 或 "expense"
    private String icon;

    @SerializedName("is_default")
    private boolean isDefault;

    @SerializedName("total_amount")
    private double totalAmount;

    @SerializedName("record_count")
    private int recordCount;

    public AccountType() {}

    // Getters
    public int getId() { return id; }
    public String getTypeName() { return typeName == null ? "" : typeName; }
    public String getCategory() { return category == null ? "" : category; }
    public String getIcon() { return icon == null ? "" : icon; }
    public boolean isDefault() { return isDefault; }
    public double getTotalAmount() { return totalAmount; }
    public int getRecordCount() { return recordCount; }

    // Setters
    public void setId(int id) { this.id = id; }
    public void setTypeName(String typeName) { this.typeName = typeName; }
    public void setCategory(String category) { this.category = category; }
    public void setIcon(String icon) { this.icon = icon; }
    public void setDefault(boolean isDefault) { this.isDefault = isDefault; }
    public void setTotalAmount(double totalAmount) { this.totalAmount = totalAmount; }
    public void setRecordCount(int recordCount) { this.recordCount = recordCount; }

    // 判断是否为收入
    public boolean isIncome() {
        return "income".equals(category);
    }

    // 判断是否为支出
    public boolean isExpense() {
        return "expense".equals(category);
    }

    @Override
    public String toString() {
        return "AccountType{" +
                "id=" + id +
                ", typeName='" + typeName + '\'' +
                ", category='" + category + '\'' +
                ", icon='" + icon + '\'' +
                ", isDefault=" + isDefault +
                ", totalAmount=" + totalAmount +
                ", recordCount=" + recordCount +
                '}';
    }
}