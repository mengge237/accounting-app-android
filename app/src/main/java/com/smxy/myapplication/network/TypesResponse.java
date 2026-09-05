package com.smxy.myapplication.network;

import com.smxy.myapplication.model.AccountType;

import java.util.List;

public class TypesResponse {
    private boolean success;
    private List<AccountType> types;
    private String message;

    public boolean isSuccess() { return success; }
    public List<AccountType> getTypes() { return types; }
    public String getMessage() { return message; }

    public void setSuccess(boolean success) { this.success = success; }
    public void setTypes(List<AccountType> types) { this.types = types; }
    public void setMessage(String message) { this.message = message; }
}