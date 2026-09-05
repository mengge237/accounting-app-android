// ColorPreset.java
package com.smxy.myapplication.model;

public class ColorPreset {
    private String name;
    private int primaryColor;
    private int backgroundColor;
    private int surfaceColor;

    public ColorPreset(String name, int primaryColor, int backgroundColor, int surfaceColor) {
        this.name = name;
        this.primaryColor = primaryColor;
        this.backgroundColor = backgroundColor;
        this.surfaceColor = surfaceColor;
    }

    public String getName() { return name; }
    public int getPrimaryColor() { return primaryColor; }
    public int getBackgroundColor() { return backgroundColor; }
    public int getSurfaceColor() { return surfaceColor; }
}