package com.gui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;

public class BHBHBox extends HBox{
    
    private static final Border DEF_BORDER = new Border(new BorderStroke(Color.BLACK, BorderStrokeStyle.SOLID, CornerRadii.EMPTY, BorderStroke.THIN));

    public BHBHBox(Node... children){
        super(children);
        this.setBorder(DEF_BORDER);
        this.setPadding(new Insets(5));
        this.setAlignment(Pos.CENTER);
        this.setSpacing(3);
    }

}
