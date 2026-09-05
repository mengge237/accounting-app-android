package com.smxy.myapplication.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.google.android.material.radiobutton.MaterialRadioButton;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.smxy.myapplication.R;
import com.smxy.myapplication.activity.ComponentManageActivity;
import com.smxy.myapplication.activity.MainContainerActivity;
import com.smxy.myapplication.activity.ThemeSettingsActivity;
import com.smxy.myapplication.manager.SettingsManager;
import com.smxy.myapplication.utils.ErrorHandler;

public class SettingsFragment extends Fragment {

    private SettingsManager settingsManager;

    private SwitchMaterial switchCheatMode;
    private SwitchMaterial switchFloatBall;
    private SwitchMaterial switchVoiceBtn;

    private RadioGroup radioGroupNavMode;
    private MaterialRadioButton radioSidebarOnly;
    private MaterialRadioButton radioBottomOnly;
    private MaterialRadioButton radioBoth;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        if (!ErrorHandler.isFragmentValid(this)) return view;

        settingsManager = new SettingsManager(requireContext());

        initViews(view);
        setupCheatMode();
        setupFloatBall();
        setupVoiceBtn();
        setupThemeSettings(view);
        setupComponentManage(view);
        setupNavigationMode();
        setupResetButton(view);

        return view;
    }

    private void initViews(View view) {
        switchCheatMode = view.findViewById(R.id.switch_cheat_mode);
        switchFloatBall = view.findViewById(R.id.switch_float_ball);
        switchVoiceBtn = view.findViewById(R.id.switch_voice_btn);

        radioGroupNavMode = view.findViewById(R.id.radio_group_nav_mode);
        radioSidebarOnly = view.findViewById(R.id.radio_sidebar_only);
        radioBottomOnly = view.findViewById(R.id.radio_bottom_only);
        radioBoth = view.findViewById(R.id.radio_both);
    }

    private void setupCheatMode() {
        if (switchCheatMode != null) {
            switchCheatMode.setChecked(settingsManager.isCheatModeEnabled());
            switchCheatMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
                settingsManager.setCheatModeEnabled(isChecked);
                String message = isChecked ? getString(R.string.cheat_mode_enabled) : getString(R.string.cheat_mode_disabled);
                ErrorHandler.showToast(getContext(), message);
            });
        }
    }

    private void setupFloatBall() {
        if (switchFloatBall != null) {
            boolean showFloatBall = requireContext().getSharedPreferences("voice_settings", android.content.Context.MODE_PRIVATE)
                    .getBoolean("show_float_ball", false);
            switchFloatBall.setChecked(showFloatBall);

            switchFloatBall.setOnCheckedChangeListener((buttonView, isChecked) -> {
                requireContext().getSharedPreferences("voice_settings", android.content.Context.MODE_PRIVATE)
                        .edit()
                        .putBoolean("show_float_ball", isChecked)
                        .apply();

                if (getActivity() instanceof MainContainerActivity) {
                    MainContainerActivity activity = (MainContainerActivity) getActivity();
                    if (isChecked) {
                        activity.showVoiceFloatBall();
                    } else {
                        activity.hideVoiceFloatBall();
                    }
                }

                String message = isChecked ? "悬浮球已开启" : "悬浮球已关闭";
                ErrorHandler.showToast(getContext(), message);
            });
        }
    }

    private void setupVoiceBtn() {
        if (switchVoiceBtn != null) {
            boolean showVoiceBtn = requireContext().getSharedPreferences("voice_settings", android.content.Context.MODE_PRIVATE)
                    .getBoolean("show_main_voice_btn", true);
            switchVoiceBtn.setChecked(showVoiceBtn);

            switchVoiceBtn.setOnCheckedChangeListener((buttonView, isChecked) -> {
                requireContext().getSharedPreferences("voice_settings", android.content.Context.MODE_PRIVATE)
                        .edit()
                        .putBoolean("show_main_voice_btn", isChecked)
                        .apply();

                if (getActivity() instanceof MainContainerActivity) {
                    ((MainContainerActivity) getActivity()).refreshVoiceButtonVisibility();
                }

                String message = isChecked ? "主界面语音按钮已开启" : "主界面语音按钮已关闭";
                ErrorHandler.showToast(getContext(), message);
            });
        }
    }

    private void setupThemeSettings(View view) {
        View cardThemeSettings = view.findViewById(R.id.card_theme_settings);
        if (cardThemeSettings != null) {
            cardThemeSettings.setOnClickListener(v -> {
                startActivity(new Intent(getContext(), ThemeSettingsActivity.class));
            });
        }
    }

    private void setupComponentManage(View view) {
        View cardComponentManage = view.findViewById(R.id.card_component_manage);
        if (cardComponentManage != null) {
            cardComponentManage.setOnClickListener(v -> {
                startActivity(new Intent(getContext(), ComponentManageActivity.class));
            });
        }
    }

    private void setupNavigationMode() {
        if (radioGroupNavMode == null) return;

        int navMode = settingsManager.getNavigationMode();
        switch (navMode) {
            case SettingsManager.NAV_MODE_SIDEBAR:
                radioSidebarOnly.setChecked(true);
                break;
            case SettingsManager.NAV_MODE_BOTTOM:
                radioBottomOnly.setChecked(true);
                break;
            case SettingsManager.NAV_MODE_BOTH:
            default:
                radioBoth.setChecked(true);
                break;
        }

        radioGroupNavMode.setOnCheckedChangeListener((group, checkedId) -> {
            int newMode;
            if (checkedId == R.id.radio_sidebar_only) {
                newMode = SettingsManager.NAV_MODE_SIDEBAR;
            } else if (checkedId == R.id.radio_bottom_only) {
                newMode = SettingsManager.NAV_MODE_BOTTOM;
            } else if (checkedId == R.id.radio_both) {
                newMode = SettingsManager.NAV_MODE_BOTH;
            } else {
                newMode = SettingsManager.NAV_MODE_BOTH;
            }
            settingsManager.setNavigationMode(newMode);
            refreshNavigationModeUI();
            refreshNavigationMenu();
        });
    }

    private void refreshNavigationModeUI() {
        if (getActivity() instanceof MainContainerActivity) {
            ((MainContainerActivity) getActivity()).refreshNavigationModeUI();
        }
    }

    private void refreshNavigationMenu() {
        if (getActivity() instanceof MainContainerActivity) {
            ((MainContainerActivity) getActivity()).refreshNavigationMenu();
        }
    }

    private void setupResetButton(View view) {
        View btnResetSettings = view.findViewById(R.id.btn_reset_settings);
        if (btnResetSettings != null) {
            btnResetSettings.setOnClickListener(v -> showResetDialog());
        }

        View btnSaveSettings = view.findViewById(R.id.btn_save_settings);
        if (btnSaveSettings != null) {
            btnSaveSettings.setOnClickListener(v -> saveSettings());
        }
    }

    private void saveSettings() {
        ErrorHandler.showToast(getContext(), "设置已保存");
        if (getActivity() instanceof MainContainerActivity) {
            ((MainContainerActivity) getActivity()).loadHomeFragment();
        }
    }

    private void showResetDialog() {
        if (!ErrorHandler.isFragmentValid(this)) return;

        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.reset_dialog_title)
                .setMessage(R.string.reset_dialog_message)
                .setPositiveButton(R.string.confirm, (dialog, which) -> {
                    settingsManager.resetToDefault();
                    refreshUI();
                    refreshNavigationMenu();
                    ErrorHandler.showToast(getContext(), R.string.reset_success);
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void refreshUI() {
        switchCheatMode.setChecked(settingsManager.isCheatModeEnabled());

        boolean showFloatBall = requireContext().getSharedPreferences("voice_settings", android.content.Context.MODE_PRIVATE)
                .getBoolean("show_float_ball", false);
        switchFloatBall.setChecked(showFloatBall);

        boolean showVoiceBtn = requireContext().getSharedPreferences("voice_settings", android.content.Context.MODE_PRIVATE)
                .getBoolean("show_main_voice_btn", true);
        switchVoiceBtn.setChecked(showVoiceBtn);

        int navMode = settingsManager.getNavigationMode();
        if (radioSidebarOnly != null) radioSidebarOnly.setChecked(navMode == SettingsManager.NAV_MODE_SIDEBAR);
        if (radioBottomOnly != null) radioBottomOnly.setChecked(navMode == SettingsManager.NAV_MODE_BOTTOM);
        if (radioBoth != null) radioBoth.setChecked(navMode == SettingsManager.NAV_MODE_BOTH);
    }
}