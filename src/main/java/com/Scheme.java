package com;

public class Scheme {
    
    private String name;
    private int length;
    private String[] schemeChars;

    public Scheme(String name, int length, String[] schemeChars){
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

    public String[] getScheme(){
        return this.schemeChars;
    }

    public void setScheme(String[] schemeChars){
        this.schemeChars = schemeChars;
    }

    @Override
    public String toString(){
        return this.name;
    }

}
