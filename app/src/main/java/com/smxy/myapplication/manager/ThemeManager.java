package com.smxy.myapplication.manager;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatDelegate;

public class ThemeManager {
    private static final String PREF_NAME = "theme_settings";
    private static final String KEY_THEME_MODE = "theme_mode";
    private static final String KEY_FONT_SIZE = "font_size";

    public static final int THEME_LIGHT = 0;
    public static final int THEME_DARK = 1;
    public static final int THEME_FOLLOW_SYSTEM = 2;

    public static final int FONT_SMALL = 0;
    public static final int FONT_MEDIUM = 1;
    public static final int FONT_LARGE = 2;
    public static final int FONT_XLARGE = 3;

    private static ThemeManager instance;
    private final SharedPreferences prefs;

    private ThemeManager(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public static synchronized ThemeManager getInstance(Context context) {
        if (context == null) {
            return null;
        }
        if (instance == null) {
            instance = new ThemeManager(context);
        }
        return instance;
    }

    public void applyTheme() {
        int themeMode = getThemeMode();
        switch (themeMode) {
            case THEME_LIGHT:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                break;
            case THEME_DARK:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                break;
            case THEME_FOLLOW_SYSTEM:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                break;
            default:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                break;
        }
    }

    public int getThemeMode() {
        return prefs.getInt(KEY_THEME_MODE, THEME_FOLLOW_SYSTEM);
    }

    public void setThemeMode(int mode) {
        prefs.edit().putInt(KEY_THEME_MODE, mode).apply();
        applyTheme();
    }

    public int getFontSize() {
        return prefs.getInt(KEY_FONT_SIZE, FONT_MEDIUM);
    }

    public void setFontSize(int size) {
        prefs.edit().putInt(KEY_FONT_SIZE, size).apply();
    }

    public float getFontScale() {
        switch (getFontSize()) {
            case FONT_SMALL:
                return 0.85f;
            case FONT_MEDIUM:
                return 1.0f;
            case FONT_LARGE:
                return 1.15f;
            case FONT_XLARGE:
                return 1.3f;
            default:
                return 1.0f;
        }
    }
}