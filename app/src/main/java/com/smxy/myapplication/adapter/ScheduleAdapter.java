package com.smxy.myapplication.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.smxy.myapplication.R;
import com.smxy.myapplication.model.Schedule;
import com.smxy.myapplication.utils.ErrorHandler;

import java.util.List;

public class ScheduleAdapter extends RecyclerView.Adapter<ScheduleAdapter.ScheduleViewHolder> {

    private List<Schedule> scheduleList;
    private final OnScheduleClickListener listener;

    public interface OnScheduleClickListener {
        void onScheduleClick(Schedule schedule);
    }

    public ScheduleAdapter(List<Schedule> scheduleList, OnScheduleClickListener listener) {
        this.scheduleList = scheduleList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ScheduleViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_schedule, parent, false);
        return new ScheduleViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ScheduleViewHolder holder, int position) {
        if (scheduleList != null && position < scheduleList.size()) {
            Schedule schedule = scheduleList.get(position);
            if (schedule != null) {
                holder.bind(schedule);
            }
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null && scheduleList != null && position < scheduleList.size()) {
                listener.onScheduleClick(scheduleList.get(position));
            }
        });
    }

    @Override
    public int getItemCount() {
        return scheduleList != null ? scheduleList.size() : 0;
    }

    public void updateSchedules(List<Schedule> newList) {
        this.scheduleList = newList;
        notifyDataSetChanged();
    }

    static class ScheduleViewHolder extends RecyclerView.ViewHolder {
        private final CardView cardView;
        private final TextView tvTitle;
        private final TextView tvType;
        private final TextView tvTime;
        private final TextView tvPriority;
        private final TextView tvStatus;

        ScheduleViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = (CardView) itemView;
            tvTitle = itemView.findViewById(R.id.tv_schedule_title);
            tvType = itemView.findViewById(R.id.tv_schedule_type);
            tvTime = itemView.findViewById(R.id.tv_schedule_time);
            tvPriority = itemView.findViewById(R.id.tv_schedule_priority);
            tvStatus = itemView.findViewById(R.id.tv_schedule_status);
        }

        void bind(Schedule schedule) {
            if (schedule == null) return;

            if (tvTitle != null) {
                tvTitle.setText(ErrorHandler.getSafeString(schedule.getTitle(), "无标题"));
            }

            String type = schedule.getType();
            String typeText;
            if ("recipe".equals(type)) typeText = "🍳 做菜";
            else if ("event".equals(type)) typeText = "📅 事件";
            else if ("salary".equals(type)) typeText = "💰 工资";
            else if ("bill".equals(type)) typeText = "📄 账单";
            else if ("reminder".equals(type)) typeText = "⏰ 提醒";
            else typeText = "📌 " + ErrorHandler.getSafeString(type, "其他");
            if (tvType != null) tvType.setText(typeText);

            String timeText = schedule.getScheduledDate();
            if (schedule.getScheduledTime() != null && !schedule.getScheduledTime().isEmpty()) {
                String time = schedule.getScheduledTime();
                if (time.length() > 5) {
                    time = time.substring(0, 5);
                }
                timeText = schedule.getScheduledDate() + " " + time;
            }
            if (tvTime != null) tvTime.setText(ErrorHandler.getSafeString(timeText, ""));

            int priority = schedule.getPriority();
            if (tvPriority != null) {
                if (priority == 1) {
                    tvPriority.setText("🟢 低");
                    tvPriority.setTextColor(0xFF4CAF50);
                } else if (priority == 2) {
                    tvPriority.setText("🟡 中");
                    tvPriority.setTextColor(0xFFFF9800);
                } else {
                    tvPriority.setText("🔴 高");
                    tvPriority.setTextColor(0xFFF44336);
                }
            }

            if (schedule.isCompleted()) {
                if (tvStatus != null) {
                    tvStatus.setText("✓ 已完成");
                    tvStatus.setTextColor(0xFF4CAF50);
                    tvStatus.setBackgroundResource(R.drawable.bg_status_completed);
                }
                if (cardView != null) {
                    cardView.setCardBackgroundColor(0xFFF0FDF4);
                }
                if (tvTitle != null) {
                    tvTitle.setTextColor(0xFF166534);
                    tvTitle.setPaintFlags(tvTitle.getPaintFlags() | android.graphics.Paint.STRIKE_THRU_TEXT_FLAG);
                }
            } else {
                if (tvStatus != null) {
                    tvStatus.setText("○ 待完成");
                    tvStatus.setTextColor(0xFF9E9E9E);
                    tvStatus.setBackgroundResource(R.drawable.bg_status_pending);
                }
                if (cardView != null) {
                    cardView.setCardBackgroundColor(0xFFFFFFFF);
                }
                if (tvTitle != null) {
                    tvTitle.setTextColor(0xFF333333);
                    tvTitle.setPaintFlags(tvTitle.getPaintFlags() & (~android.graphics.Paint.STRIKE_THRU_TEXT_FLAG));
                }
            }
        }
    }
}