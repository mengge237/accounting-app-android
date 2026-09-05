// StatisticsResponse.java
package com.smxy.myapplication.network;

public class StatisticsResponse {
    private boolean success;
    private StatisticsData statistics;
    private String message;

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    public StatisticsData getStatistics() { return statistics; }
    public void setStatistics(StatisticsData statistics) { this.statistics = statistics; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public static class StatisticsData {
        private double total_income;
        private double total_expense;
        private int income_count;
        private int expense_count;

        public double getTotal_income() { return total_income; }
        public void setTotal_income(double total_income) { this.total_income = total_income; }
        public double getTotal_expense() { return total_expense; }
        public void setTotal_expense(double total_expense) { this.total_expense = total_expense; }
        public int getIncome_count() { return income_count; }
        public void setIncome_count(int income_count) { this.income_count = income_count; }
        public int getExpense_count() { return expense_count; }
        public void setExpense_count(int expense_count) { this.expense_count = expense_count; }
    }
}