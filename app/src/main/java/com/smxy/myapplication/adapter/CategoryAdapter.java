package com.smxy.myapplication.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.smxy.myapplication.R;
import com.smxy.myapplication.utils.ErrorHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.ViewHolder> {

    private List<Map.Entry<String, Double>> entries = new ArrayList<>();
    private double totalExpense;

    public CategoryAdapter(Map<String, Double> data, double totalExpense) {
        if (data != null) {
            this.entries = new ArrayList<>(data.entrySet());
            this.entries.sort((a, b) -> b.getValue().compareTo(a.getValue()));
        }
        this.totalExpense = totalExpense;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_category, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        if (entries == null || position >= entries.size()) return;

        Map.Entry<String, Double> entry = entries.get(position);
        if (entry == null) return;

        String name = entry.getKey();
        double amount = entry.getValue();
        int percent = totalExpense > 0 ? (int) ((amount / totalExpense) * 100) : 0;

        if (holder.tvCategoryName != null) {
            holder.tvCategoryName.setText(ErrorHandler.getSafeString(name, "未知"));
        }
        if (holder.tvCategoryAmount != null) {
            holder.tvCategoryAmount.setText(String.format(Locale.CHINA, "¥%.2f", amount));
        }
        if (holder.tvCategoryPercent != null) {
            holder.tvCategoryPercent.setText(percent + "%");
        }
        if (holder.progressBar != null) {
            holder.progressBar.setProgress(percent);
        }
    }

    @Override
    public int getItemCount() {
        return entries != null ? entries.size() : 0;
    }

    public void updateData(Map<String, Double> data, double totalExpense) {
        if (data != null) {
            this.entries = new ArrayList<>(data.entrySet());
            this.entries.sort((a, b) -> b.getValue().compareTo(a.getValue()));
        } else {
            this.entries.clear();
        }
        this.totalExpense = totalExpense;
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvCategoryName;
        TextView tvCategoryAmount;
        TextView tvCategoryPercent;
        ProgressBar progressBar;

        ViewHolder(View itemView) {
            super(itemView);
            tvCategoryName = itemView.findViewById(R.id.tv_category_name);
            tvCategoryAmount = itemView.findViewById(R.id.tv_category_amount);
            tvCategoryPercent = itemView.findViewById(R.id.tv_category_percent);
            progressBar = itemView.findViewById(R.id.progress_bar);
        }
    }
}