// RecipesResponse.java
package com.smxy.myapplication.network;

import com.smxy.myapplication.model.Recipe;
import java.util.List;

public class RecipesResponse {
    private boolean success;
    private String message;
    private List<Recipe> recipes;

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public List<Recipe> getRecipes() { return recipes; }
    public void setRecipes(List<Recipe> recipes) { this.recipes = recipes; }
}

