package com.smxy.myapplication.fragment;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentActivity;

import com.smxy.myapplication.R;
import com.smxy.myapplication.utils.ExportManager;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

/**
 * 数据导出对话框
 * 提供日期范围选择功能
 */
public class ExportDialogFragment extends DialogFragment {

    private RadioGroup radioGroupRange;
    private RadioButton rbAll, rbLast3Months, rbLast6Months, rbThisYear, rbCustom;
    private View customDateLayout;
    private TextView tvStartDate, tvEndDate;
    private Button btnCancel, btnConfirm;

    private long customStartTime = 0;
    private long customEndTime = 0;
    private boolean isSelectingStart = true;

    private ExportListener listener;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.CHINA);

    public interface ExportListener {
        void onExport(int rangeOption, Long customStartDate, Long customEndDate);
    }

    public void setExportListener(ExportListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        FragmentActivity activity = requireActivity();
        LayoutInflater inflater = LayoutInflater.from(activity);
        View view = inflater.inflate(R.layout.dialog_export, null);

        initViews(view);
        setupListeners();

        // 默认选中"全部数据"
        rbAll.setChecked(true);
        customDateLayout.setVisibility(View.GONE);

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle("导出数据")
                .setView(view);

        return builder.create();
    }

    private void initViews(View view) {
        radioGroupRange = view.findViewById(R.id.radio_group_range);
        rbAll = view.findViewById(R.id.rb_all);
        rbLast3Months = view.findViewById(R.id.rb_last_3_months);
        rbLast6Months = view.findViewById(R.id.rb_last_6_months);
        rbThisYear = view.findViewById(R.id.rb_this_year);
        rbCustom = view.findViewById(R.id.rb_custom);
        customDateLayout = view.findViewById(R.id.custom_date_layout);
        tvStartDate = view.findViewById(R.id.tv_start_date);
        tvEndDate = view.findViewById(R.id.tv_end_date);
        btnCancel = view.findViewById(R.id.btn_cancel);
        btnConfirm = view.findViewById(R.id.btn_confirm);
    }

    private void setupListeners() {
        radioGroupRange.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rb_custom) {
                customDateLayout.setVisibility(View.VISIBLE);
                // 初始化日期选择器的默认值
                if (customStartTime == 0) {
                    Calendar calendar = Calendar.getInstance();
                    calendar.add(Calendar.MONTH, -1);
                    customStartTime = calendar.getTimeInMillis();
                    customEndTime = System.currentTimeMillis();
                    updateDateDisplay();
                }
            } else {
                customDateLayout.setVisibility(View.GONE);
            }
        });

        tvStartDate.setOnClickListener(v -> {
            isSelectingStart = true;
            showDatePickerDialog();
        });

        tvEndDate.setOnClickListener(v -> {
            isSelectingStart = false;
            showDatePickerDialog();
        });

        btnCancel.setOnClickListener(v -> dismiss());

        btnConfirm.setOnClickListener(v -> {
            int selectedId = radioGroupRange.getCheckedRadioButtonId();
            int rangeOption;
            Long startDate = null;
            Long endDate = null;

            if (selectedId == R.id.rb_all) {
                rangeOption = ExportManager.DateRangeOption.ALL;
            } else if (selectedId == R.id.rb_last_3_months) {
                rangeOption = ExportManager.DateRangeOption.LAST_3_MONTHS;
            } else if (selectedId == R.id.rb_last_6_months) {
                rangeOption = ExportManager.DateRangeOption.LAST_6_MONTHS;
            } else if (selectedId == R.id.rb_this_year) {
                rangeOption = ExportManager.DateRangeOption.THIS_YEAR;
            } else {
                rangeOption = ExportManager.DateRangeOption.CUSTOM;
                startDate = customStartTime;
                endDate = customEndTime;

                if (startDate > endDate) {
                    Toast.makeText(getContext(), "开始日期不能大于结束日期", Toast.LENGTH_SHORT).show();
                    return;
                }
            }

            if (listener != null) {
                listener.onExport(rangeOption, startDate, endDate);
            }
            dismiss();
        });
    }

    private void showDatePickerDialog() {
        Calendar calendar = Calendar.getInstance();
        if (isSelectingStart && customStartTime > 0) {
            calendar.setTimeInMillis(customStartTime);
        } else if (!isSelectingStart && customEndTime > 0) {
            calendar.setTimeInMillis(customEndTime);
        }

        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(getContext(),
                (view, year1, month1, dayOfMonth) -> {
                    Calendar selected = Calendar.getInstance();
                    selected.set(year1, month1, dayOfMonth, 0, 0, 0);
                    selected.set(Calendar.MILLISECOND, 0);

                    if (isSelectingStart) {
                        customStartTime = selected.getTimeInMillis();
                    } else {
                        // 结束日期设为当天的23:59:59
                        selected.set(Calendar.HOUR_OF_DAY, 23);
                        selected.set(Calendar.MINUTE, 59);
                        selected.set(Calendar.SECOND, 59);
                        customEndTime = selected.getTimeInMillis();
                    }
                    updateDateDisplay();
                }, year, month, day);

        datePickerDialog.show();
    }

    private void updateDateDisplay() {
        if (customStartTime > 0) {
            tvStartDate.setText(dateFormat.format(customStartTime));
        }
        if (customEndTime > 0) {
            tvEndDate.setText(dateFormat.format(customEndTime));
        }
    }
}