package com;

public class Scheme {
    
    private String name;
    private String[] schemeChars;

    public Scheme(String name, String[] schemeChars){
        this.name = name;
        this.schemeChars = schemeChars;
    }

    public String getName(){
        return this.name;
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
