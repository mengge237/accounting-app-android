package com.smxy.myapplication.adapter;

import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;
import com.smxy.myapplication.R;
import com.smxy.myapplication.model.Cuisine;
import com.smxy.myapplication.utils.ErrorHandler;

import java.util.List;

public class CuisineAdapter extends RecyclerView.Adapter<CuisineAdapter.CuisineViewHolder> {

    private List<Cuisine> cuisines;
    private final OnCuisineClickListener listener;
    private int selectedPosition = 0;

    public interface OnCuisineClickListener {
        void onCuisineClick(Cuisine cuisine, int position);
        void onCuisineLongClick(Cuisine cuisine);
    }

    public CuisineAdapter(List<Cuisine> cuisines, OnCuisineClickListener listener) {
        this.cuisines = cuisines;
        this.listener = listener;
        if (cuisines != null && !cuisines.isEmpty() && cuisines.get(0) != null) {
            cuisines.get(0).setSelected(true);
        }
    }

    @NonNull
    @Override
    public CuisineViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Chip chip = createChip(parent);
        return new CuisineViewHolder(chip);
    }

    private Chip createChip(ViewGroup parent) {
        if (parent == null || parent.getContext() == null) return new Chip(parent.getContext());

        Chip chip = new Chip(parent.getContext());
        chip.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        chip.setCheckable(true);
        chip.setChipBackgroundColor(ContextCompat.getColorStateList(parent.getContext(), R.color.chip_bg_selector));
        chip.setTextColor(ContextCompat.getColorStateList(parent.getContext(), R.color.chip_text_selector));

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        int marginPx = (int) (8 * parent.getContext().getResources().getDisplayMetrics().density);
        lp.setMarginEnd(marginPx);
        chip.setLayoutParams(lp);

        return chip;
    }

    @Override
    public void onBindViewHolder(@NonNull CuisineViewHolder holder, int position) {
        if (cuisines != null && position < cuisines.size()) {
            Cuisine cuisine = cuisines.get(position);
            if (cuisine != null) {
                holder.bind(cuisine, position);
            }
        }
    }

    @Override
    public int getItemCount() {
        return cuisines == null ? 0 : cuisines.size();
    }

    public void setSelectedPosition(int position) {
        if (cuisines == null || cuisines.isEmpty()) return;

        int previousPosition = selectedPosition;
        if (previousPosition >= 0 && previousPosition < cuisines.size() && cuisines.get(previousPosition) != null) {
            cuisines.get(previousPosition).setSelected(false);
            notifyItemChanged(previousPosition);
        }

        selectedPosition = position;
        if (selectedPosition >= 0 && selectedPosition < cuisines.size() && cuisines.get(selectedPosition) != null) {
            cuisines.get(selectedPosition).setSelected(true);
            notifyItemChanged(selectedPosition);
        }
    }

    public void updateCuisines(List<Cuisine> newCuisines) {
        this.cuisines = newCuisines;
        if (cuisines != null && !cuisines.isEmpty()) {
            if (cuisines.get(0) != null) {
                cuisines.get(0).setSelected(true);
            }
            selectedPosition = 0;
        }
        notifyDataSetChanged();
    }

    class CuisineViewHolder extends RecyclerView.ViewHolder {
        private final Chip chip;

        CuisineViewHolder(@NonNull Chip chip) {
            super(chip);
            this.chip = chip;
        }

        void bind(Cuisine cuisine, int position) {
            if (cuisine == null || chip == null) return;

            chip.setText(ErrorHandler.getSafeString(cuisine.getName(), "未知"));
            chip.setChecked(cuisine.isSelected());

            chip.setOnClickListener(v -> {
                if (listener != null) {
                    setSelectedPosition(position);
                    listener.onCuisineClick(cuisine, position);
                }
            });

            chip.setOnLongClickListener(v -> {
                if (listener != null) {
                    listener.onCuisineLongClick(cuisine);
                }
                return true;
            });
        }
    }
}