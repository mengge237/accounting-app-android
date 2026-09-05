package com.smxy.myapplication.model;

import com.google.gson.annotations.SerializedName;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class Record {
    private int id;

    @SerializedName("user_id")
    private int userId;

    @SerializedName("type_id")
    private int typeId;

    private double amount;
    private String note;

    @SerializedName("record_date")
    private String recordDate;

    @SerializedName("created_at")
    private String createdAt;

    @SerializedName("type_name")
    private String typeName;

    private String category;
    private String icon;

    public Record() {}

    // ========== Getters ==========
    public int getId() {
        return id;
    }

    public int getUserId() {
        return userId;
    }

    public int getTypeId() {
        return typeId;
    }

    public double getAmount() {
        return amount;
    }

    public String getNote() {
        return note == null ? "" : note;
    }

    public String getRecordDate() {
        return recordDate == null ? "" : recordDate;
    }

    public String getCreatedAt() {
        return createdAt == null ? "" : createdAt;
    }

    public String getTypeName() {
        return typeName == null ? "" : typeName;
    }

    public String getCategory() {
        return category == null ? "" : category;
    }

    public String getIcon() {
        return icon == null ? "" : icon;
    }

    // ========== Setters ==========
    public void setId(int id) {
        this.id = id;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public void setTypeId(int typeId) {
        this.typeId = typeId;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public void setRecordDate(String recordDate) {
        this.recordDate = recordDate;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public void setTypeName(String typeName) {
        this.typeName = typeName;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    // ========== 日期处理方法 ==========

    /**
     * 获取记录日期的毫秒时间戳
     * 用于图表显示和日期筛选
     *
     * @return 时间戳（毫秒）
     */
    public long getTimestamp() {
        if (recordDate == null || recordDate.isEmpty()) {
            return System.currentTimeMillis();
        }

        try {
            // 数据库存储的是 date 类型，格式通常是 yyyy-MM-dd
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.CHINA);
            Date date = dateFormat.parse(recordDate);
            if (date != null) {
                return date.getTime();
            }
        } catch (ParseException e) {
            e.printStackTrace();

            // 尝试其他常见格式
            try {
                SimpleDateFormat dateTimeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA);
                Date date = dateTimeFormat.parse(recordDate);
                if (date != null) {
                    return date.getTime();
                }
            } catch (ParseException e2) {
                e2.printStackTrace();
            }
        }

        return System.currentTimeMillis();
    }

    /**
     * 获取格式化的日期字符串
     *
     * @param pattern 日期格式，如 "yyyy-MM-dd" 或 "MM月dd日"
     * @return 格式化后的日期字符串
     */
    public String getFormattedDate(String pattern) {
        if (recordDate == null || recordDate.isEmpty()) {
            return "";
        }

        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.CHINA);
            Date date = inputFormat.parse(recordDate);
            if (date != null) {
                SimpleDateFormat outputFormat = new SimpleDateFormat(pattern, Locale.CHINA);
                return outputFormat.format(date);
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }

        return recordDate;
    }

    /**
     * 获取显示的日期（默认格式：yyyy-MM-dd）
     */
    public String getDisplayDate() {
        return getFormattedDate("yyyy-MM-dd");
    }

    /**
     * 获取年月字符串（用于月度统计）
     *
     * @return 格式：2024-01
     */
    public String getYearMonth() {
        return getFormattedDate("yyyy-MM");
    }

    /**
     * 获取年份（用于年度统计）
     *
     * @return 格式：2024
     */
    public String getYear() {
        return getFormattedDate("yyyy");
    }

    /**
     * 获取月份（用于月度统计）
     *
     * @return 格式：01
     */
    public String getMonth() {
        return getFormattedDate("MM");
    }

    /**
     * 判断是否在指定日期范围内
     *
     * @param startTime 开始时间戳（毫秒）
     * @param endTime 结束时间戳（毫秒）
     * @return 是否在范围内
     */
    public boolean isInDateRange(long startTime, long endTime) {
        long timestamp = getTimestamp();
        return timestamp >= startTime && timestamp <= endTime;
    }

    /**
     * 判断是否是指定月份
     *
     * @param year 年份
     * @param month 月份（0-11）
     * @return 是否是该月
     */
    public boolean isInMonth(int year, int month) {
        String targetYearMonth = String.format(Locale.CHINA, "%d-%02d", year, month + 1);
        String recordYearMonth = getYearMonth();
        return targetYearMonth.equals(recordYearMonth);
    }

    /**
     * 判断是否是指定年份
     *
     * @param year 年份
     * @return 是否是该年
     */
    public boolean isInYear(int year) {
        String targetYear = String.valueOf(year);
        String recordYear = getYear();
        return targetYear.equals(recordYear);
    }

    /**
     * 获取收支类型（中文）
     *
     * @return "收入" 或 "支出"
     */
    public String getCategoryName() {
        if (category == null) return "未知";
        if (category.equals("income")) return "收入";
        if (category.equals("expense")) return "支出";
        return category;
    }

    /**
     * 判断是否为收入
     */
    public boolean isIncome() {
        return "income".equals(category);
    }

    /**
     * 判断是否为支出
     */
    public boolean isExpense() {
        return "expense".equals(category);
    }

    /**
     * 获取带符号的金额字符串
     *
     * @return 收入返回 "+¥X.XX"，支出返回 "-¥X.XX"
     */
    public String getSignedAmount() {
        if (isIncome()) {
            return String.format(Locale.CHINA, "+¥%.2f", amount);
        } else if (isExpense()) {
            return String.format(Locale.CHINA, "-¥%.2f", amount);
        }
        return String.format(Locale.CHINA, "¥%.2f", amount);
    }

    @Override
    public String toString() {
        return "Record{" +
                "id=" + id +
                ", userId=" + userId +
                ", typeId=" + typeId +
                ", amount=" + amount +
                ", note='" + note + '\'' +
                ", recordDate='" + recordDate + '\'' +
                ", createdAt='" + createdAt + '\'' +
                ", typeName='" + typeName + '\'' +
                ", category='" + category + '\'' +
                ", icon='" + icon + '\'' +
                '}';
    }
}