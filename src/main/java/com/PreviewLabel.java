package com;

import javafx.scene.control.Label;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

//Class used to make nickname preview labels
class PreviewLabel extends Label {
    public PreviewLabel(char c, String color, int fullLength){
        super(Character.toString(c));
        if(BlendGUI.isHexOk(color)) this.setTextFill(Color.rgb(Integer.parseInt(color.substring(0,2), 16), Integer.parseInt(color.substring(2,4), 16), Integer.parseInt(color.substring(4,6), 16)));
        else this.setTextFill(BlendGUI.getColorFromCharacter(color));
        
        double fontsize = Math.floor(565.0 / fullLength);
        if(fontsize > 30) fontsize = 30.0;
        this.setFont(new Font("Arial", fontsize));
    }
}
