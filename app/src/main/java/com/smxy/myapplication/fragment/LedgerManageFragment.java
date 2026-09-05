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
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.smxy.myapplication.R;
import com.smxy.myapplication.model.LedgerBook;
import com.smxy.myapplication.network.ApiResponse;
import com.smxy.myapplication.network.ApiService;
import com.smxy.myapplication.network.LedgerResponse;
import com.smxy.myapplication.network.RetrofitClient;
import com.smxy.myapplication.utils.ErrorHandler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LedgerManageFragment extends Fragment {

    private static final String PREFS_NAME = "user_data";
    private static final String KEY_TOKEN = "token";
    private static final String KEY_CURRENT_LEDGER_ID = "current_ledger_id";
    private static final String KEY_CURRENT_LEDGER_NAME = "current_ledger_name";

    private RecyclerView recyclerView;
    private LedgerAdapter ledgerAdapter;
    private final List<LedgerBook> ledgerList = new ArrayList<>();
    private TextView tvEmpty;
    private MaterialButton btnAddLedger;
    private ApiService apiService;
    private String authToken;
    private int currentLedgerId = 1;

    // 预设颜色列表
    private final String[] presetColors = {
            "#4CAF50", "#2196F3", "#FF9800", "#9C27B0",
            "#F44336", "#00BCD4", "#795548", "#E91E63",
            "#3F51B5", "#009688", "#FF5722", "#607D8B"
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_ledger_manage, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getActivity() != null) {
            getActivity().setTitle(R.string.ledger_manage_title);
        }

        initViews(view);
        initApiService();
        loadToken();
        loadCurrentLedger();
        setupListeners();
        loadLedgers();
    }

    private void initViews(View view) {
        recyclerView = view.findViewById(R.id.recycler_view_ledgers);
        tvEmpty = view.findViewById(R.id.tv_empty);
        btnAddLedger = view.findViewById(R.id.btn_add_ledger);

        if (recyclerView != null) {
            recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        }
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
    }

    private void setupListeners() {
        if (btnAddLedger != null) {
            btnAddLedger.setOnClickListener(v -> showAddLedgerDialog());
        }
    }

    private void loadLedgers() {
        if (!ErrorHandler.isFragmentValid(this)) return;
        if (apiService == null) return;
        if (!ErrorHandler.isTokenValid(authToken)) {
            ErrorHandler.showToast(getContext(), R.string.please_login_first);
            return;
        }

        String authHeader = "Bearer " + authToken;
        Call<LedgerResponse> call = apiService.getLedgers(authHeader);
        call.enqueue(new Callback<LedgerResponse>() {
            @Override
            public void onResponse(@NonNull Call<LedgerResponse> call, @NonNull Response<LedgerResponse> response) {
                if (!ErrorHandler.isFragmentValid(LedgerManageFragment.this)) return;

                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    List<LedgerBook> ledgers = response.body().getLedgers();
                    if (ledgers != null) {
                        ledgerList.clear();
                        ledgerList.addAll(ledgers);
                    }
                } else {
                    if (response.code() == 401) {
                        ErrorHandler.showToast(getContext(), R.string.please_login_first);
                    }
                }
                updateUI();
            }

            @Override
            public void onFailure(@NonNull Call<LedgerResponse> call, @NonNull Throwable t) {
                if (!ErrorHandler.isFragmentValid(LedgerManageFragment.this)) return;
                ErrorHandler.showToast(getContext(), "加载账本失败: " + t.getMessage());
                updateUI();
            }
        });
    }

    private void showAddLedgerDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_ledger, null);

        EditText etLedgerName = dialogView.findViewById(R.id.et_ledger_name);
        LinearLayout colorContainer = dialogView.findViewById(R.id.color_container);

        // 保存选中的颜色
        final String[] selectedColor = {presetColors[0]};

        // 清空容器
        colorContainer.removeAllViews();

        // 创建颜色选择按钮
        for (String color : presetColors) {
            View colorView = new View(requireContext());
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(80, 80);
            params.setMargins(8, 8, 8, 8);
            colorView.setLayoutParams(params);

            GradientDrawable drawable = new GradientDrawable();
            drawable.setShape(GradientDrawable.OVAL);
            drawable.setColor(Color.parseColor(color));
            colorView.setBackground(drawable);

            colorView.setOnClickListener(v -> {
                selectedColor[0] = color;
                // 高亮选中的颜色
                for (int i = 0; i < colorContainer.getChildCount(); i++) {
                    View child = colorContainer.getChildAt(i);
                    GradientDrawable childDrawable = (GradientDrawable) child.getBackground();
                    if (child == v) {
                        childDrawable.setStroke(4, Color.WHITE);
                    } else {
                        childDrawable.setStroke(0, Color.TRANSPARENT);
                    }
                }
            });

            colorContainer.addView(colorView);
        }

        builder.setTitle(R.string.create_ledger)
                .setView(dialogView)
                .setPositiveButton(R.string.create_ledger, (dialog, which) -> {
                    String name = etLedgerName.getText().toString().trim();

                    if (name.isEmpty()) {
                        ErrorHandler.showToast(getContext(), R.string.ledger_name_empty);
                        return;
                    }
                    createLedger(name, selectedColor[0]);
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void createLedger(String name, String color) {
        if (!ErrorHandler.isTokenValid(authToken)) {
            ErrorHandler.showToast(getContext(), R.string.please_login_first);
            return;
        }

        String authHeader = "Bearer " + authToken;
        Map<String, Object> ledgerData = new HashMap<>();
        ledgerData.put("name", name);
        ledgerData.put("color", color);

        Call<ApiResponse> call = apiService.createLedger(authHeader, ledgerData);
        call.enqueue(new Callback<ApiResponse>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse> call, @NonNull Response<ApiResponse> response) {
                if (!ErrorHandler.isFragmentValid(LedgerManageFragment.this)) return;

                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    ErrorHandler.showToast(getContext(), R.string.ledger_create_success);
                    loadLedgers();
                } else {
                    String errorMsg = getString(R.string.ledger_create_failed);
                    if (response.body() != null && response.body().getMessage() != null) {
                        errorMsg = response.body().getMessage();
                    }
                    ErrorHandler.showToast(getContext(), errorMsg);
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse> call, @NonNull Throwable t) {
                ErrorHandler.showToast(getContext(), "网络错误: " + t.getMessage());
            }
        });
    }

    private void showEditLedgerDialog(LedgerBook ledger) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_edit_ledger, null);

        EditText etLedgerName = dialogView.findViewById(R.id.et_ledger_name);
        LinearLayout colorContainer = dialogView.findViewById(R.id.color_container);

        etLedgerName.setText(ledger.getName());

        // 保存选中的颜色
        final String[] selectedColor = {ledger.getColor()};

        // 清空容器
        colorContainer.removeAllViews();

        // 创建颜色选择按钮
        for (String color : presetColors) {
            View colorView = new View(requireContext());
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(80, 80);
            params.setMargins(8, 8, 8, 8);
            colorView.setLayoutParams(params);

            GradientDrawable drawable = new GradientDrawable();
            drawable.setShape(GradientDrawable.OVAL);
            drawable.setColor(Color.parseColor(color));
            colorView.setBackground(drawable);

            // 如果是当前账本的颜色，高亮显示
            if (color.equals(ledger.getColor())) {
                drawable.setStroke(4, Color.WHITE);
            }

            final String currentColor = color;
            colorView.setOnClickListener(v -> {
                selectedColor[0] = currentColor;
                // 高亮选中的颜色
                for (int i = 0; i < colorContainer.getChildCount(); i++) {
                    View child = colorContainer.getChildAt(i);
                    GradientDrawable childDrawable = (GradientDrawable) child.getBackground();
                    if (child == v) {
                        childDrawable.setStroke(4, Color.WHITE);
                    } else {
                        childDrawable.setStroke(0, Color.TRANSPARENT);
                    }
                }
            });

            colorContainer.addView(colorView);
        }

        builder.setTitle(R.string.edit_ledger)
                .setView(dialogView)
                .setPositiveButton(R.string.save, (dialog, which) -> {
                    String name = etLedgerName.getText().toString().trim();

                    if (name.isEmpty()) {
                        ErrorHandler.showToast(getContext(), R.string.ledger_name_empty);
                        return;
                    }
                    updateLedger(ledger.getId(), name, selectedColor[0]);
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void updateLedger(int ledgerId, String name, String color) {
        if (!ErrorHandler.isTokenValid(authToken)) return;

        String authHeader = "Bearer " + authToken;
        Map<String, Object> ledgerData = new HashMap<>();
        ledgerData.put("name", name);
        ledgerData.put("color", color);

        Call<ApiResponse> call = apiService.updateLedger(authHeader, ledgerId, ledgerData);
        call.enqueue(new Callback<ApiResponse>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse> call, @NonNull Response<ApiResponse> response) {
                if (!ErrorHandler.isFragmentValid(LedgerManageFragment.this)) return;

                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    ErrorHandler.showToast(getContext(), R.string.ledger_update_success);
                    loadLedgers();
                    if (currentLedgerId == ledgerId) {
                        saveCurrentLedger(ledgerId, name);
                    }
                } else {
                    ErrorHandler.showToast(getContext(), R.string.ledger_update_failed);
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse> call, @NonNull Throwable t) {
                ErrorHandler.showToast(getContext(), "网络错误: " + t.getMessage());
            }
        });
    }

    private void confirmDeleteLedger(LedgerBook ledger) {
        if (ledger.isDefault()) {
            ErrorHandler.showToast(getContext(), R.string.ledger_default_cannot_delete);
            return;
        }

        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.delete_ledger)
                .setMessage(String.format(getString(R.string.ledger_delete_confirm), ledger.getName()))
                .setPositiveButton(R.string.delete, (dialog, which) -> deleteLedger(ledger))
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void deleteLedger(LedgerBook ledger) {
        if (!ErrorHandler.isTokenValid(authToken)) return;

        String authHeader = "Bearer " + authToken;
        Call<ApiResponse> call = apiService.deleteLedger(authHeader, ledger.getId());
        call.enqueue(new Callback<ApiResponse>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse> call, @NonNull Response<ApiResponse> response) {
                if (!ErrorHandler.isFragmentValid(LedgerManageFragment.this)) return;

                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    ErrorHandler.showToast(getContext(), R.string.ledger_delete_success);
                    loadLedgers();
                } else {
                    ErrorHandler.showToast(getContext(), R.string.ledger_delete_failed);
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse> call, @NonNull Throwable t) {
                ErrorHandler.showToast(getContext(), "网络错误: " + t.getMessage());
            }
        });
    }

    private void updateUI() {
        if (!ErrorHandler.isFragmentValid(this)) return;

        if (ledgerAdapter == null) {
            ledgerAdapter = new LedgerAdapter(ledgerList, currentLedgerId);
            ledgerAdapter.setOnLedgerClickListener(ledger -> {
                saveCurrentLedger(ledger.getId(), ledger.getName());
                ErrorHandler.showToast(getContext(), String.format(getString(R.string.ledger_switch_success), ledger.getName()));
            });
            ledgerAdapter.setOnLedgerEditListener(this::showEditLedgerDialog);
            ledgerAdapter.setOnLedgerDeleteListener(this::confirmDeleteLedger);
            if (recyclerView != null) {
                recyclerView.setAdapter(ledgerAdapter);
            }
        } else {
            ledgerAdapter.updateData(ledgerList, currentLedgerId);
        }

        boolean isEmpty = ledgerList.isEmpty();
        if (tvEmpty != null) {
            tvEmpty.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        }
        if (recyclerView != null) {
            recyclerView.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        }
    }

    static class LedgerAdapter extends RecyclerView.Adapter<LedgerAdapter.ViewHolder> {
        private List<LedgerBook> ledgers;
        private int currentLedgerId;
        private OnLedgerClickListener clickListener;
        private OnLedgerEditListener editListener;
        private OnLedgerDeleteListener deleteListener;

        interface OnLedgerClickListener {
            void onLedgerClick(LedgerBook ledger);
        }

        interface OnLedgerEditListener {
            void onLedgerEdit(LedgerBook ledger);
        }

        interface OnLedgerDeleteListener {
            void onLedgerDelete(LedgerBook ledger);
        }

        LedgerAdapter(List<LedgerBook> ledgers, int currentLedgerId) {
            this.ledgers = ledgers;
            this.currentLedgerId = currentLedgerId;
        }

        void setOnLedgerClickListener(OnLedgerClickListener listener) { this.clickListener = listener; }
        void setOnLedgerEditListener(OnLedgerEditListener listener) { this.editListener = listener; }
        void setOnLedgerDeleteListener(OnLedgerDeleteListener listener) { this.deleteListener = listener; }

        void updateData(List<LedgerBook> newLedgers, int newCurrentId) {
            this.ledgers = newLedgers;
            this.currentLedgerId = newCurrentId;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_ledger, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            LedgerBook ledger = ledgers.get(position);

            // 从账本名称获取图标文字（前两个字）
            String name = ledger.getName();
            String iconText = name.length() >= 2 ? name.substring(0, 2) : name;
            holder.tvIcon.setText(iconText);
            holder.tvName.setText(ledger.getName());

            if (ledger.getId() == currentLedgerId) {
                holder.tvCurrent.setVisibility(View.VISIBLE);
                holder.tvCurrent.setText(R.string.ledger_current_mark);
            } else {
                holder.tvCurrent.setVisibility(View.GONE);
            }

            // 设置背景颜色
            GradientDrawable drawable = new GradientDrawable();
            drawable.setShape(GradientDrawable.OVAL);
            try {
                int color = Color.parseColor(ledger.getColor());
                drawable.setColor(color);
            } catch (Exception e) {
                drawable.setColor(Color.parseColor("#4CAF50"));
            }
            holder.iconContainer.setBackground(drawable);

            holder.itemView.setOnClickListener(v -> {
                if (clickListener != null) {
                    clickListener.onLedgerClick(ledger);
                }
            });

            holder.btnEdit.setOnClickListener(v -> {
                if (editListener != null) {
                    editListener.onLedgerEdit(ledger);
                }
            });

            holder.btnDelete.setOnClickListener(v -> {
                if (deleteListener != null) {
                    deleteListener.onLedgerDelete(ledger);
                }
            });

            holder.btnDelete.setVisibility(ledger.isDefault() ? View.GONE : View.VISIBLE);
        }

        @Override
        public int getItemCount() {
            return ledgers.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            MaterialCardView cardView;
            TextView tvIcon;
            TextView tvName;
            TextView tvCurrent;
            ImageButton btnEdit;
            ImageButton btnDelete;
            View iconContainer;

            ViewHolder(@NonNull View itemView) {
                super(itemView);
                cardView = (MaterialCardView) itemView;
                tvIcon = itemView.findViewById(R.id.tv_ledger_icon);
                tvName = itemView.findViewById(R.id.tv_ledger_name);
                tvCurrent = itemView.findViewById(R.id.tv_current_ledger);
                btnEdit = itemView.findViewById(R.id.btn_edit_ledger);
                btnDelete = itemView.findViewById(R.id.btn_delete_ledger);
                iconContainer = itemView.findViewById(R.id.ledger_icon_container);
            }
        }
    }
}