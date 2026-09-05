// MonthlyStatisticsResponse.java
package com.smxy.myapplication.network;

import java.util.List;

public class MonthlyStatisticsResponse {
    private boolean success;
    private List<MonthlyStat> monthly_stats;
    private String message;

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    public List<MonthlyStat> getMonthly_stats() { return monthly_stats; }
    public void setMonthly_stats(List<MonthlyStat> monthly_stats) { this.monthly_stats = monthly_stats; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public static class MonthlyStat {
        private int month;
        private double total_income;
        private double total_expense;

        public int getMonth() { return month; }
        public void setMonth(int month) { this.month = month; }
        public double getTotal_income() { return total_income; }
        public void setTotal_income(double total_income) { this.total_income = total_income; }
        public double getTotal_expense() { return total_expense; }
        public void setTotal_expense(double total_expense) { this.total_expense = total_expense; }
    }
}