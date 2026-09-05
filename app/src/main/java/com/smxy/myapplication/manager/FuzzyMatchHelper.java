package com.smxy.myapplication.manager;

import android.util.Log;

import com.smxy.myapplication.utils.ErrorHandler;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FuzzyMatchHelper {
    private static final String TAG = "FuzzyMatchHelper";

    private static final Map<String, String> CHINESE_NUMBER_MAP = new LinkedHashMap<>();
    private static final Map<String, String> CATEGORY_KEYWORDS = new LinkedHashMap<>();
    private static final Map<String, String> PINYIN_CORRECTION = new LinkedHashMap<>();
    private static final Map<String, String> ENGLISH_INGREDIENT_MAP = new LinkedHashMap<>();
    private static final Map<String, String> PINYIN_TO_CHINESE_MAP = new LinkedHashMap<>();

    static {
        CHINESE_NUMBER_MAP.put("十五元", "15");
        CHINESE_NUMBER_MAP.put("十五块", "15");
        CHINESE_NUMBER_MAP.put("十五", "15");
        CHINESE_NUMBER_MAP.put("二十五", "25");
        CHINESE_NUMBER_MAP.put("三十五", "35");
        CHINESE_NUMBER_MAP.put("四十五", "45");
        CHINESE_NUMBER_MAP.put("五十五", "55");
        CHINESE_NUMBER_MAP.put("六十五", "65");
        CHINESE_NUMBER_MAP.put("七十五", "75");
        CHINESE_NUMBER_MAP.put("八十五", "85");
        CHINESE_NUMBER_MAP.put("九十五", "95");
        CHINESE_NUMBER_MAP.put("五成", "5");
        CHINESE_NUMBER_MAP.put("十元", "10");
        CHINESE_NUMBER_MAP.put("十块", "10");
        CHINESE_NUMBER_MAP.put("十", "10");
        CHINESE_NUMBER_MAP.put("二十", "20");
        CHINESE_NUMBER_MAP.put("三十", "30");
        CHINESE_NUMBER_MAP.put("四十", "40");
        CHINESE_NUMBER_MAP.put("五十", "50");
        CHINESE_NUMBER_MAP.put("六十", "60");
        CHINESE_NUMBER_MAP.put("七十", "70");
        CHINESE_NUMBER_MAP.put("八十", "80");
        CHINESE_NUMBER_MAP.put("九十", "90");
        CHINESE_NUMBER_MAP.put("一百", "100");
        CHINESE_NUMBER_MAP.put("一", "1");
        CHINESE_NUMBER_MAP.put("二", "2");
        CHINESE_NUMBER_MAP.put("三", "3");
        CHINESE_NUMBER_MAP.put("四", "4");
        CHINESE_NUMBER_MAP.put("五", "5");
        CHINESE_NUMBER_MAP.put("六", "6");
        CHINESE_NUMBER_MAP.put("七", "7");
        CHINESE_NUMBER_MAP.put("八", "8");
        CHINESE_NUMBER_MAP.put("九", "9");

        CATEGORY_KEYWORDS.put("午餐", "餐饮");
        CATEGORY_KEYWORDS.put("晚餐", "餐饮");
        CATEGORY_KEYWORDS.put("早饭", "餐饮");
        CATEGORY_KEYWORDS.put("早餐", "餐饮");
        CATEGORY_KEYWORDS.put("吃饭", "餐饮");
        CATEGORY_KEYWORDS.put("外卖", "餐饮");
        CATEGORY_KEYWORDS.put("餐厅", "餐饮");
        CATEGORY_KEYWORDS.put("肯德基", "餐饮");
        CATEGORY_KEYWORDS.put("麦当劳", "餐饮");
        CATEGORY_KEYWORDS.put("星巴克", "餐饮");
        CATEGORY_KEYWORDS.put("超市", "购物");
        CATEGORY_KEYWORDS.put("购物", "购物");
        CATEGORY_KEYWORDS.put("淘宝", "购物");
        CATEGORY_KEYWORDS.put("京东", "购物");
        CATEGORY_KEYWORDS.put("打车", "交通");
        CATEGORY_KEYWORDS.put("地铁", "交通");
        CATEGORY_KEYWORDS.put("公交", "交通");
        CATEGORY_KEYWORDS.put("滴滴", "交通");
        CATEGORY_KEYWORDS.put("电影", "娱乐");
        CATEGORY_KEYWORDS.put("医疗", "医疗");
        CATEGORY_KEYWORDS.put("房租", "房租");
        CATEGORY_KEYWORDS.put("水电", "水电");
        CATEGORY_KEYWORDS.put("工资", "工资");
        CATEGORY_KEYWORDS.put("奖金", "工资");

        PINYIN_CORRECTION.put("早盘", "早餐");
        PINYIN_CORRECTION.put("造成", "早餐");
        PINYIN_CORRECTION.put("早场", "早餐");
        PINYIN_CORRECTION.put("早厂", "早餐");
        PINYIN_CORRECTION.put("午饭", "午餐");
        PINYIN_CORRECTION.put("无饭", "午餐");
        PINYIN_CORRECTION.put("晚饭", "晚餐");
        PINYIN_CORRECTION.put("万饭", "晚餐");
        PINYIN_CORRECTION.put("肯德基", "肯德基");
        PINYIN_CORRECTION.put("啃得鸡", "肯德基");
        PINYIN_CORRECTION.put("麦当劳", "麦当劳");
        PINYIN_CORRECTION.put("卖的牢", "麦当劳");
        PINYIN_CORRECTION.put("超市", "超市");
        PINYIN_CORRECTION.put("超时", "超市");
        PINYIN_CORRECTION.put("吵事", "超市");
        PINYIN_CORRECTION.put("购物", "购物");
        PINYIN_CORRECTION.put("够物", "购物");
        PINYIN_CORRECTION.put("狗屋", "购物");
        PINYIN_CORRECTION.put("打车", "打车");
        PINYIN_CORRECTION.put("打扯", "打车");
        PINYIN_CORRECTION.put("大车", "打车");
        PINYIN_CORRECTION.put("地铁", "地铁");
        PINYIN_CORRECTION.put("地跌", "地铁");
        PINYIN_CORRECTION.put("地贴", "地铁");
        PINYIN_CORRECTION.put("公交", "公交");
        PINYIN_CORRECTION.put("工交", "公交");
        PINYIN_CORRECTION.put("工资", "工资");
        PINYIN_CORRECTION.put("公鸡", "工资");
        PINYIN_CORRECTION.put("公职", "工资");
        PINYIN_CORRECTION.put("奖金", "奖金");
        PINYIN_CORRECTION.put("讲金", "奖金");
        PINYIN_CORRECTION.put("一时", "10");
        PINYIN_CORRECTION.put("一事", "10");
        PINYIN_CORRECTION.put("二是", "20");
        PINYIN_CORRECTION.put("而是", "20");
        PINYIN_CORRECTION.put("三世", "30");
        PINYIN_CORRECTION.put("四是", "40");
        PINYIN_CORRECTION.put("无事", "50");
        PINYIN_CORRECTION.put("六时", "60");
        PINYIN_CORRECTION.put("七十", "70");
        PINYIN_CORRECTION.put("其事", "70");
        PINYIN_CORRECTION.put("八十", "80");
        PINYIN_CORRECTION.put("八时", "80");
        PINYIN_CORRECTION.put("九十", "90");
        PINYIN_CORRECTION.put("就是", "90");

        ENGLISH_INGREDIENT_MAP.put("potato", "土豆");
        ENGLISH_INGREDIENT_MAP.put("tomato", "番茄");
        ENGLISH_INGREDIENT_MAP.put("egg", "鸡蛋");
        ENGLISH_INGREDIENT_MAP.put("pork", "猪肉");
        ENGLISH_INGREDIENT_MAP.put("chicken", "鸡肉");
        ENGLISH_INGREDIENT_MAP.put("beef", "牛肉");
        ENGLISH_INGREDIENT_MAP.put("garlic", "大蒜");
        ENGLISH_INGREDIENT_MAP.put("ginger", "生姜");
        ENGLISH_INGREDIENT_MAP.put("onion", "洋葱");
        ENGLISH_INGREDIENT_MAP.put("salt", "盐");
        ENGLISH_INGREDIENT_MAP.put("sugar", "糖");
        ENGLISH_INGREDIENT_MAP.put("soy sauce", "酱油");
        ENGLISH_INGREDIENT_MAP.put("vinegar", "醋");
        ENGLISH_INGREDIENT_MAP.put("rice wine", "料酒");
        ENGLISH_INGREDIENT_MAP.put("starch", "淀粉");

        PINYIN_TO_CHINESE_MAP.put("tudou", "土豆");
        PINYIN_TO_CHINESE_MAP.put("fanqie", "番茄");
        PINYIN_TO_CHINESE_MAP.put("jidan", "鸡蛋");
        PINYIN_TO_CHINESE_MAP.put("zhurou", "猪肉");
        PINYIN_TO_CHINESE_MAP.put("jirou", "鸡肉");
        PINYIN_TO_CHINESE_MAP.put("niurou", "牛肉");
        PINYIN_TO_CHINESE_MAP.put("dasuan", "大蒜");
        PINYIN_TO_CHINESE_MAP.put("shengjiang", "生姜");
        PINYIN_TO_CHINESE_MAP.put("yangcong", "洋葱");
        PINYIN_TO_CHINESE_MAP.put("yan", "盐");
        PINYIN_TO_CHINESE_MAP.put("tang", "糖");
        PINYIN_TO_CHINESE_MAP.put("jiangyou", "酱油");
        PINYIN_TO_CHINESE_MAP.put("cu", "醋");
        PINYIN_TO_CHINESE_MAP.put("liaojiu", "料酒");
        PINYIN_TO_CHINESE_MAP.put("dianfen", "淀粉");
        PINYIN_TO_CHINESE_MAP.put("chao", "炒");
        PINYIN_TO_CHINESE_MAP.put("zhu", "煮");
        PINYIN_TO_CHINESE_MAP.put("dun", "炖");
        PINYIN_TO_CHINESE_MAP.put("zheng", "蒸");
        PINYIN_TO_CHINESE_MAP.put("zha", "炸");
        PINYIN_TO_CHINESE_MAP.put("jian", "煎");
        PINYIN_TO_CHINESE_MAP.put("kao", "烤");
        PINYIN_TO_CHINESE_MAP.put("men", "焖");
        PINYIN_TO_CHINESE_MAP.put("shao", "烧");
        PINYIN_TO_CHINESE_MAP.put("chuan", "川菜");
        PINYIN_TO_CHINESE_MAP.put("yue", "粤菜");
        PINYIN_TO_CHINESE_MAP.put("lu", "鲁菜");
        PINYIN_TO_CHINESE_MAP.put("su", "苏菜");
        PINYIN_TO_CHINESE_MAP.put("zhe", "浙菜");
        PINYIN_TO_CHINESE_MAP.put("min", "闽菜");
        PINYIN_TO_CHINESE_MAP.put("xiang", "湘菜");
        PINYIN_TO_CHINESE_MAP.put("hui", "徽菜");
    }

    private FuzzyMatchHelper() {
        // 私有构造函数，防止实例化
    }

    public static int levenshteinDistance(String s1, String s2) {
        if (s1 == null || s2 == null) {
            return Math.max(s1 == null ? 0 : s1.length(), s2 == null ? 0 : s2.length());
        }

        int len1 = s1.length();
        int len2 = s2.length();
        int[][] dp = new int[len1 + 1][len2 + 1];

        for (int i = 0; i <= len1; i++) dp[i][0] = i;
        for (int j = 0; j <= len2; j++) dp[0][j] = j;

        for (int i = 1; i <= len1; i++) {
            for (int j = 1; j <= len2; j++) {
                int cost = (s1.charAt(i - 1) == s2.charAt(j - 1)) ? 0 : 1;
                dp[i][j] = Math.min(Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1), dp[i - 1][j - 1] + cost);
            }
        }
        return dp[len1][len2];
    }

    public static double similarity(String s1, String s2) {
        if (s1 == null || s2 == null) return 0;
        if (s1.equals(s2)) return 1;

        int distance = levenshteinDistance(s1, s2);
        int maxLen = Math.max(s1.length(), s2.length());
        return 1.0 - (double) distance / maxLen;
    }

    public static String matchByPinyin(String text, String target) {
        if (text == null || target == null) return null;

        if (text.contains(target)) {
            return target;
        }

        for (Map.Entry<String, String> entry : PINYIN_CORRECTION.entrySet()) {
            if (entry.getValue().equals(target) && text.contains(entry.getKey())) {
                return entry.getValue();
            }
        }

        double score = similarity(text, target);
        if (score > 0.5) {
            Log.d(TAG, "拼音匹配: " + text + " -> " + target + " (相似度: " + score + ")");
            return target;
        }

        return null;
    }

    public static String recognizeEnglishIngredient(String text) {
        if (text == null) return null;
        String lowerText = text.toLowerCase().trim();

        for (Map.Entry<String, String> entry : ENGLISH_INGREDIENT_MAP.entrySet()) {
            if (lowerText.equals(entry.getKey()) || lowerText.contains(entry.getKey())) {
                Log.d(TAG, "英文食材识别: " + entry.getKey() + " -> " + entry.getValue());
                return entry.getValue();
            }
        }
        return null;
    }

    public static String recognizePinyin(String text) {
        if (text == null) return null;
        String lowerText = text.toLowerCase().trim();

        if (PINYIN_TO_CHINESE_MAP.containsKey(lowerText)) {
            Log.d(TAG, "拼音识别: " + lowerText + " -> " + PINYIN_TO_CHINESE_MAP.get(lowerText));
            return PINYIN_TO_CHINESE_MAP.get(lowerText);
        }

        for (Map.Entry<String, String> entry : PINYIN_TO_CHINESE_MAP.entrySet()) {
            if (lowerText.contains(entry.getKey()) || entry.getKey().contains(lowerText)) {
                Log.d(TAG, "拼音模糊识别: " + lowerText + " -> " + entry.getValue());
                return entry.getValue();
            }
        }
        return null;
    }

    public static String correctByPinyin(String text) {
        if (text == null || text.isEmpty()) return text;

        String corrected = text;
        for (Map.Entry<String, String> entry : PINYIN_CORRECTION.entrySet()) {
            if (corrected.contains(entry.getKey())) {
                corrected = corrected.replace(entry.getKey(), entry.getValue());
                Log.d(TAG, "拼音纠正: " + entry.getKey() + " -> " + entry.getValue());
            }
        }
        return corrected;
    }

    public static String correctText(String text) {
        if (text == null || text.isEmpty()) return text;

        String corrected = text;
        corrected = correctByPinyin(corrected);
        corrected = corrected.replaceAll("[（)\\[\\]【】？?！!，,。.]", "");

        for (Map.Entry<String, String> entry : CHINESE_NUMBER_MAP.entrySet()) {
            if (corrected.contains(entry.getKey())) {
                corrected = corrected.replace(entry.getKey(), entry.getValue());
                Log.d(TAG, "数字转换: " + entry.getKey() + " -> " + entry.getValue());
            }
        }

        corrected = mergeNumbers(corrected);
        Log.d(TAG, "纠正后文本: " + corrected);
        return corrected;
    }

    public static String correctTextEnhanced(String text) {
        if (text == null || text.isEmpty()) return text;

        String corrected = text;
        String englishIngredient = recognizeEnglishIngredient(corrected);
        if (englishIngredient != null) {
            corrected = corrected.toLowerCase().replaceFirst(
                    Pattern.quote(englishIngredient.toLowerCase()), englishIngredient);
            Log.d(TAG, "英文识别替换: -> " + corrected);
        }

        String pinyinResult = recognizePinyin(corrected);
        if (pinyinResult != null) {
            corrected = pinyinResult;
            Log.d(TAG, "拼音识别替换: -> " + corrected);
        }

        corrected = correctText(corrected);
        return corrected;
    }

    public static String smartRecognizeIngredient(String input) {
        if (input == null || input.isEmpty()) return input;

        String english = recognizeEnglishIngredient(input);
        if (english != null) return english;

        String pinyin = recognizePinyin(input);
        if (pinyin != null) return pinyin;

        for (String ingredient : ENGLISH_INGREDIENT_MAP.values()) {
            if (input.contains(ingredient)) {
                return ingredient;
            }
        }

        return input;
    }

    private static String mergeNumbers(String text) {
        Pattern pattern = Pattern.compile("(\\d+)([一二三四五六七八九十])");
        Matcher matcher = pattern.matcher(text);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            int numPart = Integer.parseInt(matcher.group(1));
            String chinesePart = matcher.group(2);
            int chineseNum = convertChineseToNumber(chinesePart);
            if (chineseNum > 0) {
                int total = numPart + chineseNum;
                matcher.appendReplacement(sb, String.valueOf(total));
                Log.d(TAG, "数字合并: " + matcher.group(0) + " -> " + total);
            } else {
                matcher.appendReplacement(sb, matcher.group(0));
            }
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    private static int convertChineseToNumber(String chinese) {
        switch (chinese) {
            case "一": return 1;
            case "二": return 2;
            case "三": return 3;
            case "四": return 4;
            case "五": return 5;
            case "六": return 6;
            case "七": return 7;
            case "八": return 8;
            case "九": return 9;
            case "十": return 10;
            default: return 0;
        }
    }

    public static double extractAmountFuzzy(String text) {
        String corrected = correctText(text);
        Pattern pattern = Pattern.compile("(\\d+(?:\\.\\d+)?)");
        Matcher matcher = pattern.matcher(corrected);
        if (matcher.find()) {
            try {
                double amount = Double.parseDouble(matcher.group(1));
                if (amount >= 0.01 && amount <= 1000000) {
                    Log.d(TAG, "提取到金额: " + amount);
                    return amount;
                }
            } catch (NumberFormatException e) {
                Log.e(TAG, "金额解析失败", e);
            }
        }
        return 0;
    }

    public static String determineCategory(String text) {
        Log.d(TAG, "确定类别 - 原始文本: " + text);

        String corrected = correctByPinyin(text);
        Log.d(TAG, "拼音纠正后: " + corrected);

        for (Map.Entry<String, String> entry : CATEGORY_KEYWORDS.entrySet()) {
            if (corrected.contains(entry.getKey())) {
                Log.d(TAG, "类别匹配成功: " + entry.getKey() + " -> " + entry.getValue());
                return entry.getValue();
            }
        }

        for (Map.Entry<String, String> entry : CATEGORY_KEYWORDS.entrySet()) {
            String matched = matchByPinyin(corrected, entry.getKey());
            if (matched != null) {
                Log.d(TAG, "拼音类别匹配: " + matched + " -> " + entry.getValue());
                return entry.getValue();
            }
        }

        if (corrected.contains("餐") || corrected.contains("饭") || corrected.contains("吃")) {
            Log.d(TAG, "模糊类别匹配: 餐饮");
            return "餐饮";
        }

        if (corrected.contains("车") || corrected.contains("打")) {
            Log.d(TAG, "模糊类别匹配: 交通");
            return "交通";
        }

        if (corrected.contains("买") || corrected.contains("超市")) {
            Log.d(TAG, "模糊类别匹配: 购物");
            return "购物";
        }

        Log.d(TAG, "未匹配到类别，返回: 其他");
        return "其他";
    }
}