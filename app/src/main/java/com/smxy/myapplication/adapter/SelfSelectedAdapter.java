package com.smxy.myapplication.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.smxy.myapplication.R;
import com.smxy.myapplication.model.SelfSelectedItem;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * 股票自选列表适配器
 */
public class SelfSelectedAdapter extends RecyclerView.Adapter<SelfSelectedAdapter.ViewHolder> {

    private List<SelfSelectedItem> itemList = new ArrayList<>();
    private OnItemClickListener onItemClickListener;
    private OnItemDeleteListener onItemDeleteListener;

    public interface OnItemClickListener {
        void onItemClick(SelfSelectedItem item);
    }

    public interface OnItemDeleteListener {
        void onDelete(SelfSelectedItem item, int position);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.onItemClickListener = listener;
    }

    public void setOnItemDeleteListener(OnItemDeleteListener listener) {
        this.onItemDeleteListener = listener;
    }

    public void setItemList(List<SelfSelectedItem> list) {
        this.itemList = list;
        notifyDataSetChanged();
    }

    public List<SelfSelectedItem> getItemList() {
        return itemList;
    }

    public int getItemPosition(SelfSelectedItem item) {
        return itemList.indexOf(item);
    }

    public void addItem(SelfSelectedItem item) {
        itemList.add(0, item);
        notifyItemInserted(0);
    }

    public void removeItem(int position) {
        if (position >= 0 && position < itemList.size()) {
            itemList.remove(position);
            notifyItemRemoved(position);
        }
    }

    public SelfSelectedItem getItem(int position) {
        return itemList.get(position);
    }

    @Override
    public int getItemCount() {
        return itemList.size();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_self_selected, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        SelfSelectedItem item = itemList.get(position);
        holder.bind(item);
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvCode;
        TextView tvName;
        TextView tvType;
        TextView tvBuyPrice;
        TextView tvCurrentPrice;
        TextView tvChangePercent;
        TextView tvChangeAmount;
        TextView tvRemark;
        TextView tvCreateTime;
        ImageView ivDelete;

        ViewHolder(View itemView) {
            super(itemView);
            tvCode = itemView.findViewById(R.id.tv_code);
            tvName = itemView.findViewById(R.id.tv_name);
            tvType = itemView.findViewById(R.id.tv_type);
            tvBuyPrice = itemView.findViewById(R.id.tv_buy_price);
            tvCurrentPrice = itemView.findViewById(R.id.tv_current_price);
            tvChangePercent = itemView.findViewById(R.id.tv_change_percent);
            tvChangeAmount = itemView.findViewById(R.id.tv_change_amount);
            tvRemark = itemView.findViewById(R.id.tv_remark);
            tvCreateTime = itemView.findViewById(R.id.tv_create_time);
            ivDelete = itemView.findViewById(R.id.iv_delete);
        }

        void bind(SelfSelectedItem item) {
            String code = item.getCode() != null ? item.getCode() : "";
            String name = item.getName() != null ? item.getName() : "";
            String type = item.getType() != null ? item.getType() : "";

            tvCode.setText(code);
            tvName.setText(name);
            tvType.setText(type);

            String buyPriceText = String.format(Locale.CHINA, "买入: %.2f", item.getBuyPrice());
            String currentPriceText = String.format(Locale.CHINA, "现价: %.2f", item.getCurrentPrice());
            tvBuyPrice.setText(buyPriceText);
            tvCurrentPrice.setText(currentPriceText);

            String percentText = item.getFormattedChangePercent();
            String amountText = item.getFormattedChangeAmount();
            tvChangePercent.setText(percentText);
            tvChangeAmount.setText("(" + amountText + ")");

            int colorRes;
            if (item.getCurrentPrice() > item.getBuyPrice()) {
                colorRes = android.R.color.holo_red_dark;
            } else if (item.getCurrentPrice() < item.getBuyPrice()) {
                colorRes = android.R.color.holo_green_dark;
            } else {
                colorRes = android.R.color.darker_gray;
            }
            int color = itemView.getContext().getColor(colorRes);
            tvChangePercent.setTextColor(color);
            tvChangeAmount.setTextColor(color);

            if (item.getRemark() != null && !item.getRemark().isEmpty()) {
                tvRemark.setVisibility(View.VISIBLE);
                String remarkText = "备注: " + item.getRemark();
                tvRemark.setText(remarkText);
            } else {
                tvRemark.setVisibility(View.GONE);
            }

            SimpleDateFormat sdf = new SimpleDateFormat("MM-dd HH:mm", Locale.CHINA);
            String timeText = "添加于: " + sdf.format(new Date(item.getCreateTime()));
            tvCreateTime.setText(timeText);

            itemView.setOnClickListener(v -> {
                if (onItemClickListener != null) {
                    onItemClickListener.onItemClick(item);
                }
            });

            ivDelete.setOnClickListener(v -> {
                if (onItemDeleteListener != null) {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        onItemDeleteListener.onDelete(item, position);
                    }
                }
            });
        }
    }
}