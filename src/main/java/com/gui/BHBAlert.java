package com.gui;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;

public class BHBAlert extends Alert {
    public BHBAlert(AlertType alertType, String text, String title, String headerText) {
        super(alertType, text);
        this.setTitle(title);
        this.setHeaderText(headerText);
    }

    public BHBAlert(AlertType alertType, String text, String title, String headerText, ButtonType typeOk, ButtonType typeCancel) {
        super(alertType, text, typeOk, typeCancel);
        this.setTitle(title);
        this.setHeaderText(headerText);
    }
}
