package com;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

public class CopyButtonIcon extends ImageView {
    
    public CopyButtonIcon(boolean black){   

        if(black) this.setImage(new Image(getClass().getClassLoader().getResource("copy_black.png").toString()));
        else this.setImage(new Image(getClass().getClassLoader().getResource("copy_white.png").toString()));

        this.setFitHeight(40);
        this.setFitWidth(40);
    }

}
