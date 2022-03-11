package com.gui;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

public class CopyButtonIcon extends ImageView {
    public CopyButtonIcon(boolean black){   
        this.setImage(new Image(getClass().getClassLoader().getResource("copy_" + (black ? "black" : "white") + ".png").toString()));
        this.setFitHeight(40);
        this.setFitWidth(40);
    }
}
