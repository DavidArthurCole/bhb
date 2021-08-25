package com;

import javafx.animation.AnimationTimer;
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
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
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
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.Random;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.apache.commons.io.IOUtils;
import org.json.JSONObject;

//Logging
import java.util.logging.*;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;

public class BlendGUI extends Application {

    //Global current version indicator
    private static final String VERSION = "1.3.0";

    //Prevents threading errors in some cases
    private boolean alreadySaved = false;

    //Creates vars to be passed across functions - global variables
    private LimitedTextField[] codeFields = new LimitedTextField[6];
    private LimitedTextField enterNickName = new LimitedTextField();
    private LimitedTextField enterNickNameColorscheme = new LimitedTextField();

    private HBox previewLabels = new HBox();
    private HBox previewLabelsColorscheme = new HBox();

    //Currently in development, porting ColorScheme to BHB
    private Scene mainScene;
    private ComboBox<Scheme> schemes = new ComboBox<>();

    //Used in both stages
    private MenuBar menuBar = new MenuBar();
    private MenuItem switchStages = new MenuItem("Switch to Colorscheme");
    private BorderPane rootPane = new BorderPane();

    //For global access, changing disabling between scenes
    private MenuItem saveItem = new MenuItem("Save");
    private MenuItem loadItem = new MenuItem("Load");
    private MenuItem slotMachineColors = new MenuItem("Slot Machine (Seizure Warning)");

    private BorderPane mainColorschemeBox = new BorderPane();
    private VBox mainBox = new VBox();

    private Button copyButton = new Button();
    private Button copyButtonColorscheme = new Button();

    private Scheme[] loadedSchemes = new Scheme[9];

    private void executeCommandWindows(String command) {
        try {
            Process process = Runtime.getRuntime().exec(command);
            process.waitFor();
        } catch (IOException e) {
            log.log(Level.SEVERE, e.getMessage());
        } catch (InterruptedException e){
            Thread.currentThread().interrupt();
        }
    }

    private void executeCommandLinux(String command) {

        try(FileWriter writer = new FileWriter(new File(System.getProperty("user.dir") + "/Update.sh"))){
            writer.write(command);
        }
        catch(IOException ex){
            Alert errorAlert = new Alert(AlertType.ERROR, "Error updating, please report this.");
            errorAlert.setHeaderText(ex.getLocalizedMessage());
            errorAlert.showAndWait();
            return;
        }
        
        // -- Linux --
        // Run a shell command

        ProcessBuilder processBuilder = new ProcessBuilder().command("nohup", "sh", "Update.sh");

        try {
            processBuilder.directory(new File(System.getProperty("user.dir")));
            processBuilder.redirectErrorStream(false);

            Process updateProcess = processBuilder.start();
            updateProcess.waitFor();
    
        } catch (IOException e) {
            e.printStackTrace();
        }
        catch(InterruptedException ex){
            Thread.currentThread().interrupt();
        }
    }

    public static long getPID() {
        String processName = java.lang.management.ManagementFactory.getRuntimeMXBean().getName();
        if (processName != null && processName.length() > 0) {
            try {
                return Long.parseLong(processName.split("@")[0]);
            }
            catch (Exception e) {
                return 0;
            }
        }

        return 0;
    }

    private class SlotMachineColors extends AnimationTimer {

        private double progress;
        private LimitedTextField[] codeField;

        public SlotMachineColors(){
            this.codeField = codeFields;
            this.progress = 0;
        }

        @Override
        public void handle(long now) {
            if(this.progress % 2 != 0) for(int c = 5; c >= (int)Math.floor(progress / 30); --c) codeField[c].setText(generateRandomHex());  
            else if(progress >= 180) this.stop();
            progress++;
        }
    }

    String currentTheme = "LIGHT";

    Logger log = Logger.getLogger(BlendGUI.class.getSimpleName());
    String defaultFont = "Arial";

    Label[] previewColorLabels = new Label[6];

    protected String currentNick;
    protected String currentNickColorScheme;
    protected LimitedTextField lastField;

    protected static double defaultPreviewHeight;

    public boolean tryLoad(Stage stage){

        String[] codes = new String[6];

        FileChooser configChooser = new FileChooser();
        configChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("BHB Configs (.txt)", "*.txt"));
        configChooser.setTitle("Open configuration file");
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

    public void forceLoad(Scene mainScene, MenuItem programTheme, Label[] labelColorPreviews){
        File tempStore = new File(System.getProperty("user.dir") + "/tempstore.txt");
        if(!tempStore.exists()) return;

        String[] codes = new String[6];

        try{
            Runtime.getRuntime().exec("attrib -H \"" +  System.getProperty("user.dir") + "/tempstore.txt\"" );
        }
        catch(IOException ex){
            log.log(Level.INFO, "Not a Windows machine.", ex);
        }

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
            enterNickName.setText(bReader.readLine().replace("\n", ""));

            String savedTheme = bReader.readLine();
            if(savedTheme.equals("DARK")) changeTheme(programTheme, mainScene, labelColorPreviews);   
        }
        catch(IOException | StringIndexOutOfBoundsException e)
        {
            log.log(Level.SEVERE, e.getMessage());
        }

        String deleted = Boolean.toString(tempStore.delete());
        log.log(Level.ALL, deleted);
    }

    public void forceSave(){

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
                bWriter.write(enterNickName.getText() + "\n");
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

    public String getJSON(String url) {

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

    public String getTagFromGitJson(String tagName){
        String json = getJSON("https://api.github.com/repos/DavidArthurCole/bhb/releases/latest");
        JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
        return(obj.get(tagName).getAsString());
    }

    public int compareVersions(String v1, String v2) {
        String[] components1 = v1.split("\\.");
        String[] components2 = v2.split("\\.");
        int length = Math.min(components1.length, components2.length);
        for(int i = 0; i < length; i++) {
            int result = Integer.compare(Integer.parseInt(components1[i]), Integer.parseInt(components2[i]));
            if(result != 0) {
                return result;
            }
        }
        return Integer.compare(components1.length, components2.length);
    }

    public void buildSecondaryScene(){
        
        HBox chooseScheme = new HBox();

        Label prompt = new Label("Choose a scheme: ");
        schemes.setMinWidth(140);

        schemes.valueProperty().addListener((observable, oldValue, newValue) -> {
            if(enterNickNameColorscheme.getText().length() > 0){
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

        enterNickNameColorscheme.setRestrict("[A-Za-z0-9_]");
        enterNickNameColorscheme.setPrefWidth(275);
        enterNickNameColorscheme.textProperty().addListener((observable, oldValue, newValue) -> {
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

        enternick.getChildren().addAll(prompt1, enterNickNameColorscheme);

        //Add everything for the "second scene" to the VBox
        mainColorschemeBox.setTop(enternick);
        mainColorschemeBox.setCenter(nickPreview);
        mainColorschemeBox.setBottom(chooseScheme);
    }

    public void switchStages(Stage stage){
        if(rootPane.getCenter().equals(mainBox)){
            rootPane.setCenter(mainColorschemeBox);
            stage.setTitle("Colorscheme V2");
            //Disable un-used buttons in scene
            saveItem.setDisable(true);
            loadItem.setDisable(true);
            slotMachineColors.setDisable(true);
            switchStages.setText("Switch to BHB");
        }
        else{

            rootPane.setCenter(mainBox);
            stage.setTitle("Blazin's Hex Blender");
            //Re-enable used buttons in scene
            saveItem.setDisable(false);
            loadItem.setDisable(false);
            slotMachineColors.setDisable(false);
            switchStages.setText("Switch to Colorscheme V2");
        }
    }

    @Override
    public void start(Stage stage) throws Exception {

        ImageView copyIcon =  new ImageView(new Image(getClass().getResource("copy_black.png").toExternalForm()));
        copyIcon.setFitWidth(40);
        copyIcon.setFitHeight(40);

        copyButton = new Button("", copyIcon);
        copyButton.setTooltip(new Tooltip("Copy nickname to clipboard"));
        copyButtonColorscheme = new Button("", copyIcon);
        copyButtonColorscheme.setTooltip(new Tooltip("Copy nickname to clipboard"));

        File oldUpdater = new File(System.getProperty("user.dir") + "/Update.sh");
        oldUpdater.delete();

        Runtime.getRuntime().addShutdownHook(new Thread(this::forceSave));

        // Menu - File
        Menu menuFile = new Menu("File");

        saveItem.setOnAction( e -> trySave(stage));
        
        loadItem.setOnAction( e -> {
            boolean success = tryLoad(stage);
            if(!success){
                Alert errorAlert = new Alert(AlertType.ERROR, "The file you selected is not recognized as a valid configuration file. If you believe this is an error, please reach out.");
                errorAlert.setHeaderText("Invalid configuration file");
                errorAlert.showAndWait();
            }
        });

        // Menu - Edit
        Menu menuTools = new Menu("Tools");

        switchStages.setOnAction(e -> switchStages(stage));

        
        slotMachineColors.setOnAction(e -> new SlotMachineColors().start());

        MenuItem programTheme = new MenuItem("Dark Mode");
        MenuItem updateChecker = new MenuItem("Check For Updates");

        Menu menuHelp = new Menu("Help");

        MenuItem about = new MenuItem("About");
        about.setOnAction(e -> {
            String publishDate = getTagFromGitJson("published_at");

            String releaseDate;
            if(compareVersions(VERSION, getTagFromGitJson("tag_name")) == -1) releaseDate = "Release date unknown";
            else releaseDate = "Release date: " + publishDate.substring(0, publishDate.length() - 1).replace("T", " ");

            Alert aboutAlert = new Alert(AlertType.INFORMATION, releaseDate);
            aboutAlert.setTitle("About BHB");
            aboutAlert.setHeaderText("Version: " + VERSION);
            aboutAlert.showAndWait();
        });

        updateChecker.setOnAction(e -> {

            //Literally just gets the latest version number from the git repo
            String latest = getTagFromGitJson("tag_name");
            
            if(compareVersions(VERSION, getTagFromGitJson("tag_name")) == -1){

                ButtonType no = new ButtonType("No", ButtonBar.ButtonData.OK_DONE);
                ButtonType yes = new ButtonType("Yes", ButtonBar.ButtonData.CANCEL_CLOSE);

                Alert updateAlert = new Alert(AlertType.CONFIRMATION, "An updated version of BHB is available. Current: " 
                    + VERSION + ", New: " + latest + ". Update now? This will restart your program.", no, yes);

                updateAlert.setHeaderText("Out of date");
                updateAlert.setTitle("Updates found");
                Optional<ButtonType> result = updateAlert.showAndWait();

                if(result.orElse(no) == yes){

                    String osName = System.getProperty("os.name");

                    if(osName.length() >= 7 &&  osName.substring(0, 7).equals("Windows")){

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
                    else if(osName.substring(0,5).equals("Linux")){
                        Alert latestAlertNew = new Alert(AlertType.ERROR, "Latest at execution point: " + latest);
                        latestAlertNew.show();

                        executeCommandLinux("#!/bin/bash\n\ncd " + System.getProperty("user.dir") + "\nwget https://github.com/DavidArthurCole/bhb/releases/download/" + latest + "/BHB.jar -O BHB.jar && java -jar BHB.jar " + Long.toString(getPID()));
                        new Thread(this::forceSave).start();
                        alreadySaved = true;
                        System.exit(0);
                    }
                    else{
                        Alert unsupported = new Alert(AlertType.ERROR, "Upgrading not supported on this OS yet.");
                        unsupported.setHeaderText(osName);
                        unsupported.showAndWait();
                    }
                    
                }

            }
            else{
                Alert updateAlert = new Alert(AlertType.INFORMATION, "BHB is up to date, (version " + VERSION + ")");
                updateAlert.setHeaderText("Up to date");
                updateAlert.setTitle("No updates found");
                updateAlert.showAndWait();
            }

        });

        menuFile.getItems().addAll(saveItem, loadItem);
        menuHelp.getItems().addAll(about, updateChecker);
        menuTools.getItems().addAll(programTheme, slotMachineColors, switchStages);

        menuBar.getMenus().addAll(menuFile, menuTools, menuHelp);
        menuBar.setBorder(new Border(new BorderStroke(Color.BLACK, BorderStrokeStyle.SOLID, CornerRadii.EMPTY, BorderStroke.THIN)));
        
        previewLabels.setAlignment(Pos.CENTER);

        //Copy button functionality
        copyButton.setOnAction(e -> {
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            StringSelection stringSelection = new StringSelection("");
            if(rootPane.getCenter().equals(mainBox) && currentNick != null && !currentNick.equals("")) stringSelection = new StringSelection(currentNick);
            else if (currentNickColorScheme != null && !currentNickColorScheme.equals("")) stringSelection = new StringSelection(currentNickColorScheme);
            clipboard.setContents(stringSelection, null);
            
        });

        copyButtonColorscheme.setOnAction(e -> {
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            StringSelection stringSelection = new StringSelection("");
            if(rootPane.getCenter().equals(mainBox) && currentNick != null && !currentNick.equals("")) stringSelection = new StringSelection(currentNick);
            else if (currentNickColorScheme != null && !currentNickColorScheme.equals("")) stringSelection = new StringSelection(currentNickColorScheme);
            clipboard.setContents(stringSelection, null);
            
        });

        BorderPane previewCopyPane = new BorderPane();
        previewCopyPane.setMinHeight(50);
        previewCopyPane.setLeft(copyButton);
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

        programTheme.setOnAction(e-> changeTheme(programTheme, mainScene, previewColorLabels));

        menuBar.toBack();
        mainBox.toFront();

        rootPane.setTop(menuBar);
        rootPane.setCenter(mainBox);

        mainScene = new Scene(rootPane);

        stage.setScene(mainScene);
        stage.setTitle("Blazin's Hex Blender");

        buildSecondaryScene();

        stage.show();
        stage.setResizable(false);

        forceLoad(mainScene, programTheme, previewColorLabels);
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

    public void updatePreviewColorscheme(){

        currentNickColorScheme = "";

        StringBuilder nickBuilder = new StringBuilder();

        int userInputLength = enterNickNameColorscheme.getText().length();

        //Gets the currently selected scheme from the dropdown menu
        Scheme selectedScheme = schemes.getValue();

        //Gets the codes that correspond to the scheme
        String[] schemeCodes = selectedScheme.getScheme();

        int counter = 0;
        //Fills the codearray with the codes
        for(int i = 0; i < userInputLength; ++i, ++counter){
            //Next char to be added
            if(counter >= schemeCodes.length) counter = 0;
            if(selectedScheme.getName().equals("Random Hex")){
                nickBuilder.append("&#" + schemeCodes[counter] + enterNickNameColorscheme.getText().charAt(i));
            }
            else{
                nickBuilder.append("&" + schemeCodes[counter] + enterNickNameColorscheme.getText().charAt(i));
            }         
        }
        currentNickColorScheme = nickBuilder.toString();

        parseNickToLabelColorscheme(currentNickColorScheme, previewLabelsColorscheme, selectedScheme);

        previewLabelsColorscheme.setPrefHeight(defaultPreviewHeight);
    }

    public static void parseNickToLabel(String nick, HBox previewLabels){

        previewLabels.getChildren().clear();
        String[] comp = nick.split("&#");

        for(int i = 1; i < comp.length; i++){
            previewLabels.getChildren().add(makePreviewLabel(comp[i].charAt(6), comp[i].substring(0,6),  (comp.length - 1)));
        }

    }

    public static void parseNickToLabelColorscheme(String nick, HBox previewLabelsColorscheme, Scheme selectedScheme){

        previewLabelsColorscheme.getChildren().clear();

        if(selectedScheme.getName().equals("Random Hex")){
            String[] comp = nick.split("&#");

            for(int i = 1; i < comp.length; i++){
                previewLabelsColorscheme.getChildren().add(makePreviewLabelColorscheme(comp[i].charAt(6), comp[i].substring(0,6), (comp.length - 1)));
            }
        }
        else{
            String[] comp = nick.split("&");

            for(int i = 1; i < comp.length; i++){
                previewLabelsColorscheme.getChildren().add(makePreviewLabelColorscheme(comp[i].charAt(1), Character.toString(comp[i].charAt(0)), (comp.length - 1)));
            }
        }
    }

    public static Label makePreviewLabel(char c, String hex, int fullLength){
        Label previewLabel = new Label(Character.toString(c));
        previewLabel.setTextFill(Color.rgb(Integer.parseInt(hex.substring(0,2), 16), Integer.parseInt(hex.substring(2,4), 16), Integer.parseInt(hex.substring(4,6), 16)));

        double fontSize = Math.floor(565.0 / fullLength);
        if(fontSize > 30) fontSize = 30;

        previewLabel.setFont(new Font("Arial", fontSize));
        return previewLabel;
    }

    public static Label makePreviewLabelColorscheme(char c, String color, int fullLength){
        Label previewLabelColorscheme = new Label(Character.toString(c));
        if(color.length() > 1){
            previewLabelColorscheme.setTextFill(Color.rgb(Integer.parseInt(color.substring(0,2), 16), Integer.parseInt(color.substring(2,4), 16), Integer.parseInt(color.substring(4,6), 16)));
        }
        else{
            previewLabelColorscheme.setTextFill(getColorFromCharacter(color));
        }
        
        double fontSize = Math.floor(565.0 / fullLength);
        if(fontSize > 30) fontSize = 30;

        previewLabelColorscheme.setFont(new Font("Arial", fontSize));
        return previewLabelColorscheme;
    }

    public static Color getColorFromCharacter(String c){

        String hex;
        
        switch(c){
            case "0": hex = "000000"; break; //Black
            case "1": hex = "0000AA"; break; //Dark blue
            case "2": hex = "00AA00"; break; //Dark green
            case "3": hex = "00AAAA"; break; //Dark aqua
            case "4": hex = "AA0000"; break; //Dark red
            case "5": hex = "AA00AA"; break; //Dark purple
            case "6": hex = "FFAA00"; break; //Gold
            case "7": hex = "AAAAAA"; break; //Gray
            case "8": hex = "555555"; break; //Dark gray
            case "9": hex = "5555FF"; break; //Blue
            
            case "a": hex = "55FF55"; break; //Green
            case "b": hex = "55FFFF"; break; //Aqua
            case "c": hex = "FF5555"; break; //Red
            case "d": hex = "FF55FF"; break; //Light purple
            case "e": hex = "FFFF55"; break; //Yellow
            case "f": hex = "FFFFFF"; break; //White

            default: hex = "FFFFFF"; // i stg if vscode doesn't stop screaming at me about unconstructed strings i'm gonna lose my damn mind
        }

        return Color.rgb(Integer.parseInt(hex.substring(0,2), 16), Integer.parseInt(hex.substring(2,4), 16), Integer.parseInt(hex.substring(4,6), 16));
    }

    public void reloadSchemes(){
        schemes.getItems().clear();
        for(int i = 0; i < loadedSchemes.length; i++){
            schemes.getItems().add(loadedSchemes[i]);
        }
    }

    public void initSchemes(){

        loadedSchemes[0] = new Scheme("Rainbow", 7, "c46eab9d5".split("")); //Rainbow
        loadedSchemes[1] = new Scheme("Master", 6, "4cffff".split("")); //Master
        loadedSchemes[2] = new Scheme("Ordered", 16, "0123456789abcdef".split("")); //Ordered
        loadedSchemes[3] = new Scheme("Millionaire", 11, "666eeefff".split("")); //Millionaire
        loadedSchemes[4] = new Scheme("Phoenix", 7, "4c6ef78".split("")); //Phoenix
        loadedSchemes[5] = new Scheme("Dragon", 6, "55da22".split("")); //Dragon
        loadedSchemes[6] = new Scheme("Bacon", 5, "c6666".split("")); //Bacon
        // loadedSchemes[7] and loadedSchemes[8] are reserved!
        //DO NOT ADD SCHEMES TO POS 7 OR 8 THIS WILL GO HORRIBLY WRONG 

        //Make the random scheme different every time
        generateNewRandomScheme();

        //Make the random hex scheme different every time
        generateNewRandomHexScheme();
    }

    private void generateNewRandomScheme(){

        Random randomSeed = new Random();
        Random random = new Random(randomSeed.nextInt(Integer.MAX_VALUE));

        String[] randomArray = new String[32];
        for(int i = 0; i < 32; ++i){
            randomArray[i] = Character.toString("0123456789abcdef".charAt(random.nextInt(15)));
        }

        //Create the scheme
        if(loadedSchemes[7] == null){
            loadedSchemes[7] = new Scheme("Random", 32, randomArray);
        }
        else{
            loadedSchemes[7].setScheme(randomArray);
        }
        
    }

    private void generateNewRandomHexScheme(){
        
        String[] randomHexArray = new String[32];
        for(int i = 0; i < 32; ++i){
            randomHexArray[i] = generateRandomHex();
        }

        //Create the scheme
        if(loadedSchemes[8] == null){
            loadedSchemes[8] = new Scheme("Random Hex", 32, randomHexArray);
        }
        else{
            loadedSchemes[8].setScheme(randomHexArray);
        }
    }

    public String generateRandomHex(){

        Random randomSeed = new Random();
        Random random = new Random(randomSeed.nextInt(Integer.MAX_VALUE));
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

        ImageView copyIconWhite =  new ImageView(new Image(getClass().getResource("copy_white.png").toExternalForm()));
        copyIconWhite.setFitWidth(40);
        copyIconWhite.setFitHeight(40);
        ImageView copyIconWhite2 =  new ImageView(new Image(getClass().getResource("copy_white.png").toExternalForm()));
        copyIconWhite2.setFitWidth(40);
        copyIconWhite2.setFitHeight(40);
        
        copyButton.setGraphic(copyIconWhite);
        copyButtonColorscheme.setGraphic(copyIconWhite2);
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

        ImageView copyIconBlack =  new ImageView(new Image(getClass().getResource("copy_black.png").toExternalForm()));
        copyIconBlack.setFitWidth(40);
        copyIconBlack.setFitHeight(40);
        ImageView copyIconBlack2 =  new ImageView(new Image(getClass().getResource("copy_black.png").toExternalForm()));
        copyIconBlack2.setFitWidth(40);
        copyIconBlack2.setFitHeight(40);

        copyButton.setGraphic(copyIconBlack);
        copyButtonColorscheme.setGraphic(copyIconBlack2);
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

        if(args.length >=1 && args[0] != null){

            ProcessBuilder processBuilder = new ProcessBuilder().command("nohup", "kill", "-9", args[0]);
            try {
                processBuilder.directory(new File(System.getProperty("user.dir")));
                processBuilder.redirectErrorStream(false);
                processBuilder.start();
        
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        launch();
    }
    
}