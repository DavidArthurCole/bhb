package com.gui;

import com.BHBMainGUI;

import javafx.scene.control.Label;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

//Class used to make nickname preview labels
//Most of the code is used to color the text correctly
public class PreviewLabel extends Label {

    private ColorMap codeMap = new ColorMap();

    public PreviewLabel(char c, String color, int fullLength){
        super(Character.toString(c));
        Character principleFirstChar = color.toCharArray()[0];
        if(BHBMainGUI.isHexOk(color)) this.setTextFill(Color.rgb(Integer.parseInt(color.substring(0,2), 16), Integer.parseInt(color.substring(2,4), 16), Integer.parseInt(color.substring(4,6), 16)));
        else this.setTextFill(
                this.codeMap.get(principleFirstChar) == null ? Color.rgb(0, 0, 0) :
                    Color.rgb(
                        Integer.parseInt(this.codeMap.get(principleFirstChar).substring(0,2), 16), 
                        Integer.parseInt(this.codeMap.get(principleFirstChar).substring(2,4), 16), 
                        Integer.parseInt(this.codeMap.get(principleFirstChar).substring(4,6), 16)
                    )
                );
        double fontsize = Math.floor(565.0 / fullLength);
        if(fontsize > 30) fontsize = 30.0;
        this.setFont(new Font("Arial", fontsize));
    }
}
