package com.smxy.myapplication.fragment;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.smxy.myapplication.R;
import com.smxy.myapplication.adapter.RecordAdapter;
import com.smxy.myapplication.model.LedgerBook;
import com.smxy.myapplication.model.Record;
import com.smxy.myapplication.network.ApiService;
import com.smxy.myapplication.network.LedgerResponse;
import com.smxy.myapplication.network.RecordsResponse;
import com.smxy.myapplication.network.RetrofitClient;
import com.smxy.myapplication.utils.ErrorHandler;
import com.smxy.myapplication.utils.ExportHelper;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RecordsFragment extends Fragment {

    private static final String PREFS_NAME = "user_data";
    private static final String KEY_TOKEN = "token";
    private static final String KEY_CURRENT_LEDGER_ID = "current_ledger_id";
    private static final String KEY_CURRENT_LEDGER_NAME = "current_ledger_name";

    private RecyclerView recyclerView;
    private RecordAdapter recordAdapter;
    private final List<Record> recordList = new ArrayList<>();
    private TextView tvEmpty;
    private TextView tvTotalCount;
    private TextView tvTotalAmount;
    private TextView tvCurrentLedger;
    private MaterialCardView ledgerSelectorCard;
    private ApiService apiService;
    private String authToken;
    private ExportHelper exportHelper;

    private int currentLedgerId = 1;
    private String currentLedgerName = "默认账本";
    private List<LedgerBook> ledgerList = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_records, container, false);
        initViews(view);
        initApiService();
        loadToken();
        loadCurrentLedger();
        return view;
    }

    private void initApiService() {
        apiService = RetrofitClient.getApiService();
    }

    private void loadToken() {
        Context context = getContext();
        if (ErrorHandler.isContextValid(context)) {
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            authToken = prefs.getString(KEY_TOKEN, "");
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
        loadRecordsFromServer();
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
                updateLedgerDisplay();
            }

            @Override
            protected void onError(String message) {
                android.util.Log.w("RecordsFragment", "加载账本列表失败: " + message);
            }
        });
    }

    private void showLedgerSelector() {
        if (ledgerList.isEmpty()) {
            ErrorHandler.showToast(getContext(), "暂无其他账本");
            loadLedgers();
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

    private void initViews(View view) {
        recyclerView = view.findViewById(R.id.recycler_view);
        tvEmpty = view.findViewById(R.id.tv_empty);
        tvTotalCount = view.findViewById(R.id.tv_total_count);
        tvTotalAmount = view.findViewById(R.id.tv_total_amount);
        tvCurrentLedger = view.findViewById(R.id.tv_current_ledger);
        ledgerSelectorCard = view.findViewById(R.id.ledger_selector_card);

        if (ledgerSelectorCard != null) {
            ledgerSelectorCard.setOnClickListener(v -> showLedgerSelector());
        }
        if (tvCurrentLedger != null) {
            tvCurrentLedger.setOnClickListener(v -> showLedgerSelector());
        }

        updateLedgerDisplay();

        if (recyclerView != null) {
            recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        }

        loadLedgers();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (ErrorHandler.isFragmentValid(this)) {
            loadCurrentLedger();
            updateLedgerDisplay();
            loadRecordsFromServer();
        }
    }

    private void loadRecordsFromServer() {
        if (!ErrorHandler.isFragmentValid(this)) return;

        if (apiService == null) {
            ErrorHandler.showToast(getContext(), "网络服务未初始化");
            return;
        }

        if (!ErrorHandler.isTokenValid(authToken)) {
            ErrorHandler.showToast(getContext(), "请先登录");
            if (tvEmpty != null) {
                tvEmpty.setVisibility(View.VISIBLE);
                tvEmpty.setText(R.string.please_login_first);
            }
            return;
        }

        if (tvEmpty != null) {
            tvEmpty.setText(R.string.loading);
            tvEmpty.setVisibility(View.VISIBLE);
        }
        if (recyclerView != null) {
            recyclerView.setVisibility(View.GONE);
        }

        String authHeader = "Bearer " + authToken;
        Call<RecordsResponse> call = apiService.getRecords(authHeader, currentLedgerId);
        call.enqueue(new Callback<RecordsResponse>() {
            @Override
            public void onResponse(@NonNull Call<RecordsResponse> call, @NonNull Response<RecordsResponse> response) {
                if (!ErrorHandler.isFragmentValid(RecordsFragment.this)) return;

                if (response.isSuccessful() && response.body() != null) {
                    List<Record> records = response.body().getRecords();
                    if (records != null) {
                        recordList.clear();
                        recordList.addAll(records);
                    } else {
                        recordList.clear();
                    }
                    updateUI();
                } else {
                    recordList.clear();
                    updateUI();
                    if (response.code() == 401) {
                        ErrorHandler.showToast(getContext(), "登录已过期，请重新登录");
                    } else {
                        ErrorHandler.showToast(getContext(), "加载失败: " + response.code());
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<RecordsResponse> call, @NonNull Throwable t) {
                if (!ErrorHandler.isFragmentValid(RecordsFragment.this)) return;
                recordList.clear();
                updateUI();
                ErrorHandler.showToast(getContext(), "网络错误: " + t.getMessage());
            }
        });
    }

    private void updateUI() {
        if (!ErrorHandler.isFragmentValid(this)) return;
        updateStatistics();

        if (recordAdapter == null) {
            recordAdapter = new RecordAdapter(recordList);
            recordAdapter.setOnItemClickListener(this::showRecordDetail);
            if (recyclerView != null) {
                recyclerView.setAdapter(recordAdapter);
            }
        } else {
            recordAdapter.updateData(recordList);
        }
        updateEmptyViewVisibility();
    }

    private void showRecordDetail(Record record) {
        if (record == null || getContext() == null) return;

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA);
        String dateStr = record.getRecordDate();
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.CHINA);
            Date date = inputFormat.parse(dateStr);
            if (date != null) {
                dateStr = dateFormat.format(date);
            }
        } catch (Exception ignored) {
        }

        String typeText = record.isIncome() ? getString(R.string.income) : getString(R.string.expense);
        String amountText = String.format(Locale.CHINA, "%.2f", record.getAmount());

        String detailMessage = getString(R.string.record_detail_type, typeText) + "\n" +
                getString(R.string.record_detail_category, record.getTypeName()) + "\n" +
                getString(R.string.record_detail_amount, amountText) + "\n" +
                getString(R.string.record_detail_note, record.getNote().isEmpty() ? getString(R.string.no_note_text) : record.getNote()) + "\n" +
                getString(R.string.record_detail_date, dateStr);

        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.record_detail_title)
                .setMessage(detailMessage)
                .setPositiveButton(R.string.confirm, null)
                .show();
    }

    private void updateEmptyViewVisibility() {
        if (!ErrorHandler.isFragmentValid(this)) return;

        if (tvEmpty != null && recyclerView != null) {
            boolean isEmpty = recordList.isEmpty();
            if (isEmpty) {
                tvEmpty.setText(R.string.current_ledger_empty_records);
            }
            tvEmpty.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
            recyclerView.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        }
    }

    private void updateStatistics() {
        if (!ErrorHandler.isFragmentValid(this)) return;

        double totalIncome = 0;
        double totalExpense = 0;

        for (Record record : recordList) {
            if (record == null) continue;
            if (record.isIncome()) {
                totalIncome += record.getAmount();
            } else if (record.isExpense()) {
                totalExpense += record.getAmount();
            }
        }

        if (tvTotalCount != null) {
            String countText = getString(R.string.record_count_format, recordList.size());
            tvTotalCount.setText(countText);
        }

        if (tvTotalAmount != null) {
            String incomeStr = String.format(Locale.CHINA, "收入: ¥%.2f", totalIncome);
            String expenseStr = String.format(Locale.CHINA, "支出: ¥%.2f", totalExpense);
            String amountText = incomeStr + "  " + expenseStr;
            tvTotalAmount.setText(amountText);

            double balance = totalIncome - totalExpense;
            if (balance > 0) {
                tvTotalAmount.setTextColor(android.graphics.Color.parseColor("#4CAF50"));
            } else if (balance < 0) {
                tvTotalAmount.setTextColor(android.graphics.Color.parseColor("#F44336"));
            } else {
                tvTotalAmount.setTextColor(android.graphics.Color.parseColor("#333333"));
            }
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        exportHelper = new ExportHelper(requireActivity());
    }

    public void exportData() {
        if (exportHelper != null) {
            exportHelper.startExport();
        } else {
            exportHelper = new ExportHelper(requireActivity());
            exportHelper.startExport();
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