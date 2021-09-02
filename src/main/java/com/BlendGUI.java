package com;

//======================================================
//|                 IMPORTS                            |
//======================================================

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.Tooltip;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
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
import java.awt.datatransfer.StringSelection;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Optional;
import java.util.Random;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.commons.io.IOUtils;
import org.json.JSONObject;
import java.util.logging.*;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;

public class BlendGUI extends Application {

    //======================================================
    //|                 GLOBAL VARS                        |
    //======================================================

    //Global current version indicator
    private static final String VERSION = "1.4.0";
    //Default theme is light
    private String currentTheme = "LIGHT";
    //Stores the current nick for BHB
    protected String currentNickBHB;
    //Stores the current nick for Colorscheme
    protected String currentNickColorScheme;
    //Stores the default preferred height for previewLabelsBHB
    protected static double defaultPreviewHeight;
    //Creates a logger for debug
    private Logger log = Logger.getLogger(BlendGUI.class.getSimpleName());
    //Prevents threading errors in some cases
    private boolean alreadySaved = false;
    //Passed around for use in different UI elements in colorscheme
    private Scheme selectedScheme;
    //Store values when sending colors around
    private Deque<String> oldColorQueue = new ArrayDeque<>();
    private Deque<Integer> referencedFieldsQueue = new ArrayDeque<>();
    private boolean lastActionClearAll = false;
    //Definition for ctrl z shortcut
    private KeyCodeCombination ctrlZ = new KeyCodeCombination(KeyCode.Z, KeyCombination.CONTROL_ANY);

    //For global access, changing disabling/editing between scenes
    private BorderPane rootPane = new BorderPane();
    private MenuItem switchStagesItem = new MenuItem("Switch to Colorscheme");
    private MenuBar menuBar = new MenuBar();
    private MenuItem saveItem = new MenuItem("Save");
    private MenuItem undoItem = new MenuItem("Undo");
    private MenuItem loadItem = new MenuItem("Load");
    private Scene mainScene;

    //Colorscheme 
    private BorderPane mainColorschemeBox = new BorderPane();
    private Button copyButtonColorscheme = new Button();
    private HBox previewLabelsColorscheme = new HBox();
    private ComboBox<Scheme> schemes = new ComboBox<>();
    private LimitedTextField enterNicknameColorscheme = new LimitedTextField();
    private Scheme[] loadedSchemes = new Scheme[9];

    //BHB
    private Button[] upButtons = new Button[6];
    private Button copyButtonBHB = new Button();
    private Button clearAllCodes = new Button("Clear All");
    private Button copyToFirstEmpty = new Button("<<<");

    private LimitedTextField lastEnteredField;
    private LimitedTextField[] codeFields = new LimitedTextField[6];
    private LimitedTextField enterNicknameBHB = new LimitedTextField();

    private HBox previewLabelsBHB = new HBox();
    private HBox codesAndPicker = new HBox();
    private HBox pickerAndCopyButton = new HBox();
    private HBox nickInput = new HBox(new Label("Enter nickname: "), enterNicknameBHB);

    private Label[] previewColorLabels = new Label[6];
    
    private MenuItem slotMachineColorsItem = new MenuItem("Slot Machine (Seizure Warning)");
    private MenuItem programThemeItem = new MenuItem("Dark Mode");
    private MenuItem updateCheckerItem = new MenuItem("Check For Updates");
    private MenuItem aboutItem = new MenuItem("About");
    private MenuItem gitHubItem = new MenuItem("Visit the GitHub page");

    private Menu menuEdit = new Menu("Edit");
    private Menu menuTools = new Menu("Tools");
    private Menu menuHelp = new Menu("Help");
    private Menu menuFile = new Menu("File");
    
    private VBox mainBoxBHB = new VBox();    
    private VBox codesBox = new VBox(makeCodeBox(1), makeCodeBox(2), makeCodeBox(3), makeCodeBox(4), makeCodeBox(5), makeCodeBox(6), clearAllCodes);
    private VBox colorPicker = new VBox();

    private Circle colorCircle = new Circle();
    private ColorPicker colorPickerUI = new ColorPicker(Color.BLACK);
    private BorderPane previewCopyPane = new BorderPane();
    
    
    
    
    
    //======================================================
    //|                 MAIN METHODS                       |
    //======================================================

    //Main runtime
    public static void main(String[] args) {

        if(args.length >=1 && args[0] != null) killOldLinuxProcess(args);
        launch();
    }

    //Start the program
    @Override
    public void start(Stage stage) throws Exception {

        //Create the pane scene for BHB
        buildBHB(stage);

        //Create the pane scene for Colorscheme
        buildColorscheme();

        rootPane.setTop(menuBar);
        rootPane.setCenter(mainBoxBHB);
        mainScene = new Scene(rootPane);
        stage.setScene(mainScene);
        stage.setTitle("Blazin's Hex Blender");
        stage.show();
        stage.setResizable(false);

        forceLoad(mainScene, programThemeItem, previewColorLabels);
        updateUndoButton();
    }

    //======================================================
    //|                 BHB BUILDING                       |
    //======================================================


    private void buildBHB(Stage stage){

        //Make sure codes get saved in the event of shutdown
        Runtime.getRuntime().addShutdownHook(new Thread(this::forceSave));

        //Delete old update files that exist
        File oldUpdater = new File(System.getProperty("user.dir") + "/Update.sh");
        oldUpdater.delete();

        buildMenuItemsBHB(stage);
        buildMenusBHB();
        buildButtonsBHB();
        buildMiscBHB();
        buildBoxesBHB();
    }

    private void buildMenuItemsBHB(Stage stage){

        saveItem.setOnAction( e -> trySave(stage));

        loadItem.setOnAction( e -> {
            boolean success = tryLoad(stage);
            if(!success){
                Alert errorAlert = new Alert(AlertType.ERROR, "The file you selected is not recognized as a valid configuration file. If you believe this is an error, please reach out.");
                errorAlert.setHeaderText("Invalid configuration file");
                errorAlert.showAndWait();
            }
        });

        undoItem.setOnAction(e -> undoChange());

        switchStagesItem.setOnAction(e -> switchStages(stage));

        slotMachineColorsItem.setOnAction(e -> new SlotMachineColors(codeFields).start());  

        aboutItem.setOnAction(e -> {
            String publishDate = getTagFromGitJson("published_at");

            String releaseDate;
            if(compareVersions(VERSION, getTagFromGitJson("tag_name")) == -1) releaseDate = "Release date unknown";
            else releaseDate = "Release date: " + publishDate.substring(0, publishDate.length() - 1).replace("T", " ");

            Alert aboutAlert = new Alert(AlertType.INFORMATION, releaseDate);
            aboutAlert.setTitle("About BHB");
            aboutAlert.setHeaderText("Version: " + VERSION);
            aboutAlert.showAndWait();
        });

        gitHubItem.setOnAction(f -> getHostServices().showDocument("https://github.com/DavidArthurCole/bhb"));

        updateCheckerItem.setOnAction(e -> {
            
            if(compareVersions(VERSION, getTagFromGitJson("tag_name")) == -1) startSelfUpdate();
            else{
                Alert updateAlert = new Alert(AlertType.INFORMATION, "BHB is up to date, (version " + VERSION + ")");
                updateAlert.setHeaderText("Up to date");
                updateAlert.setTitle("No updates found");
                updateAlert.showAndWait();
            }

        });

        programThemeItem.setOnAction(e-> changeTheme(programThemeItem, mainScene, previewColorLabels));
    }

    private void buildMenusBHB(){
        menuFile.getItems().addAll(saveItem, loadItem);
        menuEdit.getItems().addAll(undoItem);
        menuHelp.getItems().addAll(aboutItem, gitHubItem, updateCheckerItem);
        menuTools.getItems().addAll(programThemeItem, slotMachineColorsItem, switchStagesItem);

        menuBar.getMenus().addAll(menuFile, menuEdit, menuTools, menuHelp);
        menuBar.setBorder(new Border(new BorderStroke(Color.BLACK, BorderStrokeStyle.SOLID, CornerRadii.EMPTY, BorderStroke.THIN)));
    }

    private void buildButtonsBHB(){
        copyButtonBHB = new Button("", new CopyButtonIcon(true));
        copyButtonBHB.setTooltip(new Tooltip("Copy nickname to clipboard"));
        copyButtonBHB.setOnAction(e -> doClipboardCopy());

        copyToFirstEmpty.setTooltip(new Tooltip("Move colorpicker color to codes"));
        copyToFirstEmpty.setFont(Font.font("Arial", FontWeight.SEMI_BOLD, 14));
        copyToFirstEmpty.setOnAction( e -> {

            int findMe = 0;
            for(int i = 0; i < 6; ++i, ++findMe) if (codeFields[i].equals(lastEnteredField)) break;

            if(lastEnteredField != null && !lastEnteredField.isDisabled()){

                oldColorQueue.offerFirst(codeFields[findMe].getText());
                referencedFieldsQueue.offerFirst(findMe);
                updateUndoButton();

                lastEnteredField.setText(colorPickerUI.getValue().toString().substring(2,8).toUpperCase());
                if(findMe + 1 < 6) lastEnteredField = codeFields[findMe + 1];
            }
            else{
                for(int i = 0; i < 6; i++){
                    if(!isHexOk(codeFields[i].getText())){

                        oldColorQueue.offerFirst(codeFields[i].getText());
                        referencedFieldsQueue.offerFirst(i);
                        updateUndoButton();

                        codeFields[i].setText(colorPickerUI.getValue().toString().substring(2,8).toUpperCase());
                        break;
                    }
                }
            } 
            
            lastEnteredField.requestFocus();

        });

        clearAllCodes.setOnAction(e -> {
            for(int i = 0; i < 6; ++i) {
                referencedFieldsQueue.offerFirst(i);
                oldColorQueue.offer(codeFields[i].getText());
                lastActionClearAll = true;

                codeFields[i].clear();
            }
            updateUndoButton();
            codeFields[0].requestFocus();
        });
        clearAllCodes.setTooltip(new Tooltip("Clear code fields"));

        pickerAndCopyButton.setPadding(new Insets(5));
        pickerAndCopyButton.setAlignment(Pos.CENTER);
        pickerAndCopyButton.setSpacing(10);
        pickerAndCopyButton.getChildren().addAll(copyToFirstEmpty, colorPickerUI);
    }

    private void buildBoxesBHB(){
        previewLabelsBHB.setAlignment(Pos.CENTER);
    
        previewCopyPane.setMinHeight(50);
        previewCopyPane.setLeft(copyButtonBHB);
        previewCopyPane.setCenter(previewLabelsBHB);
        previewCopyPane.setBorder(new Border(new BorderStroke(Color.BLACK, BorderStrokeStyle.SOLID, CornerRadii.EMPTY, BorderStroke.THIN)));
        previewCopyPane.setPadding(new Insets(5));

        codesBox.setBorder(new Border(new BorderStroke(Color.BLACK, BorderStrokeStyle.SOLID, CornerRadii.EMPTY, BorderStroke.THIN)));
        codesBox.setPadding(new Insets(5));
        codesBox.setAlignment(Pos.CENTER);
        codesBox.setSpacing(3);

        //Unlock fields based on filled codes
        unlockFields();

        colorPicker.setAlignment(Pos.CENTER);
        colorPicker.setMinHeight(160);
        colorPicker.setMinWidth(160);
        colorPicker.setPrefWidth(220);
        colorPicker.setBorder(new Border(new BorderStroke(Color.BLACK, BorderStrokeStyle.SOLID, CornerRadii.EMPTY, BorderStroke.THIN)));

        codesAndPicker.getChildren().addAll(codesBox, colorPicker);
        codesAndPicker.setBorder(new Border(new BorderStroke(Color.BLACK, BorderStrokeStyle.SOLID, CornerRadii.EMPTY, BorderStroke.THIN)));
        
        nickInput.setAlignment(Pos.CENTER);
        nickInput.setPadding(new Insets(5));
        nickInput.setBorder(new Border(new BorderStroke(Color.BLACK, BorderStrokeStyle.SOLID, CornerRadii.EMPTY, BorderStroke.THIN)));

        mainBoxBHB.getChildren().addAll(codesAndPicker, nickInput, previewCopyPane);

        menuBar.toBack();
        mainBoxBHB.toFront();
    }

    private void buildMiscBHB(){
        enterNicknameBHB.setRestrict("[A-Za-z0-9_]");
        enterNicknameBHB.setPrefWidth(275);
        enterNicknameBHB.textProperty().addListener((observable, oldValue, newValue) -> updatePreview());
        
        colorCircle.setRadius(75);
        colorCircle.setFill(colorPickerUI.getValue());

        colorPickerUI.setOnAction(e -> colorCircle.setFill(colorPickerUI.getValue()));
        colorPicker.getChildren().addAll(colorCircle, pickerAndCopyButton);
        colorPicker.setSpacing(10);
    }

    //======================================================
    //|                 FACTORIES                          |
    //======================================================

    //Factory for code boxes in BHB
    private LimitedTextField makeCodeEnterField(Label codeColorLabel, int id){

        LimitedTextField newField = new LimitedTextField();      
        newField.setPrefWidth(75);
        newField.setFont(new Font("Arial", 14));

        newField.setMaxLength(6);
        newField.pressedProperty().addListener(listener -> lastEnteredField = newField);
        newField.focusedProperty().addListener(listener -> lastEnteredField = newField);
        newField.textProperty().addListener((observable, oldValue, newValue) -> {

            if(isHexOk(newValue)){
                codeColorLabel.setBackground(new Background(new BackgroundFill(Color.rgb(
                    Integer.parseInt(newValue.substring(0,2),16),
                    Integer.parseInt(newValue.substring(2,4),16),
                    Integer.parseInt(newValue.substring(4,6),16)), CornerRadii.EMPTY, Insets.EMPTY)));
            }
            else{
                if(currentTheme.equals("DARK")) codeColorLabel.setBackground(new Background(new BackgroundFill(Color.rgb(92, 100, 108), CornerRadii.EMPTY, Insets.EMPTY)));
                else{
                    codeColorLabel.setBackground(new Background(new BackgroundFill(Color.rgb(
                    Integer.parseInt("F2",16),
                    Integer.parseInt("F2",16),
                    Integer.parseInt("F2",16)), CornerRadii.EMPTY, Insets.EMPTY)));
                }
                
            }
            unlockFields();
            updatePreview();
            updateTextFieldFontSize(newField);
        });

        newField.setOnKeyPressed(e -> {
            if(ctrlZ.match(e)){
                undoChange();
            }
        });

        newField.setRestrict("[a-fA-F0-9]");

        newField.setTextFormatter(new TextFormatter<>(change -> {
            change.setText(change.getText().toUpperCase());
            return change;
        }));

        codeFields[id - 1] = newField;

        return newField;
    }

    //Factory for code box containers in BHB
    private HBox makeCodeBox(int id){
        Label codeId = new Label("Code " + id + ": ");
        Label codeColorLabel = new CodeColorLabel();
        previewColorLabels[id - 1] = codeColorLabel;
        previewColorLabels[id - 1].setTooltip(new Tooltip("Clear code"));
        LimitedTextField codeField = makeCodeEnterField(codeColorLabel, id);

        codeColorLabel.setOnMouseClicked(e -> codeField.clear());

        Button upButton = new Button("↑");
        upButton.setTooltip(new Tooltip("Move code up"));
        upButton.setFont(new Font("Arial", 14));
        upButton.setStyle("-fx-font-weight: bold");
        upButtons[(id - 1)] = upButton;
        upButton.setOnAction(e -> {
            if(id - 1 > 0 && isHexOk(codeField.getText())){
                String currentText = codeField.getText();
                String newText = codeFields[id - 2].getText();

                codeField.setText(newText);
                codeFields[id - 2].setText(currentText);
            }
        });

        HBox newBox = new HBox(codeId, codeField, upButton, codeColorLabel);
        newBox.setAlignment(Pos.CENTER);
        newBox.setSpacing(6);
        newBox.setMinWidth(200);
        newBox.setMinHeight(30);

        return newBox;
    }

    //Builds components for colorscheme
    private void buildColorscheme(){
        
        copyButtonColorscheme = new Button("", new CopyButtonIcon(true));
        copyButtonColorscheme.setTooltip(new Tooltip("Copy nickname to clipboard"));
        copyButtonColorscheme.setOnAction(e -> doClipboardCopy());

        HBox chooseScheme = new HBox();

        Label prompt = new Label("Choose a scheme: ");
        schemes.setMinWidth(140);

        schemes.valueProperty().addListener((observable, oldValue, newValue) -> {
            if(enterNicknameColorscheme.getText().length() > 0){
                generateNewRandomScheme();
                generateNewRandomHexScheme();
                updatePreviewColorscheme();
            }  
        });

        chooseScheme.getChildren().addAll(prompt, schemes);
        chooseScheme.setBorder(new Border(new BorderStroke(Color.BLACK, BorderStrokeStyle.SOLID, CornerRadii.EMPTY, BorderStroke.THIN)));
        chooseScheme.setPadding(new Insets(5));
        chooseScheme.setAlignment(Pos.CENTER);
        chooseScheme.setSpacing(3);

        HBox enternick = new HBox();
        Label prompt1 = new Label("Enter a nickname: ");

        enterNicknameColorscheme.setRestrict("[A-Za-z0-9_]");
        enterNicknameColorscheme.setPrefWidth(275);
        enterNicknameColorscheme.textProperty().addListener((observable, oldValue, newValue) -> {
            if(schemes.getValue() != null){
                updatePreviewColorscheme();
            }
        });
        enternick.setBorder(new Border(new BorderStroke(Color.BLACK, BorderStrokeStyle.SOLID, CornerRadii.EMPTY, BorderStroke.THIN)));
        enternick.setPadding(new Insets(5));
        enternick.setAlignment(Pos.CENTER);
        enternick.setSpacing(3);

        initSchemes();
        reloadSchemes();

       
        Label previewLabel = new Label("Nickname preview:");
    
        previewLabelsColorscheme.setPrefHeight(defaultPreviewHeight);
        previewLabelsColorscheme.setMinHeight(defaultPreviewHeight);
        previewLabelsColorscheme.setAlignment(Pos.CENTER);

        BorderPane nickPreview = new BorderPane();
        nickPreview.setBorder(new Border(new BorderStroke(Color.BLACK, BorderStrokeStyle.SOLID, CornerRadii.EMPTY, BorderStroke.THIN)));
        nickPreview.setTop(previewLabel);
        nickPreview.setCenter(previewLabelsColorscheme);
        nickPreview.setBottom(copyButtonColorscheme);
        nickPreview.setMinHeight(defaultPreviewHeight + new Label("L").getHeight() + 10.0);
        nickPreview.setPadding(new Insets(5));
        BorderPane.setAlignment(previewLabel, Pos.CENTER);
        BorderPane.setAlignment(copyButtonColorscheme, Pos.CENTER);

        enternick.getChildren().addAll(prompt1, enterNicknameColorscheme);

        //Add everything for the "second scene" to the VBox
        mainColorschemeBox.setTop(enternick);
        mainColorschemeBox.setCenter(nickPreview);
        mainColorschemeBox.setBottom(chooseScheme);
    }

    //======================================================
    //|                 UI CHANGES                         |
    //======================================================

    //Unlock or lock the undo button if there are items in the queue
    private void updateUndoButton(){
        undoItem.setDisable(referencedFieldsQueue.isEmpty());
    }

    //Switch between BHB and Colorscheme
    private void switchStages(Stage stage){
        if(rootPane.getCenter().equals(mainBoxBHB)){
            rootPane.setCenter(mainColorschemeBox);
            stage.setTitle("Colorscheme V2");
            //Disable un-used buttons in scene
            saveItem.setDisable(true);
            loadItem.setDisable(true);
            slotMachineColorsItem.setDisable(true);
            switchStagesItem.setText("Switch to BHB");
        }
        else{

            rootPane.setCenter(mainBoxBHB);
            stage.setTitle("Blazin's Hex Blender");
            //Re-enable used buttons in scene
            saveItem.setDisable(false);
            loadItem.setDisable(false);
            slotMachineColorsItem.setDisable(false);
            switchStagesItem.setText("Switch to Colorscheme V2");
        }
    }

    //Unlocks or locks field inside of BHB dependent on if text is entered or not
    private void unlockFields(){
        for(int i = 0; i < 6; i++){
            if(codeFields[i].getText() != null && codeFields[i].getText().length() == 6 && !codeFields[i].isDisable() && i + 1 < 6 && isHexOk(codeFields[i].getText())) codeFields[i + 1].setDisable(false);
            else if (i + 1 < 6) codeFields[i + 1].setDisable(true);
        }
    }

    //Update font size based on how much text there is
    private static void updateTextFieldFontSize(LimitedTextField field){
        if(field.getText() != null && field.getText().length() > 0){
            int fontSize = 85 / field.getText().length();
            if (fontSize > 12) fontSize = 14;
            field.setFont(new Font("Arial", fontSize));
        }    
    }

    //Update the preview with new text or new codes
    public void updatePreview(){

        int userInputLength = enterNicknameBHB.getText().length();

        //Need to be at least 2 codes
        int validCodes = 0;
        for(int i = 0; i <= 5; i++) if(!codeFields[i].isDisabled() && isHexOk(codeFields[i].getText())) validCodes++;
        if (userInputLength >=3 && validCodes >= 2 && (userInputLength >= ((validCodes * 2) - 1))){
            String[] codeArray = new String[validCodes];
            for(int i = 0; i < validCodes; i++) codeArray[i] = codeFields[i].getText();

            previewLabelsBHB.setPrefHeight(defaultPreviewHeight);
            currentNickBHB = Blend.blendMain(validCodes, enterNicknameBHB.getText(), codeArray);
            parseNickToLabel(currentNickBHB, previewLabelsBHB, selectedScheme, true);
            return;        
        }
        previewLabelsBHB.getChildren().clear();
        previewLabelsBHB.setPrefHeight(0);
        currentNickBHB = "";
    }

    //Colorscheme version of ^
    public void updatePreviewColorscheme(){

        currentNickColorScheme = "";

        StringBuilder nickBuilder = new StringBuilder();

        int userInputLength = enterNicknameColorscheme.getText().length();

        //Gets the currently selected scheme from the dropdown menu
        selectedScheme = schemes.getValue();

        //Gets the codes that correspond to the scheme
        String[] schemeCodes = selectedScheme.getScheme();

        int counter = 0;
        //Fills the codearray with the codes
        for(int i = 0; i < userInputLength; ++i, ++counter){
            //Next char to be added
            if(counter >= schemeCodes.length) counter = 0;
            if(selectedScheme.toString().equals("Random Hex")){
                nickBuilder.append("&#" + schemeCodes[counter] + enterNicknameColorscheme.getText().charAt(i));
            }
            else{
                nickBuilder.append("&" + schemeCodes[counter] + enterNicknameColorscheme.getText().charAt(i));
            }         
        }
        currentNickColorScheme = nickBuilder.toString();

        parseNickToLabel(currentNickColorScheme, previewLabelsColorscheme, selectedScheme, false);

        previewLabelsColorscheme.setPrefHeight(defaultPreviewHeight);
    }

    //Bool swapping between light & dark
    private void changeTheme(MenuItem programTheme, Scene mainScene, Label[] labelColorPreviews){
        if(currentTheme.equals("LIGHT")){
            goDark(mainScene, programTheme, labelColorPreviews);
            currentTheme = "DARK";
            return;
        }

        goLight(mainScene, programTheme, labelColorPreviews);
        currentTheme = "LIGHT";
    }

    //Dark mode
    private void goDark(Scene mainScene, MenuItem programTheme, Label[] labelColorPreviews){
        for(Button b : upButtons) b.setTextFill(Color.WHITE);
        mainScene.getStylesheets().add(getClass().getClassLoader().getResource("dark.css").toString());
        programTheme.setText("Light Mode");
        for(int i = 0; i <= 5; ++i){
            if(codeFields[i].getText().equals("")){
                labelColorPreviews[i].setBackground(new Background(new BackgroundFill(Color.rgb(92, 100, 108), CornerRadii.EMPTY, Insets.EMPTY)));
            }
        }
        
        copyButtonBHB.setGraphic(new CopyButtonIcon(false));
        copyButtonColorscheme.setGraphic(new CopyButtonIcon(false));
    }

    //Light mode
    private void goLight(Scene mainScene, MenuItem programTheme, Label[] labelColorPreviews){
        for(Button b : upButtons) b.setTextFill(Color.BLACK);
        mainScene.getStylesheets().remove(getClass().getClassLoader().getResource("dark.css").toString());
        programTheme.setText("Dark Mode");
        for(int i = 0; i <=5; i++){
            if(codeFields[i].getText().equals("")){
                labelColorPreviews[i].setBackground(new Background(new BackgroundFill(Color.rgb(
                    Integer.parseInt("F2",16),
                    Integer.parseInt("F2",16),
                    Integer.parseInt("F2",16)), CornerRadii.EMPTY, Insets.EMPTY)));
            }
        }

        copyButtonBHB.setGraphic(new CopyButtonIcon(true));
        copyButtonColorscheme.setGraphic(new CopyButtonIcon(true));
    }

    //======================================================
    //|                 UI INTEGRATED HELPERS              |
    //======================================================

    private void doClipboardCopy(){
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        StringSelection stringSelection = new StringSelection("");
        if(rootPane.getCenter().equals(mainBoxBHB) && currentNickBHB != null && !currentNickBHB.equals("")) stringSelection = new StringSelection(currentNickBHB);
        else if (currentNickColorScheme != null && !currentNickColorScheme.equals("")) stringSelection = new StringSelection(currentNickColorScheme);
        clipboard.setContents(stringSelection, null);
    }

    //Undo changes in the order they were made
    private void undoChange(){
        //If the queue is empty, gtfo
        if(referencedFieldsQueue.isEmpty()) return;

        //If the last action was a clear all, undo all 6 clears, else, undo 1
        for(int i = 0; i < (lastActionClearAll ? 6 : 1); i++){
            Integer codeFieldsID = referencedFieldsQueue.removeFirst();
            codeFields[codeFieldsID].setText(oldColorQueue.removeFirst());
            codeFields[codeFieldsID].requestFocus();
        }
        lastActionClearAll = false;
        updateUndoButton();
    }

    //Create preview label coundaries based on a formatted nick
    private static void parseNickToLabel(String nick, HBox previewLabels, Scheme selectedScheme, boolean isBhb){
        previewLabels.getChildren().clear();
        String[] comp = nick.split("&#");
        if(isBhb || selectedScheme.toString().equals("Random Hex")) for(int i = 1; i < comp.length; i++) previewLabels.getChildren().add(new PreviewLabel(comp[i].charAt(6), comp[i].substring(0,6), (comp.length - 1)));
        else for(int i = 1; i < nick.split("&").length; i++) previewLabels.getChildren().add(new PreviewLabel(nick.split("&")[i].charAt(1), Character.toString(nick.split("&")[i].charAt(0)), (nick.split("&").length - 1)));
    }

    //Allows for new generation mid program
    private void generateNewRandomScheme(){

        //Create random colors
        Random random = new Random(new Random().nextInt(Integer.MAX_VALUE));
        String[] randomArray = new String[32];
        for(int i = 0; i < 32; ++i) randomArray[i] = Character.toString("0123456789abcdef".charAt(random.nextInt(15)));

        //Create the scheme
        if(loadedSchemes[7] == null) loadedSchemes[7] = new Scheme("Random", randomArray);
        else loadedSchemes[7].setScheme(randomArray);
    }

    //Allows for new generation mid program
    private void generateNewRandomHexScheme(){
        
        String[] randomHexArray = new String[32];
        for(int i = 0; i < 32; ++i) randomHexArray[i] = new RandomHexGenerator().generate();

        //Create the scheme
        if(loadedSchemes[8] == null) loadedSchemes[8] = new Scheme("Random Hex", randomHexArray);
        else loadedSchemes[8].setScheme(randomHexArray);
    }

    //======================================================
    //|                 UPDATERS                           |
    //======================================================

    private void startSelfUpdate(){

        //Literally just gets the latest version number from the git repo
        String latest = getTagFromGitJson("tag_name");

        //Create buttons for alert
        ButtonType no = new ButtonType("No", ButtonBar.ButtonData.OK_DONE);
        ButtonType yes = new ButtonType("Yes", ButtonBar.ButtonData.CANCEL_CLOSE);

        Alert updateAlert = new Alert(AlertType.CONFIRMATION, "An updated version of BHB is available. Current: " 
            + VERSION + ", New: " + latest + ". Update now? This will restart your program.", no, yes);

        updateAlert.setHeaderText("Out of date");
        updateAlert.setTitle("Updates found");
        Optional<ButtonType> result = updateAlert.showAndWait();

        if(result.orElse(no) == yes){
            String osName = System.getProperty("os.name");
            if(osName.length() >= 7 &&  osName.substring(0, 7).equals("Windows")) updateSelfWindows(latest);
            else if(osName.substring(0,5).equals("Linux")) updateSelfLinux(latest);
            else{
                Alert unsupported = new Alert(AlertType.ERROR, "Upgrading not supported on this OS yet.");
                unsupported.setHeaderText(osName);
                unsupported.showAndWait();
            }       
        }
    }


    private void updateSelfWindows(String latest){
        Thread saveThread = new Thread(this::forceSave);
        saveThread.start();
        try { saveThread.join();}
        catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
        alreadySaved = true;

        //If the exe exists, replace it
        if(new File(System.getProperty("user.dir") + "/BHB.exe").exists()){
            executeCommandWindows("cmd.exe /c cd " +  System.getProperty("user.dir") + " & taskkill /F /PID " + getPID() 
                + " & curl -L -O " + "https://github.com/DavidArthurCole/bhb/releases/download/" + latest + "/BHB.exe & BHB.exe");
        }
        
        //If the jar exists, replace it
        if(new File(System.getProperty("user.dir") + "/BHB.jar").exists()){
            executeCommandWindows("cmd.exe /c cd " +  System.getProperty("user.dir") + " & taskkill /F /PID " + getPID() 
                + " & curl -L -O " + "https://github.com/DavidArthurCole/bhb/releases/download/" + latest + "/BHB.jar & java -jar BHB.jar");
        }
    }

    private void updateSelfLinux(String latest){
        executeCommandLinux("#!/bin/bash\n\ncd " + System.getProperty("user.dir") + "\nwget https://github.com/DavidArthurCole/bhb/releases/download/" + latest + "/BHB.jar -O BHB.jar && java -jar BHB.jar " + Long.toString(getPID()));
        new Thread(this::forceSave).start();
        alreadySaved = true;
        System.exit(0);
    }

    //======================================================
    //|                 MISCELLANEOUS HELPERS              |
    //======================================================

    private static void killOldLinuxProcess(String[] args){
        ProcessBuilder processBuilder = new ProcessBuilder().command("nohup", "kill", "-9", args[0]);
        try {
            processBuilder.directory(new File(System.getProperty("user.dir")));
            processBuilder.redirectErrorStream(false);
            processBuilder.start();
    
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //Run a command asynchronously, outside of the JVM on Windows
    private void executeCommandWindows(String command) {
        try {
            Runtime.getRuntime().exec(command).waitFor();
        } catch (IOException e) {
            log.log(Level.SEVERE, e.getMessage());
        } catch (InterruptedException e){
            Thread.currentThread().interrupt();
        }
    }

    //Roundabout way to execute a command separate from the JVM on linux
    private void executeCommandLinux(String command) {

        //Write the command to the shell script
        try(FileWriter writer = new FileWriter(new File(System.getProperty("user.dir") + "/Update.sh"))){
            writer.write(command);
        }
        catch(IOException ex){
            //Catch any IO errors - should not ever be a problem
            Alert errorAlert = new Alert(AlertType.ERROR, "Error updating, please report this.");
            errorAlert.setHeaderText(ex.getLocalizedMessage());
            errorAlert.showAndWait();
            return;
        }

        // nohup detaches the script from the JVM process itself
        ProcessBuilder processBuilder = new ProcessBuilder().command("nohup", "sh", "Update.sh");

        try {
            //Set the process to start in the dir that the .jar is located
            processBuilder.directory(new File(System.getProperty("user.dir")));
            //Don't shoot errors - log them instead
            processBuilder.redirectErrorStream(false);
            //Create and start the process
            Process updateProcess = processBuilder.start();
            //Waitfor prevents pre-emptive termination
            updateProcess.waitFor();
    
        } catch (IOException e) {
            e.printStackTrace();
        }
        catch(InterruptedException ex){
            Thread.currentThread().interrupt();
        }
    }

    //Returns the working PID of the JVM on all OS-es
    private static long getPID() {
        return Long.parseLong(java.lang.management.ManagementFactory.getRuntimeMXBean().getName().split("@")[0]);
    }

    //Non-intrusive loading, will not force
    private boolean tryLoad(Stage stage){

        //Sto codes from file
        String[] codes = new String[6];

        //Create a new dialog
        FileChooser configChooser = new FileChooser();
        //Configure to only allow TXT files ~ set the title
        configChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("BHB Configs (.txt)", "*.txt"));
        configChooser.setTitle("Open configuration file");
        //Open the dialog
        File configFile = configChooser.showOpenDialog(stage);
        if(configFile != null){
            for(int i = 0; i < 6; i++) codeFields[i].setText("");
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
                        //Error in file reading - invalid chars reached
                        else return false;
                    }    
                }
                //Even if codes are null, should allow a pass here
                for(int i = 0; i < 6; i++) {
                    if(codes[i] != null) codeFields[i].setText(codes[i]);    
                }
                return true;
            }
            //Catch various IO errors from reading the files
            catch(IOException | StringIndexOutOfBoundsException e)
            {
                log.log(Level.SEVERE, e.getMessage());
            }
        }
        return false;
    }

    //Intrusive loading technique, will disrupt the program's functionality temporarily
    private void forceLoad(Scene mainScene, MenuItem programTheme, Label[] labelColorPreviews){

        //Create a temporary reference to the file
        File tempStore = new File(System.getProperty("user.dir") + "/tempstore.txt");
        if(!tempStore.exists()) return;

        //Create a store for codes
        String[] codes = new String[6];

        //try to make the file readable ~ Will only work on Windows
        try{
            Runtime.getRuntime().exec("attrib -H \"" +  System.getProperty("user.dir") + "/tempstore.txt\"" );
        }
        catch(IOException ex){
            log.log(Level.INFO, "Not a Windows machine.", ex);
        }

        //Create a new instances of the reader
        try( FileReader fReader = new FileReader(tempStore); //Basic reader, throws FileNotFoundException
        BufferedReader bReader = new BufferedReader(fReader);)
        { 
            //Read the codes
            for(int i = 0; i < 6; i++) {
                String line;
                if((line = bReader.readLine()) != null && isHexOk(line)){
                    codes[i] = line;
                }    
            }
            for(int i = 0; i < 6; i++) {
                if(codes[i] != null) codeFields[i].setText(codes[i]);    
            }
            enterNicknameBHB.setText(bReader.readLine().replace("\n", ""));

            //Set theme based on config
            String savedTheme = bReader.readLine();
            if(savedTheme.equals("DARK")) changeTheme(programTheme, mainScene, labelColorPreviews);   
        }
        catch(IOException | StringIndexOutOfBoundsException e)
        {
            log.log(Level.SEVERE, e.getMessage());
        }

        //Delete the file once loaded
        String deleted = Boolean.toString(tempStore.delete());
        log.log(Level.ALL, deleted);
    }

    //Non-intrusive saving, will not force
    private void trySave(Stage stage){

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

                    Alert errorAlert = new Alert(AlertType.ERROR, "There was an error saving your configuration, please try again.");
                    errorAlert.setHeaderText("Error saving");
                    errorAlert.showAndWait();
                }
            }
        }
        else{
            Alert errorAlert = new Alert(AlertType.ERROR, "You do not have any codes to save. No file was created.");
            errorAlert.setHeaderText("Nothing to save");
            errorAlert.showAndWait();
        }  
       
    }

    //Intrusive saving technique, will disrupt the program's functionality temporarily
    private void forceSave(){

        //Flag used during updates so that the JVM doesn't terminate and error out
        if(!alreadySaved){
            File tempStore = new File(System.getProperty("user.dir") + "/tempstore.txt");
            String[] codes = new String[6];
            for(int i = 0; i < 6; i++) codes[i] = codeFields[i].getText();
            try( FileWriter fWriter = new FileWriter(tempStore); //Basic reader, throws FileNotFoundException
            BufferedWriter bWriter = new BufferedWriter(fWriter);)
            { 
                for(int i = 0; i < 6; i++){
                    bWriter.write(codes[i] + "\n");
                }
                bWriter.write(enterNicknameBHB.getText() + "\n");
                bWriter.write(currentTheme);
                Runtime.getRuntime().exec("attrib +H \"" +  System.getProperty("user.dir") + "/tempstore.txt\"" );
            }
            catch(IOException | StringIndexOutOfBoundsException e)
            {
                log.log(Level.SEVERE, e.getMessage());

                Alert errorAlert = new Alert(AlertType.ERROR, "There was an error saving your configuration, please try again.");
                errorAlert.setHeaderText("Error saving");
                errorAlert.showAndWait();
            }
        }
    }

    //Parses JSON from a url
    private String getJSON(String url) {

        try{
            JSONObject json = new JSONObject(IOUtils.toString(new URL(url), StandardCharsets.UTF_8));
            return(json.toString());
        }
        catch(IOException ex){
            Alert errorAlert = new Alert(AlertType.ERROR, "An unexpected error occured.");
            errorAlert.setHeaderText("Error: " + ex.getLocalizedMessage());
            errorAlert.showAndWait();
            return null;
        }
    }

    //Pulls one tag from a json string
    private String getTagFromGitJson(String tagName){
        String json = getJSON("https://api.github.com/repos/DavidArthurCole/bhb/releases/latest");
        JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
        return(obj.get(tagName).getAsString());
    }

    //Compare two version numbers - returns -1 if v1 < v2
    private int compareVersions(String v1, String v2) {
        String[] components1 = v1.split("\\.");
        String[] components2 = v2.split("\\.");
        int length = Math.min(components1.length, components2.length);
        for(int i = 0; i < length; i++) {
            int result = Integer.compare(Integer.parseInt(components1[i]), Integer.parseInt(components2[i]));
            if(result != 0) return result;
        }
        return Integer.compare(components1.length, components2.length);
    }

    //Reload schemes into combobox from the array
    private void reloadSchemes(){
        schemes.getItems().clear();
        for(Scheme s : loadedSchemes) schemes.getItems().add(s);
    }

    //Initialize schemes for startup
    private void initSchemes(){

        loadedSchemes[0] = new Scheme("Rainbow", "c46eab9d5".split("")); //Rainbow
        loadedSchemes[1] = new Scheme("Master", "4cffff".split("")); //Master
        loadedSchemes[2] = new Scheme("Ordered", "0123456789abcdef".split("")); //Ordered
        loadedSchemes[3] = new Scheme("Millionaire", "666eeefff".split("")); //Millionaire
        loadedSchemes[4] = new Scheme("Phoenix", "4c6ef78".split("")); //Phoenix
        loadedSchemes[5] = new Scheme("Dragon", "55da22".split("")); //Dragon
        loadedSchemes[6] = new Scheme("Bacon", "c6666".split("")); //Bacon
        // loadedSchemes[7] IS RESERVED
        // loadedSchemes[8] IS RESERVED
        //DO NOT ADD SCHEMES TO POS 7 OR 8 THIS WILL GO HORRIBLY WRONG 

        //Make the random scheme different every time
        generateNewRandomScheme();

        //Make the random hex scheme different every time
        generateNewRandomHexScheme();
    }

    //Boolean is the hex valid; ie, does it contain any invalid chars, is it 6 chars, etc.
    public static boolean isHexOk(String hex){ 
        return(hex != null && hex.matches("^[a-fA-F0-9]+$") && hex.length() == 6);
    }
    
}