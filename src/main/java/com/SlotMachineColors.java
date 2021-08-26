package com;

import javafx.animation.AnimationTimer;

//Class extension needed for smooth animation
class SlotMachineColors extends AnimationTimer {

    BlendGUI m = new BlendGUI();
    private double progress;
    private LimitedTextField[] codeFields;

    public SlotMachineColors(LimitedTextField[] codeFields){
        this.codeFields = codeFields;
    }

    //Per the counter, decide if certain bars should stop being cycled
    @Override
    public void handle(long now) {
        if(this.progress % 2 != 0) for(int c = 5; c >= (int)Math.floor(progress / 30); --c) codeFields[c].setText(m.generateRandomHex());  
        else if(progress >= 180) this.stop();
        progress++;
    }
} 