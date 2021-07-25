package com;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
//Clipboard management
import java.awt.datatransfer.StringSelection;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Random;

//Logging
import java.util.logging.*;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;

public class BlendGUI extends Application {

    //Global
    private LimitedTextField[] codeFields = new LimitedTextField[6];
    private LimitedTextField enterNickName = new LimitedTextField();
    private HBox previewLabels = new HBox();

    private class SlotMachineColors extends AnimationTimer {

        private long steps;
        private long progress;
        private LimitedTextField[] codeField;

        public SlotMachineColors(){
            this.codeField = codeFields;
            this.progress = 0;
        }

        @Override
        public void handle(long now) {
            if(this.progress % 2 != 0){
                doHandle();
            }
            else{
                progress++;
            }
        }

        private void doHandle() {
            if(this.steps < 15){
                codeField[0].setText(generateRandomHex());
            }
            else if(this.steps >= 15 && this.steps < 30){
                codeField[1].setText(generateRandomHex());
            }
            else if(this.steps >= 30 && this.steps < 45){
                codeField[2].setText(generateRandomHex());
            }
            else if(this.steps >= 45 && this.steps < 60){
                codeField[3].setText(generateRandomHex());
            }
            else if(this.steps >= 60 && this.steps < 75){
                codeField[4].setText(generateRandomHex());
            }
            else if(this.steps >= 75 && this.steps < 90){
                codeField[5].setText(generateRandomHex());
            }
            else{
                this.stop();
            }

            this.progress++;
            this.steps++;
        }
    }

    String currentTheme = "LIGHT";

    Logger log = Logger.getLogger(BlendGUI.class.getSimpleName());
    String defaultFont = "Arial";

    Label[] previewColorLabels = new Label[6];

    protected String currentNick;
    protected LimitedTextField lastField;

    protected static double defaultPreviewHeight;

    public boolean tryLoad(Stage stage){

        String[] codes = new String[6];

        FileChooser configChooser = new FileChooser();
        configChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("BHB Configs (.txt)", "*.txt"));
        configChooser.setTitle("Open configuration file");
        File configFile = configChooser.showOpenDialog(stage);
        if(configFile != null){
            try( FileReader fReader = new FileReader(configFile); //Basic reader, throws FileNotFoundException
            BufferedReader bReader = new BufferedReader(fReader);)
            { 
                //Read the codes
                for(int i = 0; i < 6; i++) {
                    String line = bReader.readLine();
                    if(line != null){
                        if(isHexOk(line)){
                            codes[i] = line;
                        }
                        else return false;
                    }    
                }
                for(int i = 0; i < 6; i++) {
                    if(codes[i] != null) codeFields[i].setText(codes[i]);    
                }
                return true;
            }
            catch(IOException | StringIndexOutOfBoundsException e)
            {
                log.log(Level.SEVERE, e.getMessage());
            }
        }
        return false;
    }

    public void trySave(Stage stage){

        int goodCodes = 0;

        for(int i = 0; i < 6; i++) if(isHexOk(codeFields[i].getText())) goodCodes++;

        if(goodCodes >= 1){
            FileChooser configChooser = new FileChooser();
            configChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("BHB Configs (.txt)", "*.txt"));
            configChooser.setTitle("Save configuration file");
            File saveFile = configChooser.showSaveDialog(stage);

            if(saveFile != null){

                String[] codes = new String[6];
                for(int i = 0; i < 6; i++){
                    if(isHexOk(codeFields[i].getText())) codes[i] = codeFields[i].getText();
                }

                try( FileWriter fWriter = new FileWriter(saveFile); //Basic reader, throws FileNotFoundException
                BufferedWriter bWriter = new BufferedWriter(fWriter);)
                { 
                    for(int i = 0; i < 6; i++){
                        bWriter.write(codes[i]);
                        if(i != 5){
                            bWriter.newLine();
                        }
                    }
                }
                catch(IOException | StringIndexOutOfBoundsException e)
                {

                    log.log(Level.SEVERE, e.getMessage());

                    Alert errorAlert = new Alert(AlertType.ERROR);
                    errorAlert.setHeaderText("Error saving");
                    errorAlert.setContentText("There was an error saving your configuration, please try again.");
                    errorAlert.showAndWait();
                }
            }
        }
        else{
            Alert errorAlert = new Alert(AlertType.ERROR);
            errorAlert.setHeaderText("Nothing to save");
            errorAlert.setContentText("You do not have any codes to save. No file was created.");
            errorAlert.showAndWait();
        }  
       
    }

    @Override
    public void start(Stage stage) throws Exception {

        MenuBar menuBar = new MenuBar();
        // Menu - File
        Menu menuFile = new Menu("File");

        MenuItem saveItem = new MenuItem("Save");
        saveItem.setOnAction( e -> trySave(stage));

        MenuItem loadItem = new MenuItem("Load");
        loadItem.setOnAction( e -> {
            boolean success = tryLoad(stage);
            if(!success){
                Alert errorAlert = new Alert(AlertType.ERROR);
                errorAlert.setHeaderText("Invalid configuration file");
                errorAlert.setContentText("The file you selected is not recognized as a valid configuration file. If you believe this is an error, please reach out.");
                errorAlert.showAndWait();
            }
        });

        menuFile.getItems().addAll(saveItem, loadItem);

        // Menu - Edit
        Menu menuTools = new Menu("Tools");

        MenuItem copyItem = new MenuItem("Copy To Clipboard");
        copyItem.setOnAction( e -> {
            if(currentNick != null && !currentNick.equals("")){
                StringSelection stringSelection = new StringSelection(currentNick);
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            clipboard.setContents(stringSelection, null);
            }
        });

        MenuItem slotMachineColors = new MenuItem("Slot Machine (Seizure Warning)");
        slotMachineColors.setOnAction(e -> new SlotMachineColors().start());

        MenuItem programTheme = new MenuItem("Dark Mode");

        menuTools.getItems().addAll(copyItem, programTheme, slotMachineColors);

        menuBar.getMenus().addAll(menuFile, menuTools);

        VBox mainBox = new VBox();

        
        previewLabels.setAlignment(Pos.CENTER);

        BorderPane previewCopyPane = new BorderPane();
        previewCopyPane.setMinHeight(50);
        previewCopyPane.setCenter(previewLabels);
        previewCopyPane.setBorder(new Border(new BorderStroke(Color.BLACK, BorderStrokeStyle.SOLID, CornerRadii.EMPTY, BorderStroke.THIN)));
        previewCopyPane.setPadding(new Insets(5));

        enterNickName.setRestrict("[A-Za-z0-9_]");
        enterNickName.setPrefWidth(275);
        enterNickName.textProperty().addListener((observable, oldValue, newValue) -> updatePreview());
        
        //Holds the color code selector and the hex color picker
        HBox codesAndPicker = new HBox();

        Button clearAllCodes = new Button("Clear All");

        VBox codes = new VBox(makeCodeBox(1), makeCodeBox(2), makeCodeBox(3), makeCodeBox(4), makeCodeBox(5), makeCodeBox(6), clearAllCodes);

        unlockFields();
                               
        codes.setBorder(new Border(new BorderStroke(Color.BLACK, BorderStrokeStyle.SOLID, CornerRadii.EMPTY, BorderStroke.THIN)));
        codes.setPadding(new Insets(5));
        codes.setAlignment(Pos.CENTER);
        codes.setSpacing(3);

        //COLORPICKER CODE WOOOOOOO
        VBox colorPicker = new VBox();
        colorPicker.setAlignment(Pos.CENTER);
        colorPicker.setMinHeight(160);
        colorPicker.setMinWidth(160);
        colorPicker.setPrefWidth(220);
        colorPicker.setBorder(new Border(new BorderStroke(Color.BLACK, BorderStrokeStyle.SOLID, CornerRadii.EMPTY, BorderStroke.THIN)));

        Circle colorCircle = new Circle();
        colorCircle.setRadius(75);

        ColorPicker colorPickerUI = new ColorPicker(Color.BLACK);
        colorPickerUI.setOnAction(e -> colorCircle.setFill(colorPickerUI.getValue()));

        clearAllCodes.setOnAction(e -> {
            for(LimitedTextField f : codeFields) f.clear();
            previewLabels.getChildren().clear();
            enterNickName.clear();
            colorPickerUI.setValue(Color.BLACK);
            colorCircle.setFill(Color.BLACK);
            codeFields[0].requestFocus();
        });

        colorCircle.setFill(colorPickerUI.getValue());

        HBox pickerAndCopyButton = new HBox();
        pickerAndCopyButton.setPadding(new Insets(5));
        pickerAndCopyButton.setAlignment(Pos.CENTER);
        pickerAndCopyButton.setSpacing(10);

        Button copyToFirstEmpty = new Button("<<<");
        copyToFirstEmpty.setFont(Font.font(defaultFont, FontWeight.SEMI_BOLD, 14));
        copyToFirstEmpty.setOnAction( e -> {

            int findMe = 0;

            for(int i = 0; i < 6; i++){
                if (codeFields[i].equals(lastField)){
                    findMe = i;
                    break;
                }
            }

            if(lastField != null && !lastField.isDisabled()){
                lastField.setText(colorPickerUI.getValue().toString().substring(2,8).toUpperCase());
                if(findMe + 1 < 6){
                    lastField = codeFields[findMe + 1];
                }
            }
            else{
                for(int i = 0; i < 6; i++){
                    if(!isHexOk(codeFields[i].getText())){
                        codeFields[i].setText(colorPickerUI.getValue().toString().substring(2,8).toUpperCase());
                        break;
                    }
                }
            } 
            
            lastField.requestFocus();

        });

        pickerAndCopyButton.getChildren().addAll(copyToFirstEmpty, colorPickerUI);

        colorPicker.getChildren().addAll(colorCircle, pickerAndCopyButton);
        colorPicker.setSpacing(10);

        codesAndPicker.getChildren().addAll(codes, colorPicker);
        codesAndPicker.setBorder(new Border(new BorderStroke(Color.BLACK, BorderStrokeStyle.SOLID, CornerRadii.EMPTY, BorderStroke.THIN)));

        HBox nickInput = new HBox(new Label("Enter nickname: "), enterNickName);
        nickInput.setAlignment(Pos.CENTER);
        nickInput.setPadding(new Insets(5));
        nickInput.setBorder(new Border(new BorderStroke(Color.BLACK, BorderStrokeStyle.SOLID, CornerRadii.EMPTY, BorderStroke.THIN)));

        mainBox.getChildren().addAll(codesAndPicker, nickInput, previewCopyPane);

        Scene mainScene = new Scene(mainBox);
        ((VBox)mainScene.getRoot()).getChildren().addAll(menuBar);

        programTheme.setOnAction(e-> changeTheme(programTheme, mainScene, previewColorLabels));

        menuBar.toBack();
        mainBox.toFront();

        stage.setScene(mainScene);
        stage.setTitle("Blazin's Hex Blender");

        stage.show();
        stage.setResizable(false);
    }

    public Label makePreviewLabel(){ 
        Label newLabel = new Label("     ");
        newLabel.setBorder(new Border(new BorderStroke(Color.BLACK, BorderStrokeStyle.SOLID, CornerRadii.EMPTY, BorderStroke.THIN)));
        newLabel.setPadding(new Insets(3));
        return newLabel;
    }

    public HBox makeCodeBox(int id){
        Label codeId = new Label("Code " + id + ": ");
        Label codeColorPreview = makePreviewLabel();
        previewColorLabels[id - 1] = codeColorPreview;
        LimitedTextField codeField = makeTextField(codeColorPreview, id);
        codeField.setOnInputMethodTextChanged(e -> codeField.setText(codeField.getText().toUpperCase()));
        codeField.setRestrict("[a-fA-F0-9]");

        codeField.setTextFormatter(new TextFormatter<>(change -> {
            change.setText(change.getText().toUpperCase());
            return change;
        }));

        codeId.setOnMouseClicked(e -> codeField.setText(generateRandomHex()));

        codeColorPreview.setOnMouseClicked(e -> codeField.clear());

        HBox newBox = new HBox(codeId, codeField, codeColorPreview);
        newBox.setAlignment(Pos.CENTER);
        newBox.setSpacing(6);

        return newBox;
    }

    public LimitedTextField makeTextField(Label previewLabel, int id){
        LimitedTextField newField = new LimitedTextField();      

        newField.setPrefWidth(75);
        newField.setFont(new Font(defaultFont, 14));

        newField.setMaxLength(6);

        newField.pressedProperty().addListener(listener -> lastField = newField);

        newField.focusedProperty().addListener(listener -> lastField = newField);

        newField.textProperty().addListener((observable, oldValue, newValue) -> {

            if(isHexOk(newValue)){
                previewLabel.setBackground(new Background(new BackgroundFill(Color.rgb(
                    Integer.parseInt(newValue.substring(0,2),16),
                    Integer.parseInt(newValue.substring(2,4),16),
                    Integer.parseInt(newValue.substring(4,6),16)), CornerRadii.EMPTY, Insets.EMPTY)));
            }
            else{
                if(currentTheme.equals("DARK")) previewLabel.setBackground(new Background(new BackgroundFill(Color.rgb(92, 100, 108), CornerRadii.EMPTY, Insets.EMPTY)));
                else{
                    previewLabel.setBackground(new Background(new BackgroundFill(Color.rgb(
                    Integer.parseInt("F2",16),
                    Integer.parseInt("F2",16),
                    Integer.parseInt("F2",16)), CornerRadii.EMPTY, Insets.EMPTY)));
                }
                
            }
            unlockFields();
            updatePreview();
            updateTextFieldFontSize(newField);
        });

        codeFields[id - 1] = newField;

        return newField;
    }

    public void unlockFields(){

        for(int i = 0; i < 6; i++){

            if(codeFields[i].getText().length() == 6 && !codeFields[i].isDisable() && i + 1 < 6 && isHexOk(codeFields[i].getText())){
                codeFields[i + 1].setDisable(false);
            }
            else if (i + 1 < 6) codeFields[i + 1].setDisable(true);
        }

    }

    public static void updateTextFieldFontSize(LimitedTextField field){
        if(field.getText().length() > 0){
            int fontSize = 85 / field.getText().length();
            if (fontSize > 12) fontSize = 14;
            field.setFont(new Font("Arial", fontSize));
        }    
    }

    public void updatePreview(){

        int userInputLength = enterNickName.getText().length();

        //Need to be at least 2 codes
        int validCodes = 0;
        for(int i = 0; i <= 5; i++) if(!codeFields[i].isDisabled() && isHexOk(codeFields[i].getText())) validCodes++;
        if (userInputLength >=3 && validCodes >= 2 && (userInputLength >= ((validCodes * 2) - 1))){
            String[] codeArray = new String[validCodes];
            for(int i = 0; i < validCodes; i++) codeArray[i] = codeFields[i].getText();

            previewLabels.setPrefHeight(defaultPreviewHeight);
            currentNick = Blend.blendMain(validCodes, enterNickName.getText(), codeArray);
            parseNickToLabel(currentNick, previewLabels);
            return;        
        }
        previewLabels.getChildren().clear();
        previewLabels.setPrefHeight(0);
        currentNick = "";
    }

    public static void parseNickToLabel(String nick, HBox previewLabels){

        previewLabels.getChildren().clear();
        String[] comp = nick.split("&#");

        for(int i = 1; i < comp.length; i++){
            previewLabels.getChildren().add(makePreviewLabel(comp[i].charAt(6), comp[i].substring(0,6),  (comp.length - 1)));
        }

    }

    public static Label makePreviewLabel(char c, String hex, int fullLength){
        Label previewLabel = new Label(Character.toString(c));
        previewLabel.setTextFill(Color.rgb(Integer.parseInt(hex.substring(0,2), 16), Integer.parseInt(hex.substring(2,4), 16), Integer.parseInt(hex.substring(4,6), 16)));

        double fontSize = Math.floor(650.0 / fullLength);
        if(fontSize > 30) fontSize = 30;

        previewLabel.setFont(new Font("Arial", fontSize));
        return previewLabel;
    }

    public String generateRandomHex(){

        Random random = new Random();
        StringBuilder rndHex = new StringBuilder();

        for (int i = 0; i < 6; i++) rndHex.append("ABCDEF0123456789".charAt(random.nextInt(16)));

        return rndHex.toString();
    }

    public void changeTheme(MenuItem programTheme, Scene mainScene, Label[] labelColorPreviews){
        if(currentTheme.equals("LIGHT")){
            goDark(mainScene, programTheme, labelColorPreviews);
            currentTheme = "DARK";
            return;
        }

        goLight(mainScene, programTheme, labelColorPreviews);
        currentTheme = "LIGHT";
    }

    public void goDark(Scene mainScene, MenuItem programTheme, Label[] labelColorPreviews){
        mainScene.getStylesheets().add(getClass().getResource("dark.css").toString());
        programTheme.setText("Light Mode");
        for(int i = 0; i <= 5; i++){
            if(codeFields[i].getText().equals("")){
                labelColorPreviews[i].setBackground(new Background(new BackgroundFill(Color.rgb(92, 100, 108), CornerRadii.EMPTY, Insets.EMPTY)));
            }
        }
    }

    public void goLight(Scene mainScene, MenuItem programTheme, Label[] labelColorPreviews){
        mainScene.getStylesheets().remove(getClass().getResource("dark.css").toString());
        programTheme.setText("Dark Mode");
        for(int i = 0; i <=5; i++){
            if(codeFields[i].getText().equals("")){
                labelColorPreviews[i].setBackground(new Background(new BackgroundFill(Color.rgb(
                    Integer.parseInt("F2",16),
                    Integer.parseInt("F2",16),
                    Integer.parseInt("F2",16)), CornerRadii.EMPTY, Insets.EMPTY)));
            }
        }
    }

    public static boolean isHexOk(String hex){
        if(hex.length() != 6) return false;
        //Starts a counter for how many valid chars there are
        int okCount = 0;
        //For each character in hex     //For each valid hex char               //If it's a valid hex character, increment
        for(int i = 0; i <= 5; ++i) for(int j = 0; j <= 15; ++j) if(hex.toUpperCase().charAt(i) == ("0123456789ABCDEF".charAt(j))) ++okCount;
        //If it's 6, good, else, bad
        return okCount == 6;
    }

    public static void main(String[] args) {
        launch();
    }
    
}