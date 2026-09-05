package com.smxy.myapplication.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.switchmaterial.SwitchMaterial;
import com.smxy.myapplication.R;
import com.smxy.myapplication.model.ComponentConfig;
import com.smxy.myapplication.utils.ErrorHandler;

import java.util.List;

public class ComponentManageAdapter extends RecyclerView.Adapter<ComponentManageAdapter.ViewHolder> {

    private final Context context;
    private List<ComponentConfig> components;
    private final OnComponentChangeListener listener;

    public interface OnComponentChangeListener {
        void onVisibilityChanged(ComponentConfig component, boolean isVisible);
        void onRename(ComponentConfig component, String newName);
        void onDelete(ComponentConfig component, int position);
    }

    public ComponentManageAdapter(Context context, List<ComponentConfig> components, OnComponentChangeListener listener) {
        this.context = context;
        this.components = components;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_component_manage, parent, false);
        return new ViewHolder(view, context, listener);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        if (components != null && position < components.size()) {
            ComponentConfig component = components.get(position);
            if (component != null) {
                holder.bind(component, position);
            }
        }
    }

    @Override
    public int getItemCount() {
        return components != null ? components.size() : 0;
    }

    public void updateComponents(List<ComponentConfig> newComponents) {
        this.components = newComponents;
        notifyDataSetChanged();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvTitle;
        private final TextView tvType;
        private final SwitchMaterial switchVisible;
        private final ImageView ivRename;
        private final ImageView ivDelete;
        private final Context context;
        private final OnComponentChangeListener listener;

        public ViewHolder(@NonNull View itemView, Context context, OnComponentChangeListener listener) {
            super(itemView);
            this.context = context;
            this.listener = listener;

            tvTitle = itemView.findViewById(R.id.tv_component_title);
            tvType = itemView.findViewById(R.id.tv_component_type);
            switchVisible = itemView.findViewById(R.id.switch_visible);
            ivRename = itemView.findViewById(R.id.iv_rename);
            ivDelete = itemView.findViewById(R.id.iv_delete);
        }

        void bind(ComponentConfig component, int position) {
            if (component == null) return;

            if (tvTitle != null) tvTitle.setText(ErrorHandler.getSafeString(component.getTitle(), "未命名"));
            if (tvType != null) tvType.setText(getTypeDisplayName(component.getType()));
            if (switchVisible != null) switchVisible.setChecked(component.isVisible());

            if (ivDelete != null) {
                ivDelete.setVisibility(component.isSystemDefault() ? View.GONE : View.VISIBLE);
            }

            if (switchVisible != null) {
                switchVisible.setOnCheckedChangeListener(null);
                switchVisible.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    if (listener != null) {
                        listener.onVisibilityChanged(component, isChecked);
                    }
                });
            }

            if (ivRename != null) {
                ivRename.setOnClickListener(v -> showRenameDialog(component));
            }
            if (ivDelete != null) {
                ivDelete.setOnClickListener(v -> showDeleteDialog(component, position));
            }
        }

        private String getTypeDisplayName(String type) {
            if (type == null) return "未知";
            switch (type) {
                case "number": return "数字";
                case "text": return "文本";
                case "select": return "选择";
                case "datetime": return "日期时间";
                case "radio": return "单选";
                case "tag": return "标签";
                case "switch": return "开关";
                default: return type;
            }
        }

        private void showRenameDialog(ComponentConfig component) {
            if (context == null || component == null) return;

            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setTitle("重命名组件");

            final EditText input = new EditText(context);
            input.setText(ErrorHandler.getSafeString(component.getTitle(), ""));
            input.setSelection(input.getText().length());
            builder.setView(input);

            builder.setPositiveButton("确定", (dialog, which) -> {
                String newName = input.getText().toString().trim();
                if (!newName.isEmpty() && listener != null) {
                    listener.onRename(component, newName);
                } else if (context != null) {
                    ErrorHandler.showToast(context, "名称不能为空");
                }
            });
            builder.setNegativeButton("取消", null);
            builder.show();
        }

        private void showDeleteDialog(ComponentConfig component, int position) {
            if (context == null) return;

            new AlertDialog.Builder(context)
                    .setTitle("删除组件")
                    .setMessage("确定要删除 \"" + ErrorHandler.getSafeString(component.getTitle(), "这个组件") + "\" 吗？")
                    .setPositiveButton("删除", (dialog, which) -> {
                        if (listener != null) {
                            listener.onDelete(component, position);
                        }
                    })
                    .setNegativeButton("取消", null)
                    .show();
        }
    }
}