package com.smxy.myapplication.network;

import com.smxy.myapplication.model.AccountType;

import java.util.List;

public class CategoryStatisticsResponse {
    private boolean success;
    private List<AccountType> categories;
    private String message;

    public boolean isSuccess() { return success; }
    public List<AccountType> getCategories() { return categories; }
    public String getMessage() { return message; }

    public void setSuccess(boolean success) { this.success = success; }
    public void setCategories(List<AccountType> categories) { this.categories = categories; }
    public void setMessage(String message) { this.message = message; }
}