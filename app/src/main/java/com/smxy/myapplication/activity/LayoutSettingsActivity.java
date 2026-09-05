package com.smxy.myapplication.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.ItemTouchHelper;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;
import com.smxy.myapplication.R;
import com.smxy.myapplication.manager.LayoutManager;
import com.smxy.myapplication.model.ComponentConfig;
import com.smxy.myapplication.utils.ErrorHandler;

import java.util.ArrayList;
import java.util.List;

public class LayoutSettingsActivity extends AppCompatActivity {

    private LayoutManager layoutManager;
    private RecyclerView recyclerView;
    private ComponentAdapter adapter;
    private List<ComponentConfig> allComponents;
    private boolean hasChanges = false;
    private FloatingActionButton fabAddComponent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_layout_settings);

        layoutManager = new LayoutManager(this);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("组件管理");
        }

        initViews();
        loadComponents();
        setupDragAndDrop();
    }

    private void initViews() {
        recyclerView = findViewById(R.id.component_recycler_view);
        if (recyclerView != null) {
            recyclerView.setLayoutManager(new LinearLayoutManager(this));
        }

        Button btnResetDefault = findViewById(R.id.btn_reset_default);
        fabAddComponent = findViewById(R.id.fab_add_component);

        if (btnResetDefault != null) {
            btnResetDefault.setOnClickListener(v -> resetToDefault());
        }
        if (fabAddComponent != null) {
            fabAddComponent.setOnClickListener(v -> showAddComponentDialog());
        }
    }

    private void loadComponents() {
        if (layoutManager != null) {
            allComponents = layoutManager.getAllComponents();
        }

        if (allComponents == null || allComponents.isEmpty()) {
            allComponents = getDefaultComponents();
            if (layoutManager != null) {
                layoutManager.saveAllComponents(allComponents);
            }
        }

        adapter = new ComponentAdapter(allComponents);
        if (recyclerView != null) {
            recyclerView.setAdapter(adapter);
        }
    }

    private List<ComponentConfig> getDefaultComponents() {
        List<ComponentConfig> list = new ArrayList<>();

        ComponentConfig amount = new ComponentConfig("amount", "number", "金额", 0, true);
        amount.setSystemDefault(true);
        list.add(amount);

        ComponentConfig note = new ComponentConfig("note", "text", "备注", 1, true);
        note.setSystemDefault(true);
        list.add(note);

        ComponentConfig type = new ComponentConfig("type", "radio", "类型", 2, true);
        type.setSystemDefault(true);
        list.add(type);

        ComponentConfig date = new ComponentConfig("date", "date", "日期", 3, true);
        date.setSystemDefault(true);
        list.add(date);

        ComponentConfig category = new ComponentConfig("category", "select", "分类", 4, false);
        category.setSystemDefault(true);
        list.add(category);

        ComponentConfig tag = new ComponentConfig("tag", "text", "标签", 5, false);
        tag.setSystemDefault(true);
        list.add(tag);

        return list;
    }

    private void setupDragAndDrop() {
        if (recyclerView == null) return;

        ItemTouchHelper touchHelper = new ItemTouchHelper(
                new ItemTouchHelper.SimpleCallback(
                        ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0) {
                    @Override
                    public boolean onMove(@NonNull RecyclerView recyclerView,
                                          @NonNull RecyclerView.ViewHolder viewHolder,
                                          @NonNull RecyclerView.ViewHolder target) {
                        int fromPos = viewHolder.getAdapterPosition();
                        int toPos = target.getAdapterPosition();

                        if (fromPos < 0 || toPos < 0 || allComponents == null) return false;

                        if (fromPos < toPos) {
                            for (int i = fromPos; i < toPos; i++) {
                                java.util.Collections.swap(allComponents, i, i + 1);
                            }
                        } else {
                            for (int i = fromPos; i > toPos; i--) {
                                java.util.Collections.swap(allComponents, i, i - 1);
                            }
                        }
                        if (adapter != null) {
                            adapter.notifyItemMoved(fromPos, toPos);
                        }
                        hasChanges = true;
                        return true;
                    }

                    @Override
                    public void onSelectedChanged(RecyclerView.ViewHolder viewHolder, int actionState) {
                        super.onSelectedChanged(viewHolder, actionState);
                        if (actionState == ItemTouchHelper.ACTION_STATE_DRAG && viewHolder != null) {
                            viewHolder.itemView.setAlpha(0.7f);
                        }
                    }

                    @Override
                    public void clearView(@NonNull RecyclerView recyclerView,
                                          @NonNull RecyclerView.ViewHolder viewHolder) {
                        super.clearView(recyclerView, viewHolder);
                        viewHolder.itemView.setAlpha(1.0f);
                    }

                    @Override
                    public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                    }
                });
        touchHelper.attachToRecyclerView(recyclerView);
    }

    private void resetToDefault() {
        new AlertDialog.Builder(this)
                .setTitle("恢复默认")
                .setMessage("确定要恢复默认布局设置吗？这将重置所有组件的显示状态和顺序。")
                .setPositiveButton("确定", (dialog, which) -> {
                    allComponents = getDefaultComponents();
                    adapter = new ComponentAdapter(allComponents);
                    if (recyclerView != null) {
                        recyclerView.setAdapter(adapter);
                    }
                    hasChanges = true;
                    ErrorHandler.showToast(this, "已恢复默认布局，请点击保存");
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void saveAndBack() {
        if (layoutManager != null && allComponents != null) {
            // 更新顺序
            for (int i = 0; i < allComponents.size(); i++) {
                allComponents.get(i).setOrder(i);
            }
            layoutManager.saveAllComponents(allComponents);
        }

        getSharedPreferences("layout_config", Context.MODE_PRIVATE)
                .edit()
                .putBoolean("custom_layout_exists", true)
                .apply();

        ErrorHandler.showToast(this, "布局设置已保存");
        finish();
    }

    private void showAddComponentDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_component, null);

        TextInputEditText etComponentId = dialogView.findViewById(R.id.et_component_id);
        TextInputEditText etComponentTitle = dialogView.findViewById(R.id.et_component_title);
        Spinner spinnerComponentType = dialogView.findViewById(R.id.spinner_component_type);

        // 自动生成ID
        if (etComponentId != null) {
            etComponentId.setText("custom_" + System.currentTimeMillis());
        }

        new AlertDialog.Builder(this)
                .setTitle("添加组件")
                .setView(dialogView)
                .setPositiveButton("添加", (dialog, which) -> {
                    String title = etComponentTitle != null && etComponentTitle.getText() != null
                            ? etComponentTitle.getText().toString().trim() : "";
                    String type = spinnerComponentType != null && spinnerComponentType.getSelectedItem() != null
                            ? spinnerComponentType.getSelectedItem().toString() : "text";

                    if (title.isEmpty()) {
                        ErrorHandler.showToast(this, "请输入组件名称");
                        return;
                    }

                    String id = "custom_" + System.currentTimeMillis();
                    int order = allComponents != null ? allComponents.size() : 0;
                    ComponentConfig newComponent = new ComponentConfig(id, type, title, order, true);
                    newComponent.setSystemDefault(false);

                    if (layoutManager != null) {
                        layoutManager.addComponent(newComponent);
                    }
                    loadComponents();
                    hasChanges = true;
                    ErrorHandler.showToast(this, "已添加组件: " + title);
                })
                .setNegativeButton("取消", null)
                .show();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull android.view.MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            if (hasChanges) {
                new AlertDialog.Builder(this)
                        .setTitle("未保存的更改")
                        .setMessage("您有未保存的更改，确定要退出吗？")
                        .setPositiveButton("保存并退出", (dialog, which) -> saveAndBack())
                        .setNegativeButton("不保存", (dialog, which) -> finish())
                        .setNeutralButton("取消", null)
                        .show();
            } else {
                finish();
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if (hasChanges) {
            new AlertDialog.Builder(this)
                    .setTitle("未保存的更改")
                    .setMessage("您有未保存的更改，确定要退出吗？")
                    .setPositiveButton("保存并退出", (dialog, which) -> saveAndBack())
                    .setNegativeButton("不保存", (dialog, which) -> super.onBackPressed())
                    .setNeutralButton("取消", null)
                    .show();
        } else {
            super.onBackPressed();
        }
    }

    // ========== Adapter ==========
    class ComponentAdapter extends RecyclerView.Adapter<ComponentAdapter.ViewHolder> {
        private List<ComponentConfig> components;

        ComponentAdapter(List<ComponentConfig> components) {
            this.components = components;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = getLayoutInflater().inflate(R.layout.item_component_setting, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            if (components == null || position >= components.size()) return;

            ComponentConfig component = components.get(position);
            if (component == null) return;

            if (holder.tvName != null) holder.tvName.setText(component.getTitle());
            if (holder.tvType != null) holder.tvType.setText(getTypeDisplayName(component.getType()));
            if (holder.switchVisible != null) holder.switchVisible.setChecked(component.isVisible());

            // 系统默认组件不能删除
            if (component.isSystemDefault()) {
                if (holder.tvName != null) {
                    holder.tvName.setTextColor(android.graphics.Color.parseColor("#666666"));
                }
                if (holder.btnDelete != null) {
                    holder.btnDelete.setVisibility(View.GONE);
                }
            } else {
                if (holder.tvName != null) {
                    holder.tvName.setTextColor(android.graphics.Color.parseColor("#333333"));
                }
                if (holder.btnDelete != null) {
                    holder.btnDelete.setVisibility(View.VISIBLE);
                    holder.btnDelete.setOnClickListener(v -> {
                        showDeleteDialog(component, position);
                    });
                }
            }

            if (holder.ivDrag != null) holder.ivDrag.setVisibility(View.VISIBLE);

            if (holder.switchVisible != null) {
                holder.switchVisible.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    component.setVisible(isChecked);
                    hasChanges = true;
                });
            }
        }

        @Override
        public int getItemCount() {
            return components != null ? components.size() : 0;
        }

        private void showDeleteDialog(ComponentConfig component, int position) {
            new AlertDialog.Builder(LayoutSettingsActivity.this)
                    .setTitle("删除组件")
                    .setMessage("确定要删除 \"" + component.getTitle() + "\" 吗？")
                    .setPositiveButton("删除", (dialog, which) -> {
                        if (layoutManager != null) {
                            layoutManager.removeComponent(component.getId());
                        }
                        loadComponents();
                        hasChanges = true;
                        ErrorHandler.showToast(LayoutSettingsActivity.this, "已删除组件");
                    })
                    .setNegativeButton("取消", null)
                    .show();
        }

        private String getTypeDisplayName(String type) {
            if (type == null) return "未知";
            switch (type) {
                case "number": return "数字输入";
                case "text": return "文本输入";
                case "radio": return "单选按钮";
                case "date": return "日期选择";
                case "select": return "下拉选择";
                case "switch": return "开关";
                case "datetime": return "日期时间";
                case "tag": return "标签";
                default: return type;
            }
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvName, tvType;
            SwitchMaterial switchVisible;
            Button btnDelete;
            ImageView ivDrag;

            ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvName = itemView.findViewById(R.id.tv_component_name);
                tvType = itemView.findViewById(R.id.tv_component_type);
                switchVisible = itemView.findViewById(R.id.switch_visible);
                btnDelete = itemView.findViewById(R.id.btn_delete_component);
                ivDrag = itemView.findViewById(R.id.iv_drag);
            }
        }
    }
}