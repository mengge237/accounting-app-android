package com.smxy.myapplication.adapter;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.smxy.myapplication.R;
import com.smxy.myapplication.model.Record;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class RecordAdapter extends RecyclerView.Adapter<RecordAdapter.ViewHolder> {

    private List<Record> recordList;
    private OnItemClickListener onItemClickListener;

    public interface OnItemClickListener {
        void onItemClick(Record record);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.onItemClickListener = listener;
    }

    public RecordAdapter(List<Record> recordList) {
        this.recordList = recordList != null ? recordList : new ArrayList<>();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_record, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        if (recordList == null || position >= recordList.size()) return;

        Record record = recordList.get(position);
        if (record == null) return;

        // 设置备注
        String note = record.getNote();
        holder.tvNote.setText(note == null || note.isEmpty() ? "无备注" : note);

        // 设置日期
        holder.tvDate.setText(formatDate(record.getRecordDate()));

        // 设置分类名称 - 确保 tv_category 存在
        if (holder.tvCategory != null) {
            holder.tvCategory.setText(record.getTypeName());
        }

        // 根据收入/支出设置不同颜色
        if (record.isIncome()) {
            holder.tvAmount.setTextColor(Color.parseColor("#4CAF50"));
            holder.tvAmount.setText(String.format(Locale.CHINA, "+¥%.2f", record.getAmount()));
            holder.tvType.setText("收入");
            holder.tvType.setTextColor(Color.parseColor("#4CAF50"));
            if (holder.ivTypeIcon != null) {
                holder.ivTypeIcon.setImageResource(R.drawable.ic_income);
            }
        } else {
            holder.tvAmount.setTextColor(Color.parseColor("#F44336"));
            holder.tvAmount.setText(String.format(Locale.CHINA, "-¥%.2f", record.getAmount()));
            holder.tvType.setText("支出");
            holder.tvType.setTextColor(Color.parseColor("#F44336"));
            if (holder.ivTypeIcon != null) {
                holder.ivTypeIcon.setImageResource(R.drawable.ic_expense);
            }
        }

        // 设置点击事件
        holder.itemView.setOnClickListener(v -> {
            if (onItemClickListener != null) {
                onItemClickListener.onItemClick(record);
            }
        });
    }

    private String formatDate(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) {
            return "";
        }
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.CHINA);
            Date date = inputFormat.parse(dateStr);
            if (date != null) {
                SimpleDateFormat outputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA);
                return outputFormat.format(date);
            }
        } catch (Exception ignored) {
            try {
                SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA);
                Date date = inputFormat.parse(dateStr);
                if (date != null) {
                    SimpleDateFormat outputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA);
                    return outputFormat.format(date);
                }
            } catch (Exception ignored2) {
                if (dateStr.length() >= 10) {
                    return dateStr.substring(0, 10);
                }
            }
        }
        return dateStr;
    }

    public void updateData(List<Record> newList) {
        this.recordList = newList != null ? newList : new ArrayList<>();
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return recordList != null ? recordList.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvAmount;
        TextView tvNote;
        TextView tvDate;
        TextView tvType;
        TextView tvCategory;
        ImageView ivTypeIcon;
        View viewIconBg;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvAmount = itemView.findViewById(R.id.tv_amount);
            tvNote = itemView.findViewById(R.id.tv_note);
            tvDate = itemView.findViewById(R.id.tv_date);
            tvType = itemView.findViewById(R.id.tv_type);
            tvCategory = itemView.findViewById(R.id.tv_category);
            ivTypeIcon = itemView.findViewById(R.id.iv_type_icon);
            viewIconBg = itemView.findViewById(R.id.view_icon_bg);
        }
    }
}