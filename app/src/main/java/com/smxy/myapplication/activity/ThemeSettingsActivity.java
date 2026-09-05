package com.smxy.myapplication.activity;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;

import com.google.android.material.switchmaterial.SwitchMaterial;
import com.smxy.myapplication.R;
import com.smxy.myapplication.utils.ErrorHandler;

/**
 * ============================================================
 * 主题设置活动 - ThemeSettingsActivity
 * 布局文件：activity_theme_settings.xml
 * 功能：设置应用主题模式（浅色/深色/跟随系统）、字体大小、对比度、高对比度模式
 *
 * 使用说明：
 * 1. 主题模式 - 选择浅色、深色或跟随系统
 * 2. 字体大小 - 小/中/大/超大，实时预览效果
 * 3. 对比度 - 通过滑块调节预览文本透明度
 * 4. 高对比度 - 切换黑白高对比模式
 * 5. 保存设置 - 点击底部按钮保存并返回
 * ============================================================
 */
public class ThemeSettingsActivity extends AppCompatActivity {

    // SharedPreferences 存储键
    private static final String PREF_NAME = "theme_settings";
    private static final String KEY_THEME_MODE = "theme_mode";
    private static final String KEY_FONT_SIZE = "font_size";
    private static final String KEY_CONTRAST = "contrast";
    private static final String KEY_HIGH_CONTRAST = "high_contrast";

    // 主题模式常量
    private static final int THEME_LIGHT = 0;
    private static final int THEME_DARK = 1;
    private static final int THEME_FOLLOW_SYSTEM = 2;

    // 字体大小常量
    private static final int FONT_SMALL = 0;
    private static final int FONT_MEDIUM = 1;
    private static final int FONT_LARGE = 2;
    private static final int FONT_XLARGE = 3;

    // UI组件
    private RadioGroup themeRadioGroup;      // 主题模式单选组
    private RadioGroup fontSizeRadioGroup;   // 字体大小单选组
    private TextView previewText;             // 预览文本
    private SeekBar contrastSeekBar;          // 对比度滑块
    private TextView contrastValue;           // 对比度数值显示
    private SwitchMaterial highContrastSwitch; // 高对比度开关
    private Button btnSaveAndBack;             // 保存并返回按钮

    private SharedPreferences prefs;

    // 当前选中的设置值
    private int selectedThemeMode = THEME_FOLLOW_SYSTEM;
    private int selectedFontSize = FONT_MEDIUM;
    private int selectedContrast = 0;
    private boolean selectedHighContrast = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_theme_settings);

        setTitle("主题设置");

        prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);

        initViews();
        loadCurrentSettings();
        setupListeners();
    }

    private void initViews() {
        themeRadioGroup = findViewById(R.id.theme_radio_group);
        fontSizeRadioGroup = findViewById(R.id.font_size_radio_group);
        previewText = findViewById(R.id.preview_text);
        contrastSeekBar = findViewById(R.id.contrast_seek_bar);
        contrastValue = findViewById(R.id.contrast_value);
        highContrastSwitch = findViewById(R.id.high_contrast_switch);
        btnSaveAndBack = findViewById(R.id.btn_save_and_back);
    }

    private void setupListeners() {
        // 主题模式选择监听
        if (themeRadioGroup != null) {
            themeRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
                if (checkedId == R.id.radio_light) {
                    selectedThemeMode = THEME_LIGHT;
                } else if (checkedId == R.id.radio_dark) {
                    selectedThemeMode = THEME_DARK;
                } else {
                    selectedThemeMode = THEME_FOLLOW_SYSTEM;
                }
            });
        }

        // 字体大小选择监听
        if (fontSizeRadioGroup != null && previewText != null) {
            fontSizeRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
                float fontSize;
                if (checkedId == R.id.radio_small) {
                    fontSize = 0.85f;
                    selectedFontSize = FONT_SMALL;
                } else if (checkedId == R.id.radio_medium) {
                    fontSize = 1.0f;
                    selectedFontSize = FONT_MEDIUM;
                } else if (checkedId == R.id.radio_large) {
                    fontSize = 1.15f;
                    selectedFontSize = FONT_LARGE;
                } else {
                    fontSize = 1.3f;
                    selectedFontSize = FONT_XLARGE;
                }
                previewText.setTextSize(14 * fontSize);
            });
        }

        // 对比度滑块监听
        if (contrastSeekBar != null && contrastValue != null && previewText != null) {
            contrastSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    contrastValue.setText(progress + "%");
                    if (fromUser) {
                        selectedContrast = progress;
                        float alpha = 0.5f + (progress / 200f);
                        previewText.setAlpha(alpha);
                    }
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {}

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {}
            });
        }

        // 高对比度开关监听
        if (highContrastSwitch != null && previewText != null) {
            highContrastSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
                selectedHighContrast = isChecked;
                if (isChecked) {
                    previewText.setBackgroundColor(ContextCompat.getColor(this, android.R.color.black));
                    previewText.setTextColor(ContextCompat.getColor(this, android.R.color.white));
                } else {
                    previewText.setBackgroundColor(0);
                    previewText.setTextColor(ContextCompat.getColor(this, android.R.color.black));
                }
            });
        }

        // 保存按钮监听
        if (btnSaveAndBack != null) {
            btnSaveAndBack.setOnClickListener(v -> saveAndBack());
        }
    }

    /**
     * 加载已保存的设置
     */
    private void loadCurrentSettings() {
        if (prefs == null) return;

        // 加载主题模式
        selectedThemeMode = prefs.getInt(KEY_THEME_MODE, THEME_FOLLOW_SYSTEM);
        if (themeRadioGroup != null) {
            if (selectedThemeMode == THEME_LIGHT) {
                themeRadioGroup.check(R.id.radio_light);
            } else if (selectedThemeMode == THEME_DARK) {
                themeRadioGroup.check(R.id.radio_dark);
            } else {
                themeRadioGroup.check(R.id.radio_system);
            }
        }

        // 加载字体大小
        selectedFontSize = prefs.getInt(KEY_FONT_SIZE, FONT_MEDIUM);
        if (fontSizeRadioGroup != null && previewText != null) {
            if (selectedFontSize == FONT_SMALL) {
                fontSizeRadioGroup.check(R.id.radio_small);
                previewText.setTextSize(14 * 0.85f);
            } else if (selectedFontSize == FONT_MEDIUM) {
                fontSizeRadioGroup.check(R.id.radio_medium);
                previewText.setTextSize(14 * 1.0f);
            } else if (selectedFontSize == FONT_LARGE) {
                fontSizeRadioGroup.check(R.id.radio_large);
                previewText.setTextSize(14 * 1.15f);
            } else {
                fontSizeRadioGroup.check(R.id.radio_xlarge);
                previewText.setTextSize(14 * 1.3f);
            }
        }

        // 加载对比度
        selectedContrast = prefs.getInt(KEY_CONTRAST, 0);
        if (contrastSeekBar != null) {
            contrastSeekBar.setProgress(selectedContrast);
        }
        if (contrastValue != null) {
            contrastValue.setText(selectedContrast + "%");
        }
        if (previewText != null) {
            float alpha = 0.5f + (selectedContrast / 200f);
            previewText.setAlpha(alpha);
        }

        // 加载高对比度模式
        selectedHighContrast = prefs.getBoolean(KEY_HIGH_CONTRAST, false);
        if (highContrastSwitch != null) {
            highContrastSwitch.setChecked(selectedHighContrast);
        }
        if (selectedHighContrast && previewText != null) {
            previewText.setBackgroundColor(ContextCompat.getColor(this, android.R.color.black));
            previewText.setTextColor(ContextCompat.getColor(this, android.R.color.white));
        }
    }

    /**
     * 保存设置并返回
     */
    private void saveAndBack() {
        // 应用主题模式
        if (selectedThemeMode == THEME_LIGHT) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        } else if (selectedThemeMode == THEME_DARK) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        }

        // 保存到SharedPreferences
        if (prefs != null) {
            prefs.edit()
                    .putInt(KEY_THEME_MODE, selectedThemeMode)
                    .putInt(KEY_FONT_SIZE, selectedFontSize)
                    .putInt(KEY_CONTRAST, selectedContrast)
                    .putBoolean(KEY_HIGH_CONTRAST, selectedHighContrast)
                    .apply();
        }

        ErrorHandler.showToast(this, "主题设置已保存");
        finish();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull android.view.MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}