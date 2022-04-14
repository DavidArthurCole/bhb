package com.util.gui_helper;

import java.util.ArrayList;
import java.util.List;

public class Scheme {
    
    private String name;
    private List<String> schemeChars;

    public Scheme(String name, List<String> schemeChars){
        this.name = name;
        this.schemeChars = new ArrayList<>(schemeChars);
    }

    public List<String> getScheme(){
        return this.schemeChars;
    }

    public void setScheme(List<String> schemeChars){
        this.schemeChars = new ArrayList<>(schemeChars);
    }

    @Override
    public String toString(){
        return this.name;
    }

}
