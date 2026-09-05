package com.smxy.myapplication.network;

import com.smxy.myapplication.model.Record;
import java.util.List;

public class RecordsResponse {
    private boolean success;
    private List<Record> records;
    private String message;

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public List<Record> getRecords() { return records; }
    public void setRecords(List<Record> records) { this.records = records; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public List<Record> getData() {
        return records;
    }
}