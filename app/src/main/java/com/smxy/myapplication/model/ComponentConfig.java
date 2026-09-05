package com.smxy.myapplication.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class ComponentConfig implements Serializable {
    private String id;
    private String type;  // number, text, radio, date, time, datetime, select, category, tag, recipe_setting
    private String title;
    private int order;
    private boolean isVisible;
    private boolean isRequired;
    private int columnSpan;
    private boolean isSystemDefault;
    private List<String> options;
    private boolean checked;

    public ComponentConfig() {}

    public ComponentConfig(String id, String type, String title, int order, boolean isVisible) {
        this.id = id;
        this.type = type;
        this.title = title;
        this.order = order;
        this.isVisible = isVisible;
        this.isRequired = true;
        this.columnSpan = 1;
        this.isSystemDefault = true;
        this.options = new ArrayList<>();
        this.checked = false;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public int getOrder() { return order; }
    public void setOrder(int order) { this.order = order; }

    public boolean isVisible() { return isVisible; }
    public void setVisible(boolean visible) { isVisible = visible; }

    public boolean isRequired() { return isRequired; }
    public void setRequired(boolean required) { isRequired = required; }

    public int getColumnSpan() { return columnSpan; }
    public void setColumnSpan(int columnSpan) { this.columnSpan = columnSpan; }

    public boolean isSystemDefault() { return isSystemDefault; }
    public void setSystemDefault(boolean systemDefault) { isSystemDefault = systemDefault; }

    public List<String> getOptions() { return options; }
    public void setOptions(List<String> options) { this.options = options; }

    public boolean isChecked() { return checked; }
    public void setChecked(boolean checked) { this.checked = checked; }
}