package com;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;


public class LimitedTextField extends TextField {
    private IntegerProperty maxLength = new SimpleIntegerProperty(this, "maxLength", -1);
    private StringProperty restrict = new SimpleStringProperty(this, "restrict");

    public LimitedTextField(boolean upperCaseOnly) {
        if(upperCaseOnly){
            this.setTextFormatter(new TextFormatter<>(change -> {
                change.setText(change.getText().toUpperCase());
                return change;
            }));
        }

        textProperty().addListener(new ChangeListener<String>() {
            private boolean ignore;

            @Override
            public void changed(
                    ObservableValue<? extends String> observableValue,
                    String s, String s1) {

                if (ignore || s1 == null){
                    return;
                }
                if (maxLength.get() > -1 && s1.length() > maxLength.get()) {
                    ignore = true;
                    setText(s1.substring(0, maxLength.get()));
                    ignore = false;
                }
                if (restrict.get() != null && !restrict.get().equals("")
                        && !s1.matches(restrict.get() + "*")) {
                    ignore = true;
                    setText(s);
                    ignore = false;
                }
            }
        });

        
    }

    /**
     * Sets the max length of the text field.
     *
     * @param maxLength
     *            The max length.
     */
    public void setMaxLength(int maxLength) {
        this.maxLength.set(maxLength);
    }

    /**
     * Sets a regular expression character class which restricts the user input.
     *
     * E.g. [0-9] only allows numeric values.
     *
     * @param restrict
     *            The regular expression.
     */
    public void setRestrict(String restrict) {
        this.restrict.set(restrict);
    }
}
