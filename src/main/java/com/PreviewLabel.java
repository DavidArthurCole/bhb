package com;

import javafx.scene.control.Label;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

//Class used to make nickname preview labels
class PreviewLabel extends Label {
    public PreviewLabel(char c, String color, int fullLength){
        super(Character.toString(c));
        if(BlendGUI.isHexOk(color)) this.setTextFill(Color.rgb(Integer.parseInt(color.substring(0,2), 16), Integer.parseInt(color.substring(2,4), 16), Integer.parseInt(color.substring(4,6), 16)));
        else this.setTextFill(getColorFromCharacter(color));
        
        double fontsize = Math.floor(565.0 / fullLength);
        if(fontsize > 30) fontsize = 30.0;
        this.setFont(new Font("Arial", fontsize));
    }

    //Lookup table to return a hex Color [obj] from a char
    private static Color getColorFromCharacter(String c){

        String hex;
        
        switch(c){
            case "0": hex = "000000"; break; //Black
            case "1": hex = "0000AA"; break; //Dark blue
            case "2": hex = "00AA00"; break; //Dark green
            case "3": hex = "00AAAA"; break; //Dark aqua
            case "4": hex = "AA0000"; break; //Dark red
            case "5": hex = "AA00AA"; break; //Dark purple
            case "6": hex = "FFAA00"; break; //Gold
            case "7": hex = "AAAAAA"; break; //Gray
            case "8": hex = "555555"; break; //Dark gray
            case "9": hex = "5555FF"; break; //Blue
            
            case "a": hex = "55FF55"; break; //Green
            case "b": hex = "55FFFF"; break; //Aqua
            case "c": hex = "FF5555"; break; //Red
            case "d": hex = "FF55FF"; break; //Light purple
            case "e": hex = "FFFF55"; break; //Yellow
            case "f": hex = "FFFFFF"; break; //White

            default: hex = "FFFFFF"; // i stg if vscode doesn't stop screaming at me about unconstructed strings i'm gonna lose my damn mind
        }

        return Color.rgb(Integer.parseInt(hex.substring(0,2), 16), Integer.parseInt(hex.substring(2,4), 16), Integer.parseInt(hex.substring(4,6), 16));
    }
}
