package com.smxy.myapplication.model;

import com.google.gson.annotations.SerializedName;

public class Schedule {
    @SerializedName("id")
    private int id;

    @SerializedName("user_id")
    private int userId;

    @SerializedName("title")
    private String title;

    @SerializedName("type")
    private String type;

    @SerializedName("content")
    private String content;

    @SerializedName("related_id")
    private int relatedId;

    @SerializedName("scheduled_date")
    private String scheduledDate;

    @SerializedName("scheduled_time")
    private String scheduledTime;

    @SerializedName("priority")
    private int priority;

    @SerializedName("is_completed")
    private int isCompleted;  // 改为 int 类型，0=未完成，1=已完成

    @SerializedName("reminder_minutes")
    private int reminderMinutes;

    @SerializedName("repeat_type")
    private String repeatType;

    @SerializedName("note")
    private String note;

    @SerializedName("created_at")
    private String createdAt;

    @SerializedName("updated_at")
    private String updatedAt;

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public int getRelatedId() { return relatedId; }
    public void setRelatedId(int relatedId) { this.relatedId = relatedId; }

    public String getScheduledDate() {
        // 处理 ISO 格式日期 (2026-05-14T16:00:00.000Z -> 2026-05-14)
        if (scheduledDate != null && scheduledDate.contains("T")) {
            return scheduledDate.substring(0, 10);
        }
        return scheduledDate;
    }
    public void setScheduledDate(String scheduledDate) { this.scheduledDate = scheduledDate; }

    public String getScheduledTime() {
        // 处理时间格式，只返回 HH:MM
        if (scheduledTime != null && scheduledTime.length() > 5) {
            return scheduledTime.substring(0, 5);
        }
        return scheduledTime;
    }
    public void setScheduledTime(String scheduledTime) { this.scheduledTime = scheduledTime; }

    public int getPriority() { return priority; }
    public void setPriority(int priority) { this.priority = priority; }

    public boolean isCompleted() { return isCompleted == 1; }
    public void setCompleted(boolean completed) { this.isCompleted = completed ? 1 : 0; }

    public int getReminderMinutes() { return reminderMinutes; }
    public void setReminderMinutes(int reminderMinutes) { this.reminderMinutes = reminderMinutes; }

    public String getRepeatType() { return repeatType; }
    public void setRepeatType(String repeatType) { this.repeatType = repeatType; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }

    // 获取优先级文字
    public String getPriorityText() {
        switch (priority) {
            case 1: return "低";
            case 2: return "中";
            case 3: return "高";
            default: return "中";
        }
    }

    // 获取类型文字
    public String getTypeText() {
        if (type == null) return "事件";
        switch (type) {
            case "recipe": return "做菜";
            case "event": return "事件";
            case "salary": return "工资";
            case "bill": return "账单";
            case "reminder": return "提醒";
            default: return "事件";
        }
    }
}