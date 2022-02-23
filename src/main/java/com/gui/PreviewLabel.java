package com.gui;

import java.util.Map;

import com.BlendGUI;

import javafx.scene.control.Label;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

//Class used to make nickname preview labels
//Most of the code is used to color the text correctly
public class PreviewLabel extends Label {

    private ColorMap codeMap = new ColorMap();

    public PreviewLabel(){
        codeMap.entry("0", "000000"); 
        codeMap.entry("1", "0000AA"); 
        codeMap.entry("2", "00AA00"); 
        codeMap.entry("3", "00AAAA"); 
        codeMap.entry("4", "AA0000"); 
        codeMap.entry("5", "AA00AA"); 
        codeMap.entry("6", "AA5500"); 
        codeMap.entry("7", "AAAAAA"); 
        codeMap.entry("8", "555555"); 
        codeMap.entry("9", "5555FF"); 
        codeMap.entry("A", "55FF55"); 
        codeMap.entry("B", "55FFFF"); 
        codeMap.entry("C", "FF5555"); 
        codeMap.entry("D", "FF55FF"); 
        codeMap.entry("E", "FFFF55"); 
        codeMap.entry("F", "FFFFFF");
    }

    public PreviewLabel(char c, String color, int fullLength){
        super(Character.toString(c));
        if(BlendGUI.isHexOk(color)) this.setTextFill(Color.rgb(Integer.parseInt(color.substring(0,2), 16), Integer.parseInt(color.substring(2,4), 16), Integer.parseInt(color.substring(4,6), 16)));
        else this.setTextFill(
                codeMap.get(color) == null ? Color.rgb(0, 0, 0) :
                    Color.rgb(
                        Integer.parseInt(codeMap.get(color).substring(0,2), 16), 
                        Integer.parseInt(codeMap.get(color).substring(2,4), 16), 
                        Integer.parseInt(codeMap.get(color).substring(4,6), 16)
                    )
                );
        double fontsize = Math.floor(565.0 / fullLength);
        if(fontsize > 30) fontsize = 30.0;
        this.setFont(new Font("Arial", fontsize));
    }
}
