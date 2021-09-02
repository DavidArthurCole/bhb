package com;

import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.CornerRadii;
import javafx.scene.paint.Color;

class CodeColorLabel extends Label {
    public CodeColorLabel(){
        //I'm too lazy to set the size to be pixels, so this is how I get a square label LOL
        super("      ");
        this.setBorder(new Border(new BorderStroke(Color.BLACK, BorderStrokeStyle.SOLID, CornerRadii.EMPTY, BorderStroke.THIN)));
        this.setPadding(new Insets(3));
    }
}
