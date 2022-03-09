package com.gui;

import javafx.scene.control.Alert;

public class BHBAlert extends Alert {
    public BHBAlert(AlertType alertType, String text, String title, String headerText) {
        super(alertType, text);
        this.setTitle(title);
        this.setHeaderText(headerText);
    }
}
