package com.util.gui_helper;

import java.util.List;

import com.gui.LimitedTextField;

import javafx.animation.AnimationTimer;

//Class extension needed for smooth animation
public class SlotMachineColors extends AnimationTimer {

    private double progress;
    private List<LimitedTextField> codeFields;
    private RandomHexGenerator rndHexGenerator;

    public SlotMachineColors(List<LimitedTextField> codeFields){
        this.codeFields = codeFields;
        this.rndHexGenerator = new RandomHexGenerator();
    }

    //Per the counter, decide if certain bars should stop being cycled
    @Override
    public void handle(long now) {
        if(this.progress % 2 != 0) for(int c = 5; c >= (int)Math.floor(progress / 30); --c) codeFields.get(c).setText(rndHexGenerator.generate());  
        else if(progress >= 180) this.stop();
        progress++;
    }
} 