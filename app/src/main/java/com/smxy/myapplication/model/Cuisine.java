package com.smxy.myapplication.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class Cuisine {
    @SerializedName("id")
    private int id;

    @SerializedName("name")
    private String name;

    @SerializedName("description")
    private String description;

    private List<String> keywords;
    private boolean isSelected;

    public Cuisine(int id, String name, String description, List<String> keywords) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.keywords = keywords;
        this.isSelected = false;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public List<String> getKeywords() { return keywords; }
    public void setKeywords(List<String> keywords) { this.keywords = keywords; }

    public boolean isSelected() { return isSelected; }
    public void setSelected(boolean selected) { isSelected = selected; }
}