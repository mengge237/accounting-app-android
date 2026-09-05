package com.smxy.myapplication.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class Recipe {
    private int id;
    private int userId;
    private String name;
    private String description;
    private List<String> ingredients;
    private List<String> steps;
    private int cookTime;
    private String difficulty;
    private String coverImage;
    private boolean isDefault;
    private String createdAt;
    private String updatedAt;

    @SerializedName("cuisine_id")
    private int cuisineId;

    private boolean isFavorited = false;
    private transient boolean isFavoriteRequesting = false;

    public Recipe() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getTitle() { return name; }
    public void setTitle(String title) { this.name = title; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public List<String> getIngredients() { return ingredients; }
    public void setIngredients(List<String> ingredients) { this.ingredients = ingredients; }

    public List<String> getSteps() { return steps; }
    public void setSteps(List<String> steps) { this.steps = steps; }

    public int getCookTime() { return cookTime; }
    public void setCookTime(int cookTime) { this.cookTime = cookTime; }

    public String getDifficulty() { return difficulty; }
    public void setDifficulty(String difficulty) { this.difficulty = difficulty; }

    public String getCoverImage() { return coverImage; }
    public void setCoverImage(String coverImage) { this.coverImage = coverImage; }

    public boolean isDefault() { return isDefault; }
    public void setDefault(boolean isDefault) { this.isDefault = isDefault; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }

    public int getCuisineId() { return cuisineId; }
    public void setCuisineId(int cuisineId) { this.cuisineId = cuisineId; }

    public boolean isFavorited() { return isFavorited; }
    public void setFavorited(boolean favorited) { isFavorited = favorited; }

    public boolean isFavoriteRequesting() { return isFavoriteRequesting; }
    public void setFavoriteRequesting(boolean favoriteRequesting) { isFavoriteRequesting = favoriteRequesting; }
}