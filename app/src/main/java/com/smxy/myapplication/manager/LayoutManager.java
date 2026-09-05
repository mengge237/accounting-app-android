package com.smxy.myapplication.manager;

import android.content.Context;
import android.content.SharedPreferences;

import com.smxy.myapplication.model.ComponentConfig;
import com.smxy.myapplication.utils.ErrorHandler;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class LayoutManager {
    private static final String PREF_NAME = "layout_config";
    private static final String KEY_COMPONENTS = "components_config";

    private final Context context;
    private final SharedPreferences prefs;
    private final Gson gson;
    private List<ComponentConfig> components;

    public LayoutManager(Context context) {
        this.context = context.getApplicationContext();
        this.prefs = this.context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        this.gson = new Gson();
        loadConfig();
    }

    private void loadConfig() {
        String json = prefs.getString(KEY_COMPONENTS, "");
        if (json == null || json.isEmpty()) {
            components = getDefaultComponents();
            saveConfig();
        } else {
            try {
                Type type = new TypeToken<List<ComponentConfig>>() {}.getType();
                components = gson.fromJson(json, type);
                if (components == null) {
                    components = getDefaultComponents();
                    saveConfig();
                }
            } catch (Exception e) {
                components = getDefaultComponents();
                saveConfig();
            }
        }
    }

    public void saveConfig() {
        if (components != null) {
            String json = gson.toJson(components);
            prefs.edit().putString(KEY_COMPONENTS, json).apply();
        }
    }

    private List<ComponentConfig> getDefaultComponents() {
        List<ComponentConfig> list = new ArrayList<>();

        ComponentConfig amount = new ComponentConfig("amount", "number", "金额", 0, true);
        amount.setSystemDefault(true);
        list.add(amount);

        ComponentConfig type = new ComponentConfig("type", "radio", "类型", 1, true);
        type.setSystemDefault(true);
        list.add(type);

        ComponentConfig category = new ComponentConfig("category", "select", "分类", 2, true);
        category.setSystemDefault(true);
        List<String> categories = new ArrayList<>();
        categories.add("餐饮");
        categories.add("购物");
        categories.add("交通");
        categories.add("娱乐");
        categories.add("医疗");
        categories.add("工资");
        categories.add("奖金");
        category.setOptions(categories);
        list.add(category);

        ComponentConfig datetime = new ComponentConfig("datetime", "datetime", "日期时间", 3, true);
        datetime.setSystemDefault(true);
        list.add(datetime);

        ComponentConfig note = new ComponentConfig("note", "text", "备注", 4, true);
        note.setSystemDefault(true);
        list.add(note);

        ComponentConfig tag = new ComponentConfig("tag", "tag", "标签", 5, false);
        tag.setSystemDefault(true);
        list.add(tag);

        ComponentConfig hideDefaultRecipes = new ComponentConfig("hide_default_recipes", "switch", "隐藏预设菜谱", 6, true);
        hideDefaultRecipes.setSystemDefault(false);
        hideDefaultRecipes.setChecked(false);
        list.add(hideDefaultRecipes);

        return list;
    }

    public List<ComponentConfig> getVisibleComponents() {
        if (components == null) {
            return new ArrayList<>();
        }
        List<ComponentConfig> visible = new ArrayList<>();
        for (ComponentConfig comp : components) {
            if (comp != null && comp.isVisible()) {
                visible.add(comp);
            }
        }
        return visible;
    }

    public void addComponent(ComponentConfig component) {
        if (component == null || components == null) return;
        component.setOrder(components.size());
        components.add(component);
        saveConfig();
    }

    public void removeComponent(String id) {
        if (id == null || components == null) return;
        for (int i = 0; i < components.size(); i++) {
            if (components.get(i) != null && id.equals(components.get(i).getId())) {
                components.remove(i);
                break;
            }
        }
        for (int i = 0; i < components.size(); i++) {
            if (components.get(i) != null) {
                components.get(i).setOrder(i);
            }
        }
        saveConfig();
    }

    public void toggleVisibility(String id) {
        if (id == null || components == null) return;
        for (ComponentConfig comp : components) {
            if (comp != null && id.equals(comp.getId())) {
                comp.setVisible(!comp.isVisible());
                break;
            }
        }
        saveConfig();
    }

    public void toggleSwitch(String id) {
        if (id == null || components == null) return;
        for (ComponentConfig comp : components) {
            if (comp != null && id.equals(comp.getId())) {
                comp.setChecked(!comp.isChecked());
                break;
            }
        }
        saveConfig();
    }

    public boolean isSwitchChecked(String id) {
        if (id == null || components == null) return false;
        for (ComponentConfig comp : components) {
            if (comp != null && id.equals(comp.getId())) {
                return comp.isChecked();
            }
        }
        return false;
    }

    public void updateOrder(int fromPosition, int toPosition) {
        if (components == null) return;
        if (fromPosition < 0 || toPosition < 0 || fromPosition >= components.size() || toPosition >= components.size()) {
            return;
        }
        ComponentConfig item = components.remove(fromPosition);
        components.add(toPosition, item);
        for (int i = 0; i < components.size(); i++) {
            if (components.get(i) != null) {
                components.get(i).setOrder(i);
            }
        }
        saveConfig();
    }

    public boolean hasCustomLayout() {
        String json = prefs.getString(KEY_COMPONENTS, "");
        if (json == null || json.isEmpty()) {
            return false;
        }

        try {
            Type type = new TypeToken<List<ComponentConfig>>() {}.getType();
            List<ComponentConfig> savedComponents = gson.fromJson(json, type);
            List<ComponentConfig> defaultComponents = getDefaultComponents();

            if (savedComponents == null || defaultComponents == null) {
                return false;
            }
            if (savedComponents.size() != defaultComponents.size()) {
                return true;
            }

            for (int i = 0; i < savedComponents.size(); i++) {
                ComponentConfig saved = savedComponents.get(i);
                ComponentConfig def = defaultComponents.get(i);
                if (saved == null || def == null) continue;

                if (!saved.getId().equals(def.getId())) {
                    return true;
                }
                if (saved.isVisible() != def.isVisible()) {
                    return true;
                }
                if (saved.getType() != null && saved.getType().equals("switch") && saved.isChecked() != def.isChecked()) {
                    return true;
                }
            }
        } catch (Exception e) {
            return false;
        }
        return false;
    }

    public List<ComponentConfig> getCurrentComponents() {
        if (hasCustomLayout()) {
            return getVisibleComponents();
        } else {
            return getDefaultVisibleComponents();
        }
    }

    public List<ComponentConfig> getDefaultVisibleComponents() {
        List<ComponentConfig> visible = new ArrayList<>();
        List<ComponentConfig> defaults = getDefaultComponents();
        for (ComponentConfig comp : defaults) {
            if (comp != null && comp.isVisible()) {
                visible.add(comp);
            }
        }
        return visible;
    }

    public List<ComponentConfig> getAllComponents() {
        return components != null ? new ArrayList<>(components) : new ArrayList<>();
    }

    public void saveAllComponents(List<ComponentConfig> newComponents) {
        if (newComponents != null) {
            this.components = newComponents;
            saveConfig();
        }
    }

    public void setComponentVisibility(String id, boolean visible) {
        if (id == null || components == null) return;
        for (ComponentConfig comp : components) {
            if (comp != null && id.equals(comp.getId())) {
                comp.setVisible(visible);
                break;
            }
        }
        saveConfig();
    }

    public void resetToDefault() {
        components = getDefaultComponents();
        saveConfig();
    }

    public void updateComponentOrder(List<ComponentConfig> newOrder) {
        if (newOrder != null) {
            this.components = newOrder;
            for (int i = 0; i < components.size(); i++) {
                if (components.get(i) != null) {
                    components.get(i).setOrder(i);
                }
            }
            saveConfig();
        }
    }

    public void renameComponent(String id, String newName) {
        if (id == null || newName == null || components == null) return;
        for (ComponentConfig comp : components) {
            if (comp != null && id.equals(comp.getId())) {
                comp.setTitle(newName);
                break;
            }
        }
        saveConfig();
    }

    public boolean isHideDefaultRecipes() {
        return isSwitchChecked("hide_default_recipes");
    }
}