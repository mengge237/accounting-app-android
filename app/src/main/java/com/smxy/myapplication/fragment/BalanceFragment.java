package com.smxy.myapplication.fragment;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.card.MaterialCardView;
import com.smxy.myapplication.R;
import com.smxy.myapplication.builder.DynamicLayoutBuilder;
import com.smxy.myapplication.manager.LayoutManager;
import com.smxy.myapplication.model.ComponentConfig;
import com.smxy.myapplication.model.LedgerBook;
import com.smxy.myapplication.model.Record;
import com.smxy.myapplication.network.ApiService;
import com.smxy.myapplication.network.LedgerResponse;
import com.smxy.myapplication.network.RecordsResponse;
import com.smxy.myapplication.network.RetrofitClient;
import com.smxy.myapplication.utils.ErrorHandler;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;

public class BalanceFragment extends Fragment {

    private static final String TAG = "BalanceFragment";
    private static final String PREF_CUSTOM_LAYOUT_EXISTS = "custom_layout_exists";
    private static final String PREFS_NAME = "user_data";
    private static final String KEY_TOKEN = "token";
    private static final String KEY_CURRENT_LEDGER_ID = "current_ledger_id";
    private static final String KEY_CURRENT_LEDGER_NAME = "current_ledger_name";

    private LinearLayout dynamicContainer;
    private final Map<String, View> dynamicViews = new HashMap<>();

    private EditText etAmount;
    private EditText etNote;
    private EditText etDate;
    private EditText etTag;
    private RadioGroup rgType;
    private MaterialButtonToggleGroup toggleGroup;
    private MaterialButtonToggleGroup toggleModeGroup;
    private TextView tvTotalIncome;
    private TextView tvTotalExpense;
    private TextView tvBalance;
    private TextView tvCurrentLedger;
    private MaterialCardView ledgerSelectorCard;

    private LayoutManager layoutManager;
    private boolean hasCustomLayout = false;
    private boolean isSimpleMode = true;

    private ApiService apiService;
    private String authToken;

    private int currentLedgerId = 1;
    private String currentLedgerName = "默认账本";
    private List<LedgerBook> ledgerList = new ArrayList<>();

    private final android.os.Handler handler = new android.os.Handler(Looper.getMainLooper());

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_balance, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (!ErrorHandler.isFragmentValid(this)) {
            return;
        }

        Context context = getContext();
        if (!ErrorHandler.isContextValid(context)) {
            return;
        }

        initApiService();
        loadToken();
        loadCurrentLedger();
        initViews(view, context);
        setupListeners();
        loadLedgers();
        updateStatistics();
    }

    private void initApiService() {
        apiService = RetrofitClient.getApiService();
    }

    private void loadToken() {
        Context context = getContext();
        if (ErrorHandler.isContextValid(context)) {
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            authToken = prefs.getString(KEY_TOKEN, "");
            Log.d(TAG, "Token loaded: " + (authToken.isEmpty() ? "empty" : "exists"));
        }
    }

    private void loadCurrentLedger() {
        Context context = getContext();
        if (ErrorHandler.isContextValid(context)) {
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            currentLedgerId = prefs.getInt(KEY_CURRENT_LEDGER_ID, 1);
            currentLedgerName = prefs.getString(KEY_CURRENT_LEDGER_NAME, "默认账本");
        }
    }

    private void saveCurrentLedger(int ledgerId, String ledgerName) {
        Context context = getContext();
        if (ErrorHandler.isContextValid(context)) {
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            prefs.edit()
                    .putInt(KEY_CURRENT_LEDGER_ID, ledgerId)
                    .putString(KEY_CURRENT_LEDGER_NAME, ledgerName)
                    .apply();
        }
        currentLedgerId = ledgerId;
        currentLedgerName = ledgerName;
        updateLedgerDisplay();
        updateStatistics();
    }

    private void updateLedgerDisplay() {
        if (tvCurrentLedger != null) {
            tvCurrentLedger.setText(currentLedgerName);
        }
    }

    private void loadLedgers() {
        if (!ErrorHandler.isFragmentValid(this)) return;
        if (apiService == null) return;
        if (!ErrorHandler.isTokenValid(authToken)) {
            return;
        }

        String authHeader = "Bearer " + authToken;
        Call<LedgerResponse> call = apiService.getLedgers(authHeader);
        call.enqueue(new ErrorHandler.SafeCallback<LedgerResponse>(this) {
            @Override
            protected void onSuccess(LedgerResponse data) {
                if (data.getLedgers() != null) {
                    ledgerList.clear();
                    ledgerList.addAll(data.getLedgers());
                }
                // 确保当前账本名称正确显示
                updateLedgerDisplay();
            }

            @Override
            protected void onError(String message) {
                Log.w(TAG, "加载账本列表失败: " + message);
            }
        });
    }

    private void showLedgerSelector() {
        if (ledgerList.isEmpty()) {
            ErrorHandler.showToast(getContext(), "暂无其他账本");
            loadLedgers(); // 尝试重新加载
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_ledger_selector, null);
        RecyclerView recyclerView = dialogView.findViewById(R.id.recycler_ledgers);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        LedgerSelectorAdapter adapter = new LedgerSelectorAdapter(ledgerList, currentLedgerId, ledger -> {
            if (ledger.getId() != currentLedgerId) {
                saveCurrentLedger(ledger.getId(), ledger.getName());
                ErrorHandler.showToast(getContext(), "已切换到: " + ledger.getName());
            }
        });
        recyclerView.setAdapter(adapter);

        builder.setView(dialogView)
                .setNegativeButton("取消", null)
                .show();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (ErrorHandler.isFragmentValid(this)) {
            loadCurrentLedger();
            updateLedgerDisplay();
            refreshDynamicLayout();
            updateStatistics();
            loadLedgers(); // 刷新账本列表
        }
    }

    private void refreshDynamicLayout() {
        if (!ErrorHandler.isFragmentValid(this)) {
            return;
        }

        Context context = getContext();
        View view = getView();
        if (!ErrorHandler.isContextValid(context) || view == null) {
            return;
        }

        if (dynamicContainer != null) {
            dynamicContainer.removeAllViews();
        }
        dynamicViews.clear();

        layoutManager = new LayoutManager(context);
        hasCustomLayout = hasCustomLayoutConfig();

        List<ComponentConfig> components;
        if (hasCustomLayout) {
            components = layoutManager.getVisibleComponents();
            Log.d(TAG, "刷新布局 - 使用自定义布局，组件数量: " + components.size());
        } else {
            components = getSystemDefaultComponents();
            Log.d(TAG, "刷新布局 - 使用" + (isSimpleMode ? "简洁" : "完整") + "模式，组件数量: " + components.size());
        }

        for (ComponentConfig config : components) {
            View componentView = DynamicLayoutBuilder.buildComponent(context, config);
            if (componentView != null && dynamicContainer != null) {
                dynamicContainer.addView(componentView);
                dynamicViews.put(config.getId(), componentView);
            }
        }

        etAmount = null;
        etNote = null;
        rgType = null;
        toggleGroup = null;
        setupDynamicFieldReferences();
    }

    private void initViews(View view, Context context) {
        dynamicContainer = view.findViewById(R.id.dynamic_fields_container);
        if (dynamicContainer == null) {
            dynamicContainer = new LinearLayout(context);
            dynamicContainer.setOrientation(LinearLayout.VERTICAL);
        }

        toggleModeGroup = view.findViewById(R.id.toggle_mode);
        if (toggleModeGroup != null) {
            toggleModeGroup.check(R.id.btn_simple_mode);
            toggleModeGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
                if (isChecked) {
                    isSimpleMode = checkedId == R.id.btn_simple_mode;
                    refreshDynamicLayout();
                }
            });
        }

        tvTotalIncome = view.findViewById(R.id.tv_total_income);
        tvTotalExpense = view.findViewById(R.id.tv_total_expense);
        tvBalance = view.findViewById(R.id.tv_balance);
        tvCurrentLedger = view.findViewById(R.id.tv_current_ledger);
        ledgerSelectorCard = view.findViewById(R.id.ledger_selector_card);

        if (ledgerSelectorCard != null) {
            ledgerSelectorCard.setOnClickListener(v -> showLedgerSelector());
        }
        if (tvCurrentLedger != null) {
            tvCurrentLedger.setOnClickListener(v -> showLedgerSelector());
        }

        updateLedgerDisplay();

        refreshDynamicLayout();
    }

    private boolean hasCustomLayoutConfig() {
        Context context = getContext();
        if (!ErrorHandler.isContextValid(context)) return false;

        SharedPreferences prefs = context.getSharedPreferences("layout_config", Context.MODE_PRIVATE);
        return prefs.getBoolean(PREF_CUSTOM_LAYOUT_EXISTS, false);
    }

    private List<ComponentConfig> getSystemDefaultComponents() {
        List<ComponentConfig> defaultComponents = new ArrayList<>();
        if (isSimpleMode) {
            defaultComponents.add(new ComponentConfig("amount", "number", "金额", 0, true));
            defaultComponents.add(new ComponentConfig("type", "radio", "类型", 1, true));
            defaultComponents.add(new ComponentConfig("note", "text", "备注", 2, false));
        } else {
            defaultComponents.add(new ComponentConfig("amount", "number", "金额", 0, true));
            defaultComponents.add(new ComponentConfig("note", "text", "备注", 1, true));
            defaultComponents.add(new ComponentConfig("type", "radio", "类型", 2, true));
            defaultComponents.add(new ComponentConfig("date", "date", "日期", 3, true));
            defaultComponents.add(new ComponentConfig("category", "select", "分类", 4, false));
            defaultComponents.add(new ComponentConfig("tag", "text", "标签", 5, false));
        }
        return defaultComponents;
    }

    private void setupDynamicFieldReferences() {
        View amountView = dynamicViews.get("amount");
        if (amountView instanceof com.google.android.material.textfield.TextInputLayout) {
            com.google.android.material.textfield.TextInputLayout layout = (com.google.android.material.textfield.TextInputLayout) amountView;
            if (layout.getEditText() != null) {
                etAmount = layout.getEditText();
            }
        } else if (amountView instanceof EditText) {
            etAmount = (EditText) amountView;
        }

        View noteView = dynamicViews.get("note");
        if (noteView instanceof com.google.android.material.textfield.TextInputLayout) {
            com.google.android.material.textfield.TextInputLayout layout = (com.google.android.material.textfield.TextInputLayout) noteView;
            if (layout.getEditText() != null) {
                etNote = layout.getEditText();
            }
        } else if (noteView instanceof EditText) {
            etNote = (EditText) noteView;
        }

        View dateView = dynamicViews.get("date");
        if (dateView instanceof com.google.android.material.textfield.TextInputLayout) {
            com.google.android.material.textfield.TextInputLayout layout = (com.google.android.material.textfield.TextInputLayout) dateView;
            if (layout.getEditText() != null) {
                etDate = layout.getEditText();
            }
        } else if (dateView instanceof EditText) {
            etDate = (EditText) dateView;
        }

        View tagView = dynamicViews.get("tag");
        if (tagView instanceof LinearLayout) {
            LinearLayout container = (LinearLayout) tagView;
            for (int i = 0; i < container.getChildCount(); i++) {
                View child = container.getChildAt(i);
                if (child instanceof com.google.android.material.textfield.TextInputLayout) {
                    com.google.android.material.textfield.TextInputLayout layout = (com.google.android.material.textfield.TextInputLayout) child;
                    if (layout.getEditText() != null) {
                        etTag = layout.getEditText();
                        break;
                    }
                }
            }
        }

        View categoryView = dynamicViews.get("category");
        // Category组件使用Chip组，在getSelectedCategory()中动态读取

        View typeView = dynamicViews.get("type");
        if (typeView instanceof LinearLayout) {
            LinearLayout container = (LinearLayout) typeView;
            for (int i = 0; i < container.getChildCount(); i++) {
                View child = container.getChildAt(i);
                if (child instanceof RadioGroup) {
                    rgType = (RadioGroup) child;
                    break;
                } else if (child instanceof MaterialButtonToggleGroup) {
                    toggleGroup = (MaterialButtonToggleGroup) child;
                    break;
                }
            }
        }
    }

    private int getSelectedType() {
        if (rgType != null) {
            int checkedId = rgType.getCheckedRadioButtonId();
            if (checkedId == DynamicLayoutBuilder.RB_INCOME_ID) {
                return 1;
            } else if (checkedId == DynamicLayoutBuilder.RB_EXPENSE_ID) {
                return 0;
            }
        }
        if (toggleGroup != null) {
            int checkedId = toggleGroup.getCheckedButtonId();
            if (checkedId == DynamicLayoutBuilder.RB_INCOME_ID) {
                return 1;
            } else if (checkedId == DynamicLayoutBuilder.RB_EXPENSE_ID) {
                return 0;
            }
        }
        return 1;
    }

    private void setupListeners() {
        View btnSave = getView() != null ? getView().findViewById(R.id.btn_save) : null;
        if (btnSave != null) {
            btnSave.setOnClickListener(v -> saveRecord());
        }
    }

    private String getSelectedCategory() {
        View categoryView = dynamicViews.get("category");
        if (categoryView instanceof LinearLayout) {
            LinearLayout container = (LinearLayout) categoryView;
            for (int i = 0; i < container.getChildCount(); i++) {
                View child = container.getChildAt(i);
                if (child instanceof LinearLayout) {
                    LinearLayout chipContainer = (LinearLayout) child;
                    for (int j = 0; j < chipContainer.getChildCount(); j++) {
                        View chipView = chipContainer.getChildAt(j);
                        if (chipView instanceof com.google.android.material.chip.Chip) {
                            com.google.android.material.chip.Chip chip = (com.google.android.material.chip.Chip) chipView;
                            if (chip.isChecked()) {
                                return chip.getText().toString();
                            }
                        }
                    }
                }
            }
        }
        return "";
    }

    private void saveRecord() {
        if (!ErrorHandler.isFragmentValid(this)) return;

        Context context = getContext();
        if (!ErrorHandler.isContextValid(context)) return;

        if (etAmount == null) {
            ErrorHandler.showToast(context, "金额输入框未找到");
            return;
        }

        String amountStr = etAmount.getText().toString().trim();
        if (amountStr.isEmpty()) {
            ErrorHandler.showToast(context, R.string.amount_empty);
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(amountStr);
        } catch (NumberFormatException e) {
            ErrorHandler.showToast(context, "请输入有效的金额");
            return;
        }

        int type = getSelectedType();
        String note = (etNote != null) ? etNote.getText().toString().trim() : "";
        String date = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        if (etDate != null) {
            String dateText = etDate.getText().toString().trim();
            if (!dateText.isEmpty()) {
                date = dateText;
            }
        }
        String tag = (etTag != null) ? etTag.getText().toString().trim() : "";
        String category = getSelectedCategory();

        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String token = prefs.getString(KEY_TOKEN, "");

        if (token.isEmpty()) {
            ErrorHandler.showToast(context, "请先登录");
            return;
        }

        if (apiService == null) {
            ErrorHandler.showToast(context, "网络服务未初始化");
            return;
        }

        String authHeader = "Bearer " + token;
        Map<String, Object> recordData = new HashMap<>();
        recordData.put("type_id", type == 1 ? 6 : 1);
        recordData.put("amount", amount);
        recordData.put("note", note);
        recordData.put("record_date", date);
        recordData.put("ledger_id", currentLedgerId);
        if (!tag.isEmpty()) {
            recordData.put("tag", tag);
        }
        if (!category.isEmpty()) {
            recordData.put("category_name", category);
        }

        Call<com.smxy.myapplication.network.ApiResponse> call = apiService.addRecord(authHeader, recordData);
        call.enqueue(new ErrorHandler.SafeCallback<com.smxy.myapplication.network.ApiResponse>(this, "保存失败") {
            @Override
            protected void onSuccess(com.smxy.myapplication.network.ApiResponse data) {
                ErrorHandler.showToast(context, R.string.save_success);
                if (etAmount != null) etAmount.setText("");
                if (etNote != null) etNote.setText("");
                if (etTag != null) etTag.setText("");
                updateStatistics();
            }

            @Override
            protected void onError(String message) {
                ErrorHandler.showToast(context, message);
            }
        });
    }

    private void updateStatistics() {
        if (!ErrorHandler.isFragmentValid(this)) return;
        if (apiService == null) return;
        if (!ErrorHandler.isTokenValid(authToken)) return;

        String authHeader = "Bearer " + authToken;
        Call<RecordsResponse> call = apiService.getRecords(authHeader, currentLedgerId);
        call.enqueue(new ErrorHandler.SafeCallback<RecordsResponse>(this) {
            @Override
            protected void onSuccess(RecordsResponse data) {
                if (!ErrorHandler.isFragmentValid(BalanceFragment.this)) return;

                List<Record> records = data.getRecords();
                double totalIncome = 0;
                double totalExpense = 0;

                if (records != null) {
                    for (Record record : records) {
                        String category = record.getCategory();
                        if (category != null) {
                            if (category.equals("income")) {
                                totalIncome += record.getAmount();
                            } else if (category.equals("expense")) {
                                totalExpense += record.getAmount();
                            }
                        }
                    }
                }

                updateStatisticsUI(totalIncome, totalExpense);
            }

            @Override
            protected void onError(String message) {
                Log.w(TAG, "更新统计失败: " + message);
            }
        });
    }

    private void updateStatisticsUI(double totalIncome, double totalExpense) {
        if (!ErrorHandler.isFragmentValid(this)) return;

        if (tvTotalIncome != null) {
            tvTotalIncome.setText(String.format(Locale.getDefault(), "收入: ¥%.2f", totalIncome));
        }
        if (tvTotalExpense != null) {
            tvTotalExpense.setText(String.format(Locale.getDefault(), "支出: ¥%.2f", totalExpense));
        }
        if (tvBalance != null) {
            double balance = totalIncome - totalExpense;
            tvBalance.setText(String.format(Locale.getDefault(), "结余: ¥%.2f", balance));
            tvBalance.setTextColor(balance >= 0 ? Color.parseColor("#4CAF50") : Color.parseColor("#F44336"));
        }
    }

    public void fillForm(double amount, String category, int type, String note) {
        if (!ErrorHandler.isFragmentValid(this)) return;

        requireActivity().runOnUiThread(() -> {
            if (etAmount != null) {
                etAmount.setText(String.format(Locale.CHINA, "%.2f", amount));
                Log.d(TAG, "填充金额: " + amount);
            }

            if (etNote != null) {
                etNote.setText(note);
                Log.d(TAG, "填充备注: " + note);
            }

            if (type == 1) {
                if (rgType != null) {
                    rgType.check(DynamicLayoutBuilder.RB_INCOME_ID);
                }
                if (toggleGroup != null) {
                    toggleGroup.check(DynamicLayoutBuilder.RB_INCOME_ID);
                }
            } else {
                if (rgType != null) {
                    rgType.check(DynamicLayoutBuilder.RB_EXPENSE_ID);
                }
                if (toggleGroup != null) {
                    toggleGroup.check(DynamicLayoutBuilder.RB_EXPENSE_ID);
                }
            }

            autoFillCategory(category);
        });
    }

    public void fillNoteOnly(String note) {
        if (!ErrorHandler.isFragmentValid(this)) return;

        requireActivity().runOnUiThread(() -> {
            Log.d(TAG, "fillNoteOnly 被调用，备注: " + note);
            if (etNote != null) {
                etNote.setText(note);
                etNote.invalidate();
                etNote.requestLayout();
            } else {
                Log.e(TAG, "etNote 为空，延迟重试");
                handler.postDelayed(() -> fillNoteOnly(note), 500);
            }
        });
    }

    private void autoFillCategory(String category) {
        if (category == null || category.isEmpty()) return;
        Log.d(TAG, "尝试自动填充分类: " + category);

        View categoryView = dynamicViews.get("category");
        if (categoryView == null) {
            Log.d(TAG, "没有分类组件，跳过自动填充");
            return;
        }

        if (categoryView instanceof com.google.android.material.textfield.TextInputLayout) {
            com.google.android.material.textfield.TextInputLayout layout = (com.google.android.material.textfield.TextInputLayout) categoryView;
            if (layout.getEditText() != null) {
                layout.getEditText().setText(category);
                Log.d(TAG, "分类已设置到 TextInputLayout: " + category);
            }
        } else if (categoryView instanceof EditText) {
            ((EditText) categoryView).setText(category);
            Log.d(TAG, "分类已设置到 EditText: " + category);
        } else if (categoryView instanceof TextView) {
            ((TextView) categoryView).setText(category);
            Log.d(TAG, "分类已设置到 TextView: " + category);
        }
    }

    // 账本选择适配器
    static class LedgerSelectorAdapter extends RecyclerView.Adapter<LedgerSelectorAdapter.ViewHolder> {
        private final List<LedgerBook> ledgers;
        private final int currentLedgerId;
        private final OnLedgerSelectedListener listener;

        interface OnLedgerSelectedListener {
            void onSelected(LedgerBook ledger);
        }

        LedgerSelectorAdapter(List<LedgerBook> ledgers, int currentLedgerId, OnLedgerSelectedListener listener) {
            this.ledgers = ledgers;
            this.currentLedgerId = currentLedgerId;
            this.listener = listener;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_ledger_selector, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            LedgerBook ledger = ledgers.get(position);
            holder.tvIcon.setText(ledger.getIcon());
            holder.tvName.setText(ledger.getName());

            GradientDrawable drawable = new GradientDrawable();
            drawable.setShape(GradientDrawable.OVAL);
            try {
                int color = Color.parseColor(ledger.getColor());
                drawable.setColor(color);
            } catch (Exception e) {
                drawable.setColor(Color.parseColor("#4CAF50"));
            }
            holder.iconContainer.setBackground(drawable);

            if (ledger.getId() == currentLedgerId) {
                holder.tvCurrent.setVisibility(View.VISIBLE);
                holder.tvCurrent.setText("✓ 当前");
            } else {
                holder.tvCurrent.setVisibility(View.GONE);
            }

            holder.itemView.setOnClickListener(v -> {
                if (listener != null && ledger.getId() != currentLedgerId) {
                    listener.onSelected(ledger);
                }
            });
        }

        @Override
        public int getItemCount() {
            return ledgers.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvIcon, tvName, tvCurrent;
            View iconContainer;

            ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvIcon = itemView.findViewById(R.id.tv_ledger_icon);
                tvName = itemView.findViewById(R.id.tv_ledger_name);
                tvCurrent = itemView.findViewById(R.id.tv_current_ledger);
                iconContainer = itemView.findViewById(R.id.ledger_icon_container);
            }
        }
    }
}