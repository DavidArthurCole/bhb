package com.util.gui_helper;

public class SizeDeterminer {
    
    public enum Axis {
        WIDTH,
        HEIGHT;
    }

    private double originalHeight;
    private double originalWidth;

    private double scaledHeight;
    private double scaledWidth;

    public SizeDeterminer(double originalHeight, double originalWidth, double scaledHeight, double scaledWidth) {
        this.originalHeight = originalHeight;
        this.originalWidth = originalWidth;
        this.scaledHeight = scaledHeight;
        this.scaledWidth = scaledWidth;
    }

    /*
        Given an original value, and an Axis, scale the value to a new ratio
    */
    public double detSize(int originalValue, Axis axis){
        double originalRatio = (originalValue)/((axis == Axis.WIDTH) ? originalWidth : originalHeight);
        return originalRatio / ((axis == Axis.WIDTH) ? scaledWidth : scaledHeight);
    }

    /*
        Set a new scaled height on app size change
    */
    public void setScaledHeight(int scaledHeight) {
        this.scaledHeight = scaledHeight;
    }

    /*
        Set a new scaled width on app size change
    */
    public void setScaledWidth(int scaledWidth) {
        this.scaledWidth = scaledWidth;
    }

}
