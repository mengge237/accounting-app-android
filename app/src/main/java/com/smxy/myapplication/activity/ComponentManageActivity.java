package com.smxy.myapplication.activity;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.switchmaterial.SwitchMaterial;
import com.smxy.myapplication.R;
import com.smxy.myapplication.manager.SettingsManager;
import com.smxy.myapplication.utils.ErrorHandler;

import java.util.ArrayList;
import java.util.List;

public class ComponentManageActivity extends AppCompatActivity {

    private SettingsManager settingsManager;
    private RecyclerView recyclerView;
    private ComponentAdapter adapter;
    private List<Object> items = new ArrayList<>();
    private boolean hasChanges = false;

    private static final int TYPE_GROUP = 0;
    private static final int TYPE_MODULE = 1;

    private static final int GROUP_ACCOUNTING = 0;
    private static final int GROUP_OTHER = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_component_manage);

        settingsManager = new SettingsManager(this);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(getString(R.string.component_manage_title));
        }

        initViews();
        buildItems();
    }

    private void initViews() {
        recyclerView = findViewById(R.id.component_recycler_view);
        if (recyclerView != null) {
            recyclerView.setLayoutManager(new LinearLayoutManager(this));
        }

        Button btnResetDefault = findViewById(R.id.btn_reset_default);
        Button btnSave = findViewById(R.id.btn_save);

        if (btnResetDefault != null) {
            btnResetDefault.setOnClickListener(v -> resetToDefault());
        }
        if (btnSave != null) {
            btnSave.setOnClickListener(v -> saveAndExit());
        }
    }

    private void buildItems() {
        items.clear();

        items.add(new GroupItem(GROUP_ACCOUNTING, getString(R.string.accounting_group_title), true));
        items.add(new ModuleItem("accounting", getString(R.string.module_accounting), getString(R.string.desc_accounting), settingsManager.showAccounting(), GROUP_ACCOUNTING, true, false));
        items.add(new ModuleItem("ledger_manage", getString(R.string.module_ledger_manage), getString(R.string.desc_ledger_manage), settingsManager.getUserSettingShowLedgerManage(), GROUP_ACCOUNTING, false, true));
        items.add(new ModuleItem("statistics", getString(R.string.module_statistics), getString(R.string.desc_statistics), settingsManager.getUserSettingShowStatistics(), GROUP_ACCOUNTING, false, true));
        items.add(new ModuleItem("records", getString(R.string.module_records), getString(R.string.desc_records), settingsManager.getUserSettingShowRecords(), GROUP_ACCOUNTING, false, true));
        items.add(new ModuleItem("export", getString(R.string.module_export), getString(R.string.desc_export), settingsManager.getUserSettingShowExport(), GROUP_ACCOUNTING, false, true));

        items.add(new GroupItem(GROUP_OTHER, getString(R.string.other_group_title), true));
        items.add(new ModuleItem("voice", getString(R.string.module_voice), getString(R.string.desc_voice), settingsManager.showVoice(), GROUP_OTHER, false, false));
        items.add(new ModuleItem("recipe", getString(R.string.module_recipe), getString(R.string.desc_recipe), settingsManager.showRecipe(), GROUP_OTHER, false, false));
        items.add(new ModuleItem("my_recipes", getString(R.string.module_my_recipes), getString(R.string.desc_my_recipes), settingsManager.showMyRecipes(), GROUP_OTHER, false, false));
        items.add(new ModuleItem("schedule", getString(R.string.module_schedule), getString(R.string.desc_schedule), settingsManager.showSchedule(), GROUP_OTHER, false, false));
        items.add(new ModuleItem("music", getString(R.string.module_music), getString(R.string.desc_music), settingsManager.showMusic(), GROUP_OTHER, false, false));
        items.add(new ModuleItem("stock", getString(R.string.module_self_selected), getString(R.string.desc_stock), settingsManager.showStock(), GROUP_OTHER, false, false));

        adapter = new ComponentAdapter(items);
        if (recyclerView != null) {
            recyclerView.setAdapter(adapter);
        }
    }

    private void resetToDefault() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.component_reset_confirm_title)
                .setMessage(R.string.component_reset_confirm_message)
                .setPositiveButton(R.string.confirm, (dialog, which) -> {
                    settingsManager.resetToDefault();
                    buildItems();
                    hasChanges = true;
                    ErrorHandler.showToast(this, R.string.component_reset_success);
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void saveAndExit() {
        for (Object item : items) {
            if (item instanceof ModuleItem) {
                ModuleItem module = (ModuleItem) item;
                switch (module.id) {
                    case "accounting":
                        settingsManager.setShowAccounting(module.visible);
                        break;
                    case "ledger_manage":
                        settingsManager.setShowLedgerManage(module.visible);
                        break;
                    case "statistics":
                        settingsManager.setShowStatistics(module.visible);
                        break;
                    case "records":
                        settingsManager.setShowRecords(module.visible);
                        break;
                    case "export":
                        settingsManager.setShowExport(module.visible);
                        break;
                    case "voice":
                        settingsManager.setShowVoice(module.visible);
                        break;
                    case "recipe":
                        settingsManager.setShowRecipe(module.visible);
                        break;
                    case "my_recipes":
                        settingsManager.setShowMyRecipes(module.visible);
                        break;
                    case "schedule":
                        settingsManager.setShowSchedule(module.visible);
                        break;
                    case "music":
                        settingsManager.setShowMusic(module.visible);
                        break;
                    case "stock":
                        settingsManager.setShowStock(module.visible);
                        break;
                }
            }
        }

        ErrorHandler.showToast(this, R.string.component_save_success);

        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(this::finish, 500);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull android.view.MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            if (hasChanges) {
                new AlertDialog.Builder(this)
                        .setTitle(R.string.unsaved_changes_title)
                        .setMessage(R.string.unsaved_changes_message)
                        .setPositiveButton(R.string.save_and_exit, (dialog, which) -> saveAndExit())
                        .setNegativeButton(R.string.discard, (dialog, which) -> finish())
                        .setNeutralButton(R.string.cancel, null)
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
                    .setTitle(R.string.unsaved_changes_title)
                    .setMessage(R.string.unsaved_changes_message)
                    .setPositiveButton(R.string.save_and_exit, (dialog, which) -> saveAndExit())
                    .setNegativeButton(R.string.discard, (dialog, which) -> super.onBackPressed())
                    .setNeutralButton(R.string.cancel, null)
                    .show();
        } else {
            super.onBackPressed();
        }
    }

    private void toggleGroup(int groupId) {
        for (Object item : items) {
            if (item instanceof GroupItem) {
                GroupItem group = (GroupItem) item;
                if (group.id == groupId) {
                    group.expanded = !group.expanded;
                    break;
                }
            }
        }
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }

    private boolean isGroupExpanded(int groupId) {
        for (Object item : items) {
            if (item instanceof GroupItem) {
                GroupItem group = (GroupItem) item;
                if (group.id == groupId) {
                    return group.expanded;
                }
            }
        }
        return true;
    }

    private boolean isAccountingEnabled() {
        for (Object item : items) {
            if (item instanceof ModuleItem) {
                ModuleItem module = (ModuleItem) item;
                if (module.id.equals("accounting")) {
                    return module.visible;
                }
            }
        }
        return false;
    }

    private void onAccountingToggled(boolean enabled) {
        for (Object item : items) {
            if (item instanceof ModuleItem) {
                ModuleItem module = (ModuleItem) item;
                if (module.dependent) {
                    module.visible = enabled;
                }
            }
        }
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
        if (enabled) {
            ErrorHandler.showToast(this, "记账模块已开启，依赖模块已自动开启");
        } else {
            ErrorHandler.showToast(this, "记账模块已关闭，依赖模块已被禁用");
        }
    }

    // ========== 数据模型 ==========

    static class GroupItem {
        int id;
        String title;
        boolean expanded;

        GroupItem(int id, String title, boolean expanded) {
            this.id = id;
            this.title = title;
            this.expanded = expanded;
        }
    }

    static class ModuleItem {
        String id;
        String title;
        String desc;
        boolean visible;
        int groupId;
        boolean isMain;
        boolean dependent;

        ModuleItem(String id, String title, String desc, boolean visible, int groupId, boolean isMain, boolean dependent) {
            this.id = id;
            this.title = title;
            this.desc = desc;
            this.visible = visible;
            this.groupId = groupId;
            this.isMain = isMain;
            this.dependent = dependent;
        }
    }

    // ========== Adapter ==========

    class ComponentAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        private final List<Object> items;

        ComponentAdapter(List<Object> items) {
            this.items = items;
        }

        @Override
        public int getItemViewType(int position) {
            Object item = getRawItem(position);
            if (item instanceof GroupItem) {
                return TYPE_GROUP;
            }
            return TYPE_MODULE;
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            if (viewType == TYPE_GROUP) {
                View view = inflater.inflate(R.layout.item_component_group, parent, false);
                return new GroupViewHolder(view);
            }
            View view = inflater.inflate(R.layout.item_component_module, parent, false);
            return new ModuleViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            Object item = getRawItem(position);

            if (item == null) return;

            if (holder instanceof GroupViewHolder) {
                GroupViewHolder groupHolder = (GroupViewHolder) holder;
                GroupItem group = (GroupItem) item;
                groupHolder.bind(group);
            } else if (holder instanceof ModuleViewHolder) {
                ModuleViewHolder moduleHolder = (ModuleViewHolder) holder;
                ModuleItem module = (ModuleItem) item;
                moduleHolder.bind(module);
            }
        }

        @Override
        public int getItemCount() {
            int count = 0;
            for (Object item : items) {
                if (item instanceof GroupItem) {
                    count++;
                } else if (item instanceof ModuleItem) {
                    ModuleItem module = (ModuleItem) item;
                    if (isGroupExpanded(module.groupId)) {
                        count++;
                    }
                }
            }
            return count;
        }

        private Object getRawItem(int position) {
            int index = 0;
            for (Object item : items) {
                if (item instanceof GroupItem) {
                    if (index == position) return item;
                    index++;
                } else if (item instanceof ModuleItem) {
                    ModuleItem module = (ModuleItem) item;
                    if (isGroupExpanded(module.groupId)) {
                        if (index == position) return item;
                        index++;
                    }
                }
            }
            return null;
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position, @NonNull List<Object> payloads) {
            super.onBindViewHolder(holder, position, payloads);
        }
    }

    class GroupViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle;
        ImageView ivExpand;

        GroupViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tv_group_title);
            ivExpand = itemView.findViewById(R.id.iv_group_expand);
        }

        void bind(GroupItem group) {
            tvTitle.setText(group.title);
            ivExpand.setRotation(group.expanded ? 0 : 180);
            itemView.setOnClickListener(v -> toggleGroup(group.id));
        }
    }

    class ModuleViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle;
        TextView tvDesc;
        SwitchMaterial switchModule;

        ModuleViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tv_module_title);
            tvDesc = itemView.findViewById(R.id.tv_module_desc);
            switchModule = itemView.findViewById(R.id.switch_module);
        }

        void bind(ModuleItem module) {
            tvTitle.setText(module.title);
            tvDesc.setText(module.desc);

            switchModule.setOnCheckedChangeListener(null);
            switchModule.setChecked(module.visible);

            boolean accountingEnabled = isAccountingEnabled();
            boolean enabled = !module.dependent || accountingEnabled;
            switchModule.setEnabled(enabled);
            itemView.setAlpha(enabled ? 1.0f : 0.5f);

            switchModule.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (module.dependent && !accountingEnabled) {
                    switchModule.setChecked(false);
                    ErrorHandler.showToast(ComponentManageActivity.this, "请先开启记账模块");
                    return;
                }

                module.visible = isChecked;
                hasChanges = true;

                if (module.isMain) {
                    onAccountingToggled(isChecked);
                } else {
                    String message = isChecked ? getString(R.string.module_shown) : getString(R.string.module_hidden);
                    ErrorHandler.showToast(ComponentManageActivity.this, String.format(message, module.title));
                }
            });
        }
    }
}