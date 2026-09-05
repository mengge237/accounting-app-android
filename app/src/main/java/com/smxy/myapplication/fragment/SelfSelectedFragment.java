package com.smxy.myapplication.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.smxy.myapplication.R;
import com.smxy.myapplication.adapter.SelfSelectedAdapter;
import com.smxy.myapplication.db.SelfSelectedDao;
import com.smxy.myapplication.dialog.AddSelfSelectedDialog;
import com.smxy.myapplication.model.SelfSelectedItem;
import com.smxy.myapplication.utils.ErrorHandler;

import java.util.List;
import java.util.Locale;

/**
 * 股票自选Fragment
 */
public class SelfSelectedFragment extends Fragment {

    private RecyclerView recyclerView;
    private TextView tvEmpty;
    private FloatingActionButton fabAdd;
    private SelfSelectedAdapter adapter;
    private SelfSelectedDao dao;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_self_selected, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initViews(view);
        setupRecyclerView();
        loadData();
        setupFab();
    }

    private void initViews(View view) {
        recyclerView = view.findViewById(R.id.recycler_view);
        tvEmpty = view.findViewById(R.id.tv_empty);
        fabAdd = view.findViewById(R.id.fab_add);
    }

    private void setupRecyclerView() {
        adapter = new SelfSelectedAdapter();
        if (getContext() != null) {
            recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        }
        recyclerView.setAdapter(adapter);

        adapter.setOnItemClickListener(this::showDetailDialog);
        adapter.setOnItemDeleteListener((item, position) -> {
            if (getContext() == null) return;
            new AlertDialog.Builder(getContext())
                    .setTitle("确认删除")
                    .setMessage("确定要删除「" + item.getName() + "」吗？")
                    .setPositiveButton("删除", (dialog, which) -> deleteItem(item, position))
                    .setNegativeButton("取消", null)
                    .show();
        });
    }

    private void setupFab() {
        fabAdd.setOnClickListener(v -> showAddDialog());
    }

    private void loadData() {
        if (dao == null && getContext() != null) {
            dao = new SelfSelectedDao(getContext());
        }
        if (dao == null) return;

        List<SelfSelectedItem> list = dao.getAll();
        adapter.setItemList(list);

        if (list.isEmpty()) {
            tvEmpty.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            tvEmpty.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }

    private void showAddDialog() {
        if (getContext() == null) return;

        AddSelfSelectedDialog dialog = new AddSelfSelectedDialog(getContext());
        dialog.setOnConfirmListener(item -> {
            if (getContext() == null) return;

            if (dao.exists(item.getCode())) {
                ErrorHandler.showToast(getContext(), "该股票代码已存在");
                return;
            }
            long id = dao.insert(item);
            if (id > 0) {
                item.setId(id);
                adapter.addItem(item);
                ErrorHandler.showToast(getContext(), "添加成功");

                if (tvEmpty.getVisibility() == View.VISIBLE) {
                    tvEmpty.setVisibility(View.GONE);
                    recyclerView.setVisibility(View.VISIBLE);
                }
            } else {
                ErrorHandler.showToast(getContext(), "添加失败");
            }
        });
        dialog.show();
    }

    private void showDetailDialog(SelfSelectedItem item) {
        if (getContext() == null || item == null) return;

        View dialogView = LayoutInflater.from(getContext())
                .inflate(R.layout.dialog_self_selected_detail, null);

        TextView tvCode = dialogView.findViewById(R.id.tv_detail_code);
        TextView tvName = dialogView.findViewById(R.id.tv_detail_name);
        TextView tvType = dialogView.findViewById(R.id.tv_detail_type);
        TextView tvBuyPrice = dialogView.findViewById(R.id.tv_detail_buy_price);
        TextView tvCurrentPrice = dialogView.findViewById(R.id.tv_detail_current_price);
        TextView tvChangePercent = dialogView.findViewById(R.id.tv_detail_change_percent);
        TextView tvChangeAmount = dialogView.findViewById(R.id.tv_detail_change_amount);
        TextView tvRemark = dialogView.findViewById(R.id.tv_detail_remark);
        Button btnUpdatePrice = dialogView.findViewById(R.id.btn_update_price);
        Button btnDelete = dialogView.findViewById(R.id.btn_delete);
        LinearLayout layoutPriceUpdate = dialogView.findViewById(R.id.layout_price_update);
        Button btnConfirmUpdate = dialogView.findViewById(R.id.btn_confirm_update);
        Button btnCancelUpdate = dialogView.findViewById(R.id.btn_cancel_update);
        TextInputEditText etNewPrice = dialogView.findViewById(R.id.et_new_price);

        String code = item.getCode() != null ? item.getCode() : "";
        String name = item.getName() != null ? item.getName() : "";
        String type = item.getType() != null ? item.getType() : "";

        tvCode.setText(code);
        tvName.setText(name);
        tvType.setText(type);

        String buyPriceText = String.format(Locale.CHINA, "买入价: %.2f", item.getBuyPrice());
        String currentPriceText = String.format(Locale.CHINA, "当前价: %.2f", item.getCurrentPrice());
        tvBuyPrice.setText(buyPriceText);
        tvCurrentPrice.setText(currentPriceText);
        tvChangePercent.setText(item.getFormattedChangePercent());
        tvChangeAmount.setText(item.getFormattedChangeAmount());

        if (item.getRemark() != null && !item.getRemark().isEmpty()) {
            String remarkText = "备注: " + item.getRemark();
            tvRemark.setText(remarkText);
            tvRemark.setVisibility(View.VISIBLE);
        } else {
            tvRemark.setVisibility(View.GONE);
        }

        int colorRes;
        if (item.getCurrentPrice() > item.getBuyPrice()) {
            colorRes = android.R.color.holo_red_dark;
        } else if (item.getCurrentPrice() < item.getBuyPrice()) {
            colorRes = android.R.color.holo_green_dark;
        } else {
            colorRes = android.R.color.darker_gray;
        }
        int color = getContext().getColor(colorRes);
        tvChangePercent.setTextColor(color);
        tvChangeAmount.setTextColor(color);

        AlertDialog dialog = new AlertDialog.Builder(getContext())
                .setView(dialogView)
                .setPositiveButton("关闭", null)
                .create();

        btnUpdatePrice.setOnClickListener(v -> {
            btnUpdatePrice.setVisibility(View.GONE);
            layoutPriceUpdate.setVisibility(View.VISIBLE);
        });

        btnConfirmUpdate.setOnClickListener(v -> {
            if (getContext() == null) return;

            String priceStr = etNewPrice.getText().toString().trim();
            if (priceStr.isEmpty()) {
                ErrorHandler.showToast(getContext(), "请输入当前价格");
                return;
            }
            try {
                double newPrice = Double.parseDouble(priceStr);
                item.setCurrentPrice(newPrice);
                int rows = dao.updateCurrentPrice(item.getId(), newPrice);
                if (rows > 0) {
                    ErrorHandler.showToast(getContext(), "更新成功");
                    loadData();
                    dialog.dismiss();
                } else {
                    ErrorHandler.showToast(getContext(), "更新失败");
                }
            } catch (NumberFormatException e) {
                ErrorHandler.showToast(getContext(), "请输入有效的数字");
            }
        });

        btnCancelUpdate.setOnClickListener(v -> {
            layoutPriceUpdate.setVisibility(View.GONE);
            btnUpdatePrice.setVisibility(View.VISIBLE);
        });

        btnDelete.setOnClickListener(v -> {
            dialog.dismiss();
            if (getContext() == null) return;
            new AlertDialog.Builder(getContext())
                    .setTitle("确认删除")
                    .setMessage("确定要删除「" + item.getName() + "」吗？")
                    .setPositiveButton("删除", (d, which) -> {
                        int position = adapter.getItemPosition(item);
                        if (position >= 0) {
                            deleteItem(item, position);
                        }
                    })
                    .setNegativeButton("取消", null)
                    .show();
        });

        dialog.show();
    }

    private void deleteItem(SelfSelectedItem item, int position) {
        if (getContext() == null) return;

        int rows = dao.delete(item.getId());
        if (rows > 0) {
            adapter.removeItem(position);
            ErrorHandler.showToast(getContext(), "删除成功");

            if (adapter.getItemCount() == 0) {
                tvEmpty.setVisibility(View.VISIBLE);
                recyclerView.setVisibility(View.GONE);
            }
        } else {
            ErrorHandler.showToast(getContext(), "删除失败");
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        loadData();
    }
}