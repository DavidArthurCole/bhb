package com.gui;

import java.util.ArrayList;

public class ColorMap{
    
    private final ArrayList<String> keys;
    private final ArrayList<String> vals;

    public ColorMap() {
        this.keys = new ArrayList<>();
        this.vals = new ArrayList<>();
    }

    public void entry(String key, String val) {
        this.keys.add(key);
        this.vals.add(val);
    }

    public String get(String key){
        int index = this.keys.indexOf(key);
        if (index == -1) return null;
        return this.vals.get(index);
    }
}
