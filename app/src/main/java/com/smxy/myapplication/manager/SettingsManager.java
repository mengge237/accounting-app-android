package com.smxy.myapplication.manager;

import android.content.Context;
import android.content.SharedPreferences;

public class SettingsManager {
    private static final String PREF_NAME = "app_settings";
    private static final String KEY_CHEAT_MODE = "cheat_mode";
    private static final String KEY_SHOW_ACCOUNTING = "show_accounting";
    private static final String KEY_SHOW_LEDGER_MANAGE = "show_ledger_manage";
    private static final String KEY_SHOW_STATISTICS = "show_statistics";
    private static final String KEY_SHOW_RECORDS = "show_records";
    private static final String KEY_SHOW_VOICE = "show_voice";
    private static final String KEY_SHOW_RECIPE = "show_recipe";
    private static final String KEY_SHOW_MY_RECIPES = "show_my_recipes";
    private static final String KEY_SHOW_SCHEDULE = "show_schedule";
    private static final String KEY_SHOW_SETTINGS = "show_settings";
    private static final String KEY_SHOW_THEME = "show_theme";
    private static final String KEY_SHOW_EXPORT = "show_export";
    private static final String KEY_SHOW_ABOUT = "show_about";
    private static final String KEY_SHOW_LOGOUT = "show_logout";
    private static final String KEY_SHOW_COMPONENT_SETTINGS = "show_component_settings";
    private static final String KEY_SHOW_NOTES = "show_notes";
    private static final String KEY_SHOW_MUSIC = "show_music";
    private static final String KEY_SHOW_STOCK = "show_stock";
    private static final String KEY_NAVIGATION_MODE = "navigation_mode";

    public static final int NAV_MODE_SIDEBAR = 0;
    public static final int NAV_MODE_BOTTOM = 1;
    public static final int NAV_MODE_BOTH = 2;

    private final SharedPreferences prefs;

    public SettingsManager(Context context) {
        this.prefs = context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public boolean isCheatModeEnabled() {
        return prefs.getBoolean(KEY_CHEAT_MODE, false);
    }

    public void setCheatModeEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_CHEAT_MODE, enabled).apply();
    }

    public boolean showAccounting() {
        return prefs.getBoolean(KEY_SHOW_ACCOUNTING, true);
    }

    public void setShowAccounting(boolean show) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(KEY_SHOW_ACCOUNTING, show);

        if (!show) {
            editor.putBoolean(KEY_SHOW_LEDGER_MANAGE, false);
            editor.putBoolean(KEY_SHOW_STATISTICS, false);
            editor.putBoolean(KEY_SHOW_RECORDS, false);
            editor.putBoolean(KEY_SHOW_COMPONENT_SETTINGS, false);
            editor.putBoolean(KEY_SHOW_EXPORT, false);
        } else {
            editor.putBoolean(KEY_SHOW_LEDGER_MANAGE, true);
            editor.putBoolean(KEY_SHOW_STATISTICS, true);
            editor.putBoolean(KEY_SHOW_RECORDS, true);
            editor.putBoolean(KEY_SHOW_COMPONENT_SETTINGS, true);
            editor.putBoolean(KEY_SHOW_EXPORT, true);
        }
        editor.apply();
    }

    public boolean showLedgerManage() {
        boolean accountingEnabled = showAccounting();
        boolean userSet = prefs.getBoolean(KEY_SHOW_LEDGER_MANAGE, true);
        return accountingEnabled && userSet;
    }

    public void setShowLedgerManage(boolean show) {
        if (show && !showAccounting()) {
            return;
        }
        prefs.edit().putBoolean(KEY_SHOW_LEDGER_MANAGE, show).apply();
    }

    public boolean showStatistics() {
        boolean accountingEnabled = showAccounting();
        boolean userSet = prefs.getBoolean(KEY_SHOW_STATISTICS, true);
        return accountingEnabled && userSet;
    }

    public void setShowStatistics(boolean show) {
        if (show && !showAccounting()) {
            return;
        }
        prefs.edit().putBoolean(KEY_SHOW_STATISTICS, show).apply();
    }

    public boolean showRecords() {
        boolean accountingEnabled = showAccounting();
        boolean userSet = prefs.getBoolean(KEY_SHOW_RECORDS, true);
        return accountingEnabled && userSet;
    }

    public void setShowRecords(boolean show) {
        if (show && !showAccounting()) {
            return;
        }
        prefs.edit().putBoolean(KEY_SHOW_RECORDS, show).apply();
    }

    public boolean showVoice() {
        return prefs.getBoolean(KEY_SHOW_VOICE, true);
    }

    public void setShowVoice(boolean show) {
        prefs.edit().putBoolean(KEY_SHOW_VOICE, show).apply();
    }

    public boolean showRecipe() {
        return prefs.getBoolean(KEY_SHOW_RECIPE, true);
    }

    public void setShowRecipe(boolean show) {
        prefs.edit().putBoolean(KEY_SHOW_RECIPE, show).apply();
    }

    public boolean showMyRecipes() {
        return prefs.getBoolean(KEY_SHOW_MY_RECIPES, true);
    }

    public void setShowMyRecipes(boolean show) {
        prefs.edit().putBoolean(KEY_SHOW_MY_RECIPES, show).apply();
    }

    public boolean showSchedule() {
        return prefs.getBoolean(KEY_SHOW_SCHEDULE, true);
    }

    public void setShowSchedule(boolean show) {
        prefs.edit().putBoolean(KEY_SHOW_SCHEDULE, show).apply();
    }

    public boolean showNotes() {
        return prefs.getBoolean(KEY_SHOW_NOTES, true);
    }

    public void setShowNotes(boolean show) {
        prefs.edit().putBoolean(KEY_SHOW_NOTES, show).apply();
    }

    public boolean showMusic() {
        return prefs.getBoolean(KEY_SHOW_MUSIC, false);
    }

    public void setShowMusic(boolean show) {
        prefs.edit().putBoolean(KEY_SHOW_MUSIC, show).apply();
    }

    public boolean showStock() {
        return prefs.getBoolean(KEY_SHOW_STOCK, false);
    }

    public void setShowStock(boolean show) {
        prefs.edit().putBoolean(KEY_SHOW_STOCK, show).apply();
    }

    public int getNavigationMode() {
        return prefs.getInt(KEY_NAVIGATION_MODE, NAV_MODE_SIDEBAR);
    }

    public void setNavigationMode(int mode) {
        if (mode >= NAV_MODE_SIDEBAR && mode <= NAV_MODE_BOTH) {
            prefs.edit().putInt(KEY_NAVIGATION_MODE, mode).apply();
        }
    }

    public boolean isSidebarEnabled() {
        int mode = getNavigationMode();
        return mode == NAV_MODE_SIDEBAR || mode == NAV_MODE_BOTH;
    }

    public boolean isBottomNavEnabled() {
        int mode = getNavigationMode();
        return mode == NAV_MODE_BOTTOM || mode == NAV_MODE_BOTH;
    }

    public boolean showSettings() {
        return true;
    }

    public void setShowSettings(boolean show) {
    }

    public boolean showComponentSettings() {
        boolean accountingEnabled = showAccounting();
        boolean userSet = prefs.getBoolean(KEY_SHOW_COMPONENT_SETTINGS, true);
        return accountingEnabled && userSet;
    }

    public void setShowComponentSettings(boolean show) {
        if (show && !showAccounting()) {
            return;
        }
        prefs.edit().putBoolean(KEY_SHOW_COMPONENT_SETTINGS, show).apply();
    }

    public boolean showTheme() {
        return prefs.getBoolean(KEY_SHOW_THEME, true);
    }

    public void setShowTheme(boolean show) {
        prefs.edit().putBoolean(KEY_SHOW_THEME, show).apply();
    }

    public boolean showExport() {
        boolean accountingEnabled = showAccounting();
        boolean userSet = prefs.getBoolean(KEY_SHOW_EXPORT, true);
        return accountingEnabled && userSet;
    }

    public void setShowExport(boolean show) {
        if (show && !showAccounting()) {
            return;
        }
        prefs.edit().putBoolean(KEY_SHOW_EXPORT, show).apply();
    }

    public boolean showAbout() {
        return true;
    }

    public void setShowAbout(boolean show) {
    }

    public boolean showLogout() {
        return true;
    }

    public void setShowLogout(boolean show) {
    }

    // 获取用户设置的实际值（用于UI显示）
    public boolean getUserSettingShowLedgerManage() {
        return prefs.getBoolean(KEY_SHOW_LEDGER_MANAGE, true);
    }

    public boolean getUserSettingShowStatistics() {
        return prefs.getBoolean(KEY_SHOW_STATISTICS, true);
    }

    public boolean getUserSettingShowRecords() {
        return prefs.getBoolean(KEY_SHOW_RECORDS, true);
    }

    public boolean getUserSettingShowComponentSettings() {
        return prefs.getBoolean(KEY_SHOW_COMPONENT_SETTINGS, true);
    }

    public boolean getUserSettingShowExport() {
        return prefs.getBoolean(KEY_SHOW_EXPORT, true);
    }

    public void resetToDefault() {
        prefs.edit()
                .putBoolean(KEY_CHEAT_MODE, false)
                .putBoolean(KEY_SHOW_ACCOUNTING, true)
                .putBoolean(KEY_SHOW_LEDGER_MANAGE, true)
                .putBoolean(KEY_SHOW_STATISTICS, true)
                .putBoolean(KEY_SHOW_RECORDS, true)
                .putBoolean(KEY_SHOW_VOICE, true)
                .putBoolean(KEY_SHOW_RECIPE, true)
                .putBoolean(KEY_SHOW_MY_RECIPES, true)
                .putBoolean(KEY_SHOW_SCHEDULE, true)
                .putBoolean(KEY_SHOW_NOTES, true)
                .putBoolean(KEY_SHOW_MUSIC, true)
                .putBoolean(KEY_SHOW_THEME, true)
                .putBoolean(KEY_SHOW_EXPORT, true)
                .putBoolean(KEY_SHOW_COMPONENT_SETTINGS, true)
                .putInt(KEY_NAVIGATION_MODE, NAV_MODE_SIDEBAR)
                .apply();
    }
}