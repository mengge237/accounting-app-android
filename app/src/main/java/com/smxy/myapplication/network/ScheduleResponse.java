package com.smxy.myapplication.network;

import com.smxy.myapplication.model.Schedule;
import com.google.gson.annotations.SerializedName;
import java.util.List;

public class ScheduleResponse {
    @SerializedName("success")
    private boolean success;

    @SerializedName("schedules")
    private List<Schedule> schedules;

    @SerializedName("message")
    private String message;

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public List<Schedule> getSchedules() {
        return schedules;
    }

    public void setSchedules(List<Schedule> schedules) {
        this.schedules = schedules;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}