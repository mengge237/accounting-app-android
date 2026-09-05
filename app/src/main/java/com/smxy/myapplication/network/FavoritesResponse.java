package com.smxy.myapplication.network;

import com.smxy.myapplication.model.Recipe;
import java.util.List;

public class FavoritesResponse {
    private boolean success;
    private String message;
    private List<Recipe> favorites;

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public List<Recipe> getFavorites() { return favorites; }
    public void setFavorites(List<Recipe> favorites) { this.favorites = favorites; }
}