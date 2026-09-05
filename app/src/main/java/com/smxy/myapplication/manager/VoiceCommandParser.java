package com.smxy.myapplication.manager;

import android.util.Log;

import com.smxy.myapplication.utils.ErrorHandler;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class VoiceCommandParser {

    private static final String TAG = "VoiceCommandParser";

    private static final String[] KEYWORDS = {
            "午餐", "晚餐", "早餐", "早饭", "吃饭", "外卖", "餐厅",
            "超市", "购物", "淘宝", "京东",
            "打车", "地铁", "公交", "滴滴",
            "电影", "游戏", "娱乐",
            "医疗", "医院", "药",
            "房租", "水电", "工资", "奖金", "收入"
    };

    private VoiceCommandParser() {
        // 私有构造函数
    }

    public static class ParseResult {
        public double amount;
        public String category;
        public int type;
        public String note;
        public String date;

        public ParseResult() {
            this.date = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
            this.type = 0;
            this.category = "其他";
            this.amount = 0;
            this.note = "";
        }

        public boolean isValid() {
            return amount > 0 || (note != null && !note.isEmpty() && !note.equals("未识别到内容"));
        }

        @Override
        public String toString() {
            return String.format(Locale.CHINA, "金额:%.2f, 类别:%s, 类型:%d, 备注:%s, 日期:%s",
                    amount, category, type, note, date);
        }
    }

    public static ParseResult parse(String text) {
        ParseResult result = new ParseResult();

        if (text == null || text.isEmpty()) {
            result.note = "未识别到内容";
            return result;
        }

        Log.d(TAG, "开始解析: " + text);

        result.amount = FuzzyMatchHelper.extractAmountFuzzy(text);
        result.category = FuzzyMatchHelper.determineCategory(text);

        if (text.contains("收入") || text.contains("赚") || text.contains("工资") || text.contains("奖金")) {
            result.type = 1;
            Log.d(TAG, "类型匹配: 收入");
        } else {
            result.type = 0;
            Log.d(TAG, "类型匹配: 支出");
        }

        result.note = extractKeywordNote(text, result.category);

        Log.d(TAG, "最终解析结果: 金额=" + result.amount + ", 类别=" + result.category + ", 备注=" + result.note);

        return result;
    }

    private static String extractKeywordNote(String text, String category) {
        if (text == null || text.isEmpty()) return "语音记账";

        for (String keyword : KEYWORDS) {
            if (text.contains(keyword)) {
                Log.d(TAG, "提取关键词: " + keyword);
                return keyword;
            }
        }

        if (category.equals("餐饮")) {
            if (text.contains("午餐") || text.contains("午")) return "午餐";
            if (text.contains("晚餐") || text.contains("晚")) return "晚餐";
            if (text.contains("早餐") || text.contains("早")) return "早餐";
            if (text.contains("吃饭") || text.contains("吃")) return "吃饭";
            if (text.contains("外卖")) return "外卖";
        }

        if (category.equals("交通")) {
            if (text.contains("打车")) return "打车";
            if (text.contains("地铁")) return "地铁";
            if (text.contains("公交") || text.contains("公共汽车")) return "公交";
            if (text.contains("滴滴")) return "滴滴";
        }

        if (category.equals("购物")) {
            if (text.contains("超市")) return "超市";
            if (text.contains("购物")) return "购物";
            if (text.contains("淘宝")) return "淘宝";
            if (text.contains("京东")) return "京东";
        }

        if (category.equals("娱乐")) {
            if (text.contains("电影")) return "电影";
            if (text.contains("游戏")) return "游戏";
        }

        if (category.equals("医疗")) {
            if (text.contains("医疗") || text.contains("医院")) return "医疗";
            if (text.contains("药")) return "买药";
        }

        if (!category.equals("其他")) {
            return category;
        }

        String cleanNote = text.replaceAll("\\d+", "");
        cleanNote = cleanNote.replaceAll("\\s+", " ").trim();
        if (cleanNote.length() > 10) {
            cleanNote = cleanNote.substring(0, 10);
        }
        if (cleanNote.isEmpty()) {
            cleanNote = "语音记账";
        }

        return cleanNote;
    }
}