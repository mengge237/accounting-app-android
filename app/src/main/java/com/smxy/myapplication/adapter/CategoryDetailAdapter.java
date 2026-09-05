package com.smxy.myapplication.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.smxy.myapplication.R;
import com.smxy.myapplication.model.AccountType;
import com.smxy.myapplication.utils.ErrorHandler;

import java.util.List;
import java.util.Locale;

public class CategoryDetailAdapter extends RecyclerView.Adapter<CategoryDetailAdapter.ViewHolder> {

    private final List<AccountType> expenseTypes;
    private double totalExpense = 0;

    public CategoryDetailAdapter(List<AccountType> expenseTypes) {
        this.expenseTypes = expenseTypes;
        if (expenseTypes != null) {
            for (AccountType type : expenseTypes) {
                if (type != null) {
                    totalExpense += type.getTotalAmount();
                }
            }
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_category_detail, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        if (expenseTypes == null || position >= expenseTypes.size()) return;

        AccountType type = expenseTypes.get(position);
        if (type == null) return;

        double percentage = totalExpense > 0 ? (type.getTotalAmount() / totalExpense) * 100 : 0;

        if (holder.tvCategoryName != null) {
            holder.tvCategoryName.setText(ErrorHandler.getSafeString(type.getTypeName(), "未知"));
        }
        if (holder.tvAmount != null) {
            holder.tvAmount.setText(String.format(Locale.CHINA, "¥%.2f", type.getTotalAmount()));
        }
        if (holder.tvCount != null) {
            holder.tvCount.setText(String.format(Locale.CHINA, "%d笔", type.getRecordCount()));
        }
        if (holder.tvPercentage != null) {
            holder.tvPercentage.setText(String.format(Locale.CHINA, "%.1f%%", percentage));
        }
        if (holder.progressBar != null) {
            holder.progressBar.setProgress((int) percentage);
        }

        if (holder.progressBar != null && holder.progressBar.getContext() != null) {
            if (type.getTotalAmount() > 1000) {
                holder.progressBar.setProgressTintList(android.content.res.ColorStateList.valueOf(0xFFF44336));
            } else if (type.getTotalAmount() > 500) {
                holder.progressBar.setProgressTintList(android.content.res.ColorStateList.valueOf(0xFFFF9800));
            } else {
                holder.progressBar.setProgressTintList(android.content.res.ColorStateList.valueOf(0xFF4CAF50));
            }
        }
    }

    @Override
    public int getItemCount() {
        return expenseTypes != null ? expenseTypes.size() : 0;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvCategoryName, tvAmount, tvCount, tvPercentage;
        ProgressBar progressBar;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCategoryName = itemView.findViewById(R.id.tv_category_name);
            tvAmount = itemView.findViewById(R.id.tv_amount);
            tvCount = itemView.findViewById(R.id.tv_count);
            tvPercentage = itemView.findViewById(R.id.tv_percentage);
            progressBar = itemView.findViewById(R.id.progress_bar);
        }
    }
}