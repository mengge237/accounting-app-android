package com.smxy.myapplication.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.smxy.myapplication.R;
import com.smxy.myapplication.utils.ErrorHandler;

import java.util.ArrayList;
import java.util.List;

public class StepAdapter extends RecyclerView.Adapter<StepAdapter.StepViewHolder> {

    private List<String> steps;
    private OnStepDeleteListener deleteListener;

    public interface OnStepDeleteListener {
        void onDelete(int position);
    }

    public StepAdapter(List<String> steps) {
        this.steps = steps != null ? steps : new ArrayList<>();
    }

    public void setOnDeleteListener(OnStepDeleteListener listener) {
        this.deleteListener = listener;
    }

    @NonNull
    @Override
    public StepViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_step, parent, false);
        return new StepViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull StepViewHolder holder, int position) {
        if (steps != null && position < steps.size()) {
            String step = steps.get(position);
            if (step != null) {
                if (holder.tvStepNumber != null) {
                    holder.tvStepNumber.setText(String.valueOf(position + 1));
                }
                if (holder.tvStepContent != null) {
                    holder.tvStepContent.setText(step);
                }
            }
        }

        if (holder.btnDelete != null) {
            holder.btnDelete.setOnClickListener(v -> {
                if (deleteListener != null) {
                    deleteListener.onDelete(position);
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return steps != null ? steps.size() : 0;
    }

    public void addStep(String step) {
        if (steps != null && step != null && !step.isEmpty()) {
            steps.add(step);
            notifyItemInserted(steps.size() - 1);
        }
    }

    public void removeStep(int position) {
        if (steps != null && position >= 0 && position < steps.size()) {
            steps.remove(position);
            notifyItemRemoved(position);
            notifyItemRangeChanged(position, steps.size() - position);
        }
    }

    public List<String> getSteps() {
        return steps;
    }

    public void setSteps(List<String> newSteps) {
        this.steps = newSteps != null ? newSteps : new ArrayList<>();
        notifyDataSetChanged();
    }

    static class StepViewHolder extends RecyclerView.ViewHolder {
        TextView tvStepNumber;
        TextView tvStepContent;
        TextView btnDelete;

        StepViewHolder(@NonNull View itemView) {
            super(itemView);
            tvStepNumber = itemView.findViewById(R.id.tv_step_number);
            tvStepContent = itemView.findViewById(R.id.tv_step_content);
            btnDelete = itemView.findViewById(R.id.btn_delete_step);
        }
    }
}