package com;

import java.util.Random;

public class RandomHexGenerator {
    
    public String generate(){
        Random random = new Random(new Random().nextInt(Integer.MAX_VALUE));
        StringBuilder rndHex = new StringBuilder();
        for (int i = 0; i < 6; i++) rndHex.append("0123456789ABCDEF".charAt(random.nextInt(16)));
        return rndHex.toString();
    }

}
