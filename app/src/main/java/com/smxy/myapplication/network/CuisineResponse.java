package com.smxy.myapplication.network;

import com.smxy.myapplication.model.Cuisine;
import java.util.List;

public class CuisineResponse {
    private boolean success;
    private List<Cuisine> cuisines;
    private String message;

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public List<Cuisine> getCuisines() { return cuisines; }
    public void setCuisines(List<Cuisine> cuisines) { this.cuisines = cuisines; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}