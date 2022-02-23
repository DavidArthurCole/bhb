package com.util.gui_helper;

public class Setting{

    private String name;
    private String description;
    private String value;
    private String[] options;

    public Setting(String name, String description, String value, String[] options){
        this.name = name;
        this.description = description;
        this.value = value;
        this.options = options;
    }

    public String getName(){
        return name;
    }

    public String getDescription(){
        return description;
    }

    public String getValue(){
        return value;
    }

    public String[] getOptions(){
        return options;
    }

    public void setValue(String value){
        this.value = value;
    }

    public void execute(){
        //Do nothing by default. Override in subclasses.
    }
}
