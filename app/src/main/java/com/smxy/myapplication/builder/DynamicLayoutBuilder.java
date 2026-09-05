package com.smxy.myapplication.builder;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import com.google.android.material.chip.Chip;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.smxy.myapplication.R;
import com.smxy.myapplication.model.ComponentConfig;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DynamicLayoutBuilder {
    public static final int RB_INCOME_ID = 1001;
    public static final int RB_EXPENSE_ID = 1002;

    // 分类选项
    private static final String[] EXPENSE_CATEGORIES = {
            "餐饮", "购物", "交通", "娱乐", "医疗", "房租", "水电", "通讯", "教育", "美容", "宠物", "其他"
    };

    // 时间精度选项
    private static final String[] TIME_PRECISION = {
            "精确时间(时:分)", "粗略(早/中/晚)"
    };

    // 早中晚时段
    private static final String[] TIME_PERIODS = {
            "🌅 早晨 (5:00-9:00)", "☀️ 中午 (11:00-13:00)",
            "🌤️ 下午 (13:00-17:00)", "🌙 晚上 (17:00-21:00)", "🌃 深夜 (21:00-5:00)"
    };

    public static View buildComponent(Context context, ComponentConfig config) {
        LinearLayout container = new LinearLayout(context);
        container.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(0, 8, 0, 8);

        switch (config.getType()) {
            case "number":
                return buildNumberField(context, config);
            case "text":
                return buildTextField(context, config);
            case "radio":
                return buildRadioGroup(context, config);
            case "date":
                return buildDateField(context, config);
            case "time":
                return buildTimeField(context, config);
            case "datetime":
                return buildDateTimeField(context, config);
            case "select":
                return buildSelectField(context, config);
            case "category":
                return buildCategoryField(context, config);
            case "tag":
                return buildTagField(context, config);
            default:
                return container;
        }
    }

    private static View buildNumberField(Context context, ComponentConfig config) {
        TextInputLayout layout = new TextInputLayout(context);
        layout.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        layout.setHint(config.getTitle());
        layout.setBoxBackgroundMode(TextInputLayout.BOX_BACKGROUND_OUTLINE);
        layout.setStartIconDrawable(R.drawable.ic_currency);

        TextInputEditText editText = new TextInputEditText(context);
        editText.setInputType(android.text.InputType.TYPE_CLASS_NUMBER |
                android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
        editText.setTag(config.getId());

        layout.addView(editText);
        return layout;
    }

    private static View buildTextField(Context context, ComponentConfig config) {
        TextInputLayout layout = new TextInputLayout(context);
        layout.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        layout.setHint(config.getTitle());
        layout.setBoxBackgroundMode(TextInputLayout.BOX_BACKGROUND_OUTLINE);
        layout.setStartIconDrawable(R.drawable.ic_note);

        TextInputEditText editText = new TextInputEditText(context);
        editText.setInputType(android.text.InputType.TYPE_CLASS_TEXT);
        editText.setTag(config.getId());

        layout.addView(editText);
        return layout;
    }

    private static View buildRadioGroup(Context context, ComponentConfig config) {
        LinearLayout container = new LinearLayout(context);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setTag(config.getId());

        TextView title = new TextView(context);
        title.setText(config.getTitle());
        title.setTextSize(14);
        title.setTextColor(context.getColor(R.color.text_secondary));
        container.addView(title);

        RadioGroup radioGroup = createRadioGroup(context);
        container.addView(radioGroup);

        return container;
    }

    private static RadioGroup createRadioGroup(Context context) {
        RadioGroup radioGroup = new RadioGroup(context);
        radioGroup.setOrientation(RadioGroup.HORIZONTAL);

        RadioButton incomeBtn = new RadioButton(context);
        incomeBtn.setText("收入");
        incomeBtn.setId(RB_INCOME_ID);
        incomeBtn.setChecked(true);

        RadioButton expenseBtn = new RadioButton(context);
        expenseBtn.setText("支出");
        expenseBtn.setId(RB_EXPENSE_ID);

        radioGroup.addView(incomeBtn);
        radioGroup.addView(expenseBtn);
        return radioGroup;
    }

    private static View buildDateField(Context context, ComponentConfig config) {
        LinearLayout container = new LinearLayout(context);
        container.setOrientation(LinearLayout.VERTICAL);

        TextView title = new TextView(context);
        title.setText(config.getTitle());
        title.setTextSize(14);
        title.setTextColor(context.getColor(R.color.text_secondary));
        container.addView(title);

        TextInputLayout layout = createDateLayout(context);
        TextInputEditText editText = createDateEditText(context, config);
        layout.addView(editText);
        container.addView(layout);

        return container;
    }

    private static TextInputLayout createDateLayout(Context context) {
        TextInputLayout layout = new TextInputLayout(context);
        layout.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        layout.setHint("选择日期");
        layout.setBoxBackgroundMode(TextInputLayout.BOX_BACKGROUND_OUTLINE);
        // 使用 END_ICON_CLEAR_TEXT 替代，日历图标需要另外添加
        layout.setEndIconMode(TextInputLayout.END_ICON_CLEAR_TEXT);
        return layout;
    }

    private static TextInputEditText createDateEditText(Context context, ComponentConfig config) {
        TextInputEditText editText = new TextInputEditText(context);
        editText.setFocusable(false);
        editText.setClickable(true);
        editText.setTag(config.getId());

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        editText.setText(sdf.format(new Date()));

        editText.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            DatePickerDialog datePicker = new DatePickerDialog(context,
                    (view, year, month, dayOfMonth) -> {
                        String dateStr = String.format(Locale.getDefault(), "%04d-%02d-%02d",
                                year, month + 1, dayOfMonth);
                        editText.setText(dateStr);
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH));
            datePicker.show();
        });
        return editText;
    }

    private static View buildTimeField(Context context, ComponentConfig config) {
        LinearLayout container = new LinearLayout(context);
        container.setOrientation(LinearLayout.VERTICAL);

        TextView title = new TextView(context);
        title.setText(config.getTitle());
        title.setTextSize(14);
        title.setTextColor(context.getColor(R.color.text_secondary));
        container.addView(title);

        // 时间精度选择
        TextInputLayout precisionLayout = createPrecisionLayout(context);
        AutoCompleteTextView precisionInput = createPrecisionInput(context);
        precisionLayout.addView(precisionInput);
        container.addView(precisionLayout);

        // 精确时间选择器
        TextInputLayout timeLayout = createTimeLayout(context);
        TextInputEditText timeInput = createTimeEditText(context);
        timeLayout.addView(timeInput);
        container.addView(timeLayout);

        // 粗略时间选择器
        LinearLayout periodContainer = createPeriodContainer(context);
        AutoCompleteTextView periodInput = createPeriodInput(context);
        periodContainer.addView(periodInput);
        container.addView(periodContainer);
        periodContainer.setVisibility(View.GONE);

        // 根据精度切换显示
        precisionInput.setOnItemClickListener((parent, view, position, id) -> {
            if (position == 0) {
                timeLayout.setVisibility(View.VISIBLE);
                periodContainer.setVisibility(View.GONE);
            } else {
                timeLayout.setVisibility(View.GONE);
                periodContainer.setVisibility(View.VISIBLE);
            }
        });

        container.setTag(config.getId());
        return container;
    }

    private static TextInputLayout createPrecisionLayout(Context context) {
        TextInputLayout layout = new TextInputLayout(context);
        layout.setHint("时间精度");
        layout.setBoxBackgroundMode(TextInputLayout.BOX_BACKGROUND_OUTLINE);
        layout.setEndIconMode(TextInputLayout.END_ICON_DROPDOWN_MENU);
        return layout;
    }

    private static AutoCompleteTextView createPrecisionInput(Context context) {
        AutoCompleteTextView input = new AutoCompleteTextView(context);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(context,
                android.R.layout.simple_dropdown_item_1line, TIME_PRECISION);
        input.setAdapter(adapter);
        input.setText(TIME_PRECISION[0], false);
        return input;
    }

    private static TextInputLayout createTimeLayout(Context context) {
        TextInputLayout layout = new TextInputLayout(context);
        layout.setHint("选择时间");
        layout.setBoxBackgroundMode(TextInputLayout.BOX_BACKGROUND_OUTLINE);
        layout.setEndIconMode(TextInputLayout.END_ICON_CLEAR_TEXT);
        return layout;
    }

    private static TextInputEditText createTimeEditText(Context context) {
        TextInputEditText input = new TextInputEditText(context);
        input.setFocusable(false);
        input.setClickable(true);
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
        input.setText(timeFormat.format(new Date()));

        input.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            TimePickerDialog timePicker = new TimePickerDialog(context,
                    (view, hourOfDay, minute) -> {
                        String timeStr = String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute);
                        input.setText(timeStr);
                    },
                    calendar.get(Calendar.HOUR_OF_DAY),
                    calendar.get(Calendar.MINUTE),
                    true);
            timePicker.show();
        });
        return input;
    }

    private static LinearLayout createPeriodContainer(Context context) {
        LinearLayout container = new LinearLayout(context);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        TextView label = new TextView(context);
        label.setText("选择时段");
        label.setTextSize(12);
        label.setTextColor(context.getColor(R.color.text_secondary));
        container.addView(label);

        return container;
    }

    private static AutoCompleteTextView createPeriodInput(Context context) {
        AutoCompleteTextView input = new AutoCompleteTextView(context);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(context,
                android.R.layout.simple_dropdown_item_1line, TIME_PERIODS);
        input.setAdapter(adapter);
        input.setText(TIME_PERIODS[0], false);
        return input;
    }

    private static View buildDateTimeField(Context context, ComponentConfig config) {
        LinearLayout container = new LinearLayout(context);
        container.setOrientation(LinearLayout.VERTICAL);

        TextView title = new TextView(context);
        title.setText(config.getTitle());
        title.setTextSize(14);
        title.setTextColor(context.getColor(R.color.text_secondary));
        container.addView(title);

        // 日期选择
        TextInputLayout dateLayout = createDateLayout(context);
        TextInputEditText dateInput = createDateEditText(context, config);
        dateLayout.addView(dateInput);
        container.addView(dateLayout);

        // 时间选择
        TextInputLayout timeLayout = createDateTimeLayout(context);
        TextInputEditText timeInput = createDateTimeEditText(context);
        timeLayout.addView(timeInput);
        container.addView(timeLayout);

        container.setTag(config.getId());
        return container;
    }

    private static TextInputLayout createDateTimeLayout(Context context) {
        TextInputLayout layout = new TextInputLayout(context);
        layout.setHint("选择时间");
        layout.setBoxBackgroundMode(TextInputLayout.BOX_BACKGROUND_OUTLINE);
        layout.setEndIconMode(TextInputLayout.END_ICON_CLEAR_TEXT);
        return layout;
    }

    private static TextInputEditText createDateTimeEditText(Context context) {
        TextInputEditText input = new TextInputEditText(context);
        input.setFocusable(false);
        input.setClickable(true);
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
        input.setText(timeFormat.format(new Date()));

        input.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            TimePickerDialog timePicker = new TimePickerDialog(context,
                    (view, hourOfDay, minute) -> {
                        String timeStr = String.format(Locale.getDefault(), "%02d:%02d:00", hourOfDay, minute);
                        input.setText(timeStr);
                    },
                    calendar.get(Calendar.HOUR_OF_DAY),
                    calendar.get(Calendar.MINUTE),
                    true);
            timePicker.show();
        });
        return input;
    }

    private static View buildSelectField(Context context, ComponentConfig config) {
        TextInputLayout layout = new TextInputLayout(context);
        layout.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        layout.setHint(config.getTitle());
        layout.setBoxBackgroundMode(TextInputLayout.BOX_BACKGROUND_OUTLINE);
        layout.setEndIconMode(TextInputLayout.END_ICON_DROPDOWN_MENU);

        AutoCompleteTextView autoCompleteTextView = new AutoCompleteTextView(context);
        autoCompleteTextView.setInputType(android.text.InputType.TYPE_CLASS_TEXT);
        autoCompleteTextView.setTag(config.getId());
        autoCompleteTextView.setFocusable(true);

        List<String> options = config.getOptions();
        if (options == null || options.isEmpty()) {
            options = getDefaultOptions(config.getId());
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(context,
                android.R.layout.simple_dropdown_item_1line, options);
        autoCompleteTextView.setAdapter(adapter);
        autoCompleteTextView.setThreshold(1);

        layout.addView(autoCompleteTextView);
        return layout;
    }

    private static View buildCategoryField(Context context, ComponentConfig config) {
        LinearLayout container = new LinearLayout(context);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setTag(config.getId());

        TextView title = new TextView(context);
        title.setText(config.getTitle());
        title.setTextSize(14);
        title.setTextColor(context.getColor(R.color.text_secondary));
        container.addView(title);

        LinearLayout chipContainer = createChipContainer(context);
        container.addView(chipContainer);

        return container;
    }

    private static LinearLayout createChipContainer(Context context) {
        LinearLayout chipContainer = new LinearLayout(context);
        chipContainer.setOrientation(LinearLayout.HORIZONTAL);
        chipContainer.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        // 设置换行效果
        chipContainer.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        for (String category : EXPENSE_CATEGORIES) {
            Chip chip = new Chip(context);
            chip.setText(category);
            chip.setCheckable(true);
            chipContainer.addView(chip);
        }

        return chipContainer;
    }

    private static View buildTagField(Context context, ComponentConfig config) {
        LinearLayout container = new LinearLayout(context);
        container.setOrientation(LinearLayout.VERTICAL);

        TextView title = new TextView(context);
        title.setText(config.getTitle());
        title.setTextSize(14);
        title.setTextColor(context.getColor(R.color.text_secondary));
        container.addView(title);

        TextInputLayout layout = createTagLayout(context);
        TextInputEditText editText = createTagEditText(context, config);
        layout.addView(editText);
        container.addView(layout);

        return container;
    }

    private static TextInputLayout createTagLayout(Context context) {
        TextInputLayout layout = new TextInputLayout(context);
        layout.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        layout.setHint("输入标签，多个标签用逗号分隔");
        layout.setBoxBackgroundMode(TextInputLayout.BOX_BACKGROUND_OUTLINE);
        return layout;
    }

    private static TextInputEditText createTagEditText(Context context, ComponentConfig config) {
        TextInputEditText editText = new TextInputEditText(context);
        editText.setInputType(android.text.InputType.TYPE_CLASS_TEXT);
        editText.setTag(config.getId());
        return editText;
    }

    private static List<String> getDefaultOptions(String id) {
        List<String> options = new ArrayList<>();
        if ("category".equals(id)) {
            options.addAll(Arrays.asList(EXPENSE_CATEGORIES));
        } else {
            options.add("选项1");
            options.add("选项2");
            options.add("选项3");
        }
        return options;
    }
}