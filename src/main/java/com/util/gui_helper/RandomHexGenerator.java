package com.util.gui_helper;

import java.util.Random;

public class RandomHexGenerator {
    
    private Random random;

    public RandomHexGenerator(){
        this.random = new Random(new Random().nextInt(Integer.MAX_VALUE));
    }

    //Generate a random hex string of length 6
    public String generate(){
        //New builder object
        StringBuilder rndHex = new StringBuilder();
        //Get a new random char from the string, 6 times
        for (int i = 0; i < 6; i++) rndHex.append("0123456789ABCDEF".charAt(this.random.nextInt(16)));
        return rndHex.toString();
    }
}
