package com.gui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ColorMap{
    
    public final List<String> vals;

    public ColorMap(){
        this.vals = new ArrayList<>(Arrays.asList(
            "000000", "0000AA", "00AA00", "00AAAA",
            "AA0000", "AA00AA", "FFAA00", "AAAAAA",
            "555555", "5555FF", "55FF55", "55FFFF",
            "FF5555", "FF55FF", "FFFF55", "FFFFFF"
        ));
    }

    public String get(char c){
        int index = Integer.parseInt(Character.toString(c), 16);
        return (index < 0 || index >= 16 ? "000000" : this.vals.get(index));
    }
}
