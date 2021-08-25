package com;

public class Scheme {
    
    private String name;
    private int length;
    private char[] schemeChars;

    public Scheme(String name, int length, char[] schemeChars){
        this.name = name;
        this.length = length;
        this.schemeChars = schemeChars;
    }

    public String getName(){
        return this.name;
    }

    public int getLength(){
        return this.length;
    }

    public char[] getScheme(){
        return this.schemeChars;
    }

}
