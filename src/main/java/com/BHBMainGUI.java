package com;

//======================================================
//|                 IMPORTS                            |
//======================================================

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.Window;

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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.Optional;
import java.util.Random;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.gui.BHBAlert;
import com.gui.CodeColorLabel;
import com.gui.CopyButtonIcon;
import com.gui.LimitedTextField;
import com.gui.PreviewLabel;
import com.util.gui_helper.Blend;
import com.util.gui_helper.RandomHexGenerator;
import com.util.gui_helper.Scheme;
import com.util.gui_helper.Setting;
import com.util.gui_helper.SlotMachineColors;
import com.util.updater.Updater;

import org.apache.commons.io.IOUtils;
import org.json.JSONObject;
import java.util.logging.*;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;

public class BHBMainGUI extends Application {

    //======================================================
    //|                 GLOBAL VARS                        |
    //======================================================

    //Global current version indicator
    private static final String CURRENT_VERSION = "1.5.1";
    //Gets the latest version number from the git repo
    private final String LATEST_VERSION = getTagFromGitJson("tag_name");

    //Enable verbose logging
    private static boolean verboseLogging = false;
    //Count logged items
    private static int loggedItems = 0;
    //Init log file
    private File logFile = new File("log.txt");
    
    //Global updater object
    private Updater updater; 
    //Default theme is light
    private String currentTheme = "LIGHT";
    //Stores the current nick for BHB
    private String currentNickBHB;
    //Stores the current nick for Colorscheme
    private String currentNickColorScheme;
    //Stores the default preferred height for previewLabelsBHB
    private static double defaultPreviewHeight;
    //Creates a logger for debug
    private Logger log = Logger.getLogger(BHBMainGUI.class.getSimpleName());
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
    private MenuItem switchStagesItem = new MenuItem("Switch to Colorscheme V2");
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
    private LimitedTextField enterNicknameColorscheme = new LimitedTextField(false, "[A-Za-z0-9_\\[\\] ]", -1);
    private ArrayList<Scheme> loadedSchemes = new ArrayList<>();

    //BHB
    private VBox mainBHBBox = new VBox();   
    private Button[] upButtons = new Button[6];
    private Button copyButtonBHB = new Button();
    private Button clearAllCodes = new Button("Clear All");
    private Button copyToFirstEmpty = new Button("<<<");

    private LimitedTextField lastEnteredField;
    private LimitedTextField[] codeFields = new LimitedTextField[6];
    private LimitedTextField enterNicknameBHB = new LimitedTextField(false, "[A-Za-z0-9_\\[\\]]", -1);

    private HBox previewLabelsBHBBox = new HBox();
    private HBox codesAndPickerBox = new HBox();
    private HBox pickerAndCopyButtonBox = new HBox();
    private HBox nickInputBox = new HBox(new Label("Enter text: "), enterNicknameBHB);

    private Label[] previewColorLabels = new Label[6];
    
    // Each individual menu item for the submenus - any var ending in ..item is a MenuItem, these are all theoretically global,
    // however not all are used in both modes
    private MenuItem slotMachineColorsItem = new MenuItem("Slot Machine (Seizure Warning)");
    private MenuItem settingsItem = new MenuItem("Settings");
    private MenuItem updateCheckerItem = new MenuItem("Check For Updates");
    private MenuItem aboutItem = new MenuItem("About");
    private MenuItem gitHubItem = new MenuItem("Visit the GitHub page");

    //Sub menus displayed in the MenuBar
    private Menu menuEdit = new Menu("Edit");
    private Menu menuTools = new Menu("Tools");
    private Menu menuHelp = new Menu("Help");
    private Menu menuFile = new Menu("File");
     
    private VBox codesBox = new VBox(makeCodeBox(1), makeCodeBox(2), makeCodeBox(3), makeCodeBox(4), makeCodeBox(5), makeCodeBox(6), clearAllCodes);
    private VBox colorPickerBox = new VBox();

    private Circle colorCircle = new Circle();
    private ColorPicker colorPickerUI = new ColorPicker(Color.BLACK);
    private BorderPane previewCopyPane = new BorderPane();

    //Settings
    private BorderPane settingsPane = new BorderPane();
    private Setting justificationPriority = new Setting("Justification Priority", "When a string cannot evenly be split, there will need to be longer strings on one side. This setting changes which side gets longer strings.", 
    "Left", new String[]{"Left", "Right"}){
        @Override
        public void execute(){
            renderOnHook = true;
        }   
    };
    private Setting darkMode = new Setting("Dark Mode", "Enable dark mode for the GUI.", "Off", new String[]{"Off", "On"}){
        @Override
        public void execute(){
            changeTheme(mainScene, previewColorLabels);
        }
    };
    private boolean renderOnHook = false;
    
    //======================================================
    //|                 MAIN METHODS                       |
    //======================================================

    //Main runtime
    public static void main(String[] args) {
        //Catch flag for old linux process
        if(args.length >=1 && args[0] != null) killOldLinuxProcess(args);
        //Catch flag for verbose logging mode
        if(args.length >=2 && args[1] != null && args[1].equalsIgnoreCase("-v")) verboseLogging = true;
        launch();    
    }

    //Start the program
    @Override
    public void start(Stage stage) throws Exception {

        initLogger();

        //Create the pane scene for BHB
        buildBHB(stage);

        //Create the pane scene for Colorscheme
        buildColorscheme();

        rootPane.setTop(menuBar);
        rootPane.setCenter(mainBHBBox);
        mainScene = new Scene(rootPane);
        stage.setScene(mainScene);
        stage.setTitle("Hex Blender");
        stage.show();
        stage.setResizable(false);

        forceLoad(mainScene, previewColorLabels);

        //Build the settings page
        buildSettingsBox();

        updateUndoButton();

        updater = new Updater(LATEST_VERSION);
    }

    //======================================================
    //|                 BHB BUILDING                       |
    //======================================================


    /**
     * Initializes UI components that are needed for the BHB section of the program. 
     * 
     * @param stage the stage object, which is passed by reference to the {@link #buildMenuItemsBHB(Stage) buildMenuItemsBHB()} method.
     *  
     */
    private void buildBHB(Stage stage){

        //Make sure codes get saved in the event of shutdown
        Runtime.getRuntime().addShutdownHook(new Thread(this::saveApplicationState));
        //Log file handler, deleting if empty
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try{
                //Sleep for 250 ms to account for file lock
                Thread.sleep(250);
            }
            catch(InterruptedException ex){
                Thread.currentThread().interrupt();
                logStatic(Level.SEVERE, "Exception in hook \"delete log file\"; Stacktrace: " + ex.getStackTrace(), ex);
            }
            //If no items are logged, delete the file - will not trigger in verbose mode
            if(loggedItems < 1) logFile.delete();
        }));

        //Delete old update files that exist
        File oldUpdater = new File(System.getProperty("user.dir") + "/Update.sh");
        oldUpdater.delete();

        buildMenuItemsBHB(stage);
        buildMenusBHB();
        buildButtonsBHB();
        buildMiscBHB();
        buildBoxesBHB();

        logStatic(Level.INFO, "BHB init completed", null);
    }

    private void buildSettingsBox(){
        settingsPane.getChildren().clear();

        ImageView settingsIcon = new ImageView(new Image(getClass().getClassLoader().getResource("exit.png").toString()));
        settingsIcon.setFitWidth(30);
        settingsIcon.setFitHeight(30);

        Button exitButton = new Button();
        exitButton.setMinWidth(mainScene.getWidth());
        exitButton.setTooltip(new Tooltip("Exit the settings menu"));
        exitButton.setGraphic(settingsIcon);
        exitButton.setOnAction(e -> toggleSettings());

        HBox justificationBox = buildSetting(justificationPriority);
        HBox darkModeBox = buildSetting(darkMode);
        VBox settingsBox = new VBox(justificationBox, darkModeBox);

        settingsPane.setTop(settingsBox);
        settingsPane.setBottom(exitButton);
        BorderPane.setAlignment(exitButton, Pos.CENTER);
    }

    private HBox buildSetting(Setting setting){

        HBox masterBox = new HBox();
        masterBox.setMinHeight(45);
        masterBox.setAlignment(Pos.CENTER);
        masterBox.setBorder(new Border(new BorderStroke(Color.BLACK, BorderStrokeStyle.SOLID, CornerRadii.EMPTY, BorderStroke.THIN)));
        ImageView helpIcon = new ImageView(new Image(getClass().getClassLoader().getResource("help.png").toString()));
        helpIcon.setFitWidth(15);
        helpIcon.setFitHeight(15);
        
        Label helpLabel = new Label();
        helpLabel.setAlignment(Pos.BASELINE_RIGHT);
        helpLabel.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        helpLabel.setGraphic(helpIcon);
        helpLabel.setTooltip(new Tooltip(setting.getDescription()));

        HBox newBox = new HBox();
        newBox.setAlignment(Pos.CENTER);
        newBox.setSpacing(6);
        newBox.setMinWidth(250);
        newBox.setMinHeight(30);

        Label settingName = new Label(setting.getName() + ":");
        RadioButton rb1 = new RadioButton(setting.getOptions()[0]);
        RadioButton rb2 = new RadioButton(setting.getOptions()[1]);  

        rb1.setOnAction(e -> {
            if(rb1.isSelected()) {
                rb2.setSelected(false);
                setting.setValue(rb1.getText());
            }
            setting.execute();
        });

        rb2.setOnAction(e -> {
            if(rb2.isSelected()){
                rb1.setSelected(false);
                setting.setValue(rb2.getText());
            }
            setting.execute();
        });

        if(setting.getValue().equals(setting.getOptions()[0])) rb1.setSelected(true);
        else rb2.setSelected(true);

        newBox.getChildren().addAll(settingName, rb1, rb2);
        masterBox.getChildren().addAll(newBox, helpLabel);
        
        return masterBox;
    }

    /**
     * Called by {@link #buildBHB(Stage) buildBHB}.
     * 
     * Initializes menu items, which will control other various UI elements and user I/O.
     * 
     * @param stage the stage object, which is passed by reference to the {@link #tryLoad(Stage) tryLoad()} method
     * 
     * @see MenuItem
     */
    private void buildMenuItemsBHB(Stage stage){

        saveItem.setOnAction( e -> saveCodes());

        loadItem.setOnAction( e -> {
            if(!tryLoad(stage)){
                new BHBAlert(AlertType.ERROR, 
                "The file you selected is not recognized as a valid configuration file. If you believe this is an error, please reach out.",
                "Error", "Invalid configuration file").showAndWait();
            }
        });

        undoItem.setOnAction(e -> undoChange());

        switchStagesItem.setOnAction(e -> switchStages(stage));

        settingsItem.setOnAction(e -> toggleSettings());

        slotMachineColorsItem.setOnAction(e -> new SlotMachineColors(codeFields).start());  

        aboutItem.setOnAction(e -> {
            String publishDate = getTagFromGitJson("published_at");
            String releaseDate = compareVersions(CURRENT_VERSION, LATEST_VERSION) == -1 ? 
                "Release date unknown" : "Release date: " + publishDate.substring(0, publishDate.length() - 1).replace("T", " ").split(" ")[0];

            new BHBAlert(AlertType.INFORMATION, releaseDate, "About BHB", "Version: " + CURRENT_VERSION).showAndWait();
        });

        gitHubItem.setOnAction(f -> getHostServices().showDocument("https://github.com/DavidArthurCole/bhb"));

        updateCheckerItem.setOnAction(e -> {
            
            if(compareVersions(CURRENT_VERSION, LATEST_VERSION) == -1) startSelfUpdate();
            else{
                new BHBAlert(AlertType.INFORMATION, "BHB is up to date, (version " + CURRENT_VERSION + ")",
                "No updates found", "Up to date").showAndWait();
            }
        });
    }

    /**
     * Called by {@link #buildBHB(Stage) buildBHB}
     * Initializes the Menus themselves, which will contain the MenuItems generated in {@link #buildMenuItemsBHB(Stage) buildMenuItemsBHB()}
     * 
     * @see Menu
     */
    private void buildMenusBHB(){
        menuFile.getItems().addAll(saveItem, loadItem);
        menuEdit.getItems().addAll(undoItem);
        menuHelp.getItems().addAll(aboutItem, gitHubItem, updateCheckerItem);
        menuTools.getItems().addAll(slotMachineColorsItem, switchStagesItem, settingsItem);

        menuBar.getMenus().addAll(menuFile, menuEdit, menuTools, menuHelp);
        menuBar.setBorder(new Border(new BorderStroke(Color.BLACK, BorderStrokeStyle.SOLID, CornerRadii.EMPTY, BorderStroke.THIN)));

        logStatic(Level.INFO, "BHB Menus init completed", null);
    }

    /**
     * Called by {@link #buildBHB(Stage) buildBHB} 
     * Initializes the various Button objects that are spread throughout the BHB layout. 
     * Note that this method does not generate buttons for factory-created boxes, and only creates '1-off' buttons
     * 
     * @see Button
     */
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
                oldColorQueue.offerFirst(codeFields[i].getText());
                lastActionClearAll = true;

                codeFields[i].clear();
            }
            updateUndoButton();
            codeFields[0].requestFocus();
        });
        clearAllCodes.setTooltip(new Tooltip("Clear code fields"));

        pickerAndCopyButtonBox.setPadding(new Insets(5));
        pickerAndCopyButtonBox.setAlignment(Pos.CENTER);
        pickerAndCopyButtonBox.setSpacing(10);
        pickerAndCopyButtonBox.getChildren().addAll(copyToFirstEmpty, colorPickerUI);

        logStatic(Level.INFO, "BHB Buttons init completed", null);
    }

    /**
     * Called by {@link #buildBHB(Stage) buildBHB} 
     * 
     * Initializes the various boxes that make up the BHB main view. Most of these constructions rely on previously initialized data
     * 
     * @see VBox
     * @see HBox
     * @see BorderPane
     */
    private void buildBoxesBHB(){

        Border boxBorder = new Border(new BorderStroke(Color.BLACK, BorderStrokeStyle.SOLID, CornerRadii.EMPTY, BorderStroke.THIN));

        previewLabelsBHBBox.setAlignment(Pos.CENTER);
    
        previewCopyPane.setMinHeight(50);
        previewCopyPane.setLeft(copyButtonBHB);
        previewCopyPane.setCenter(previewLabelsBHBBox);
        previewCopyPane.setBorder(boxBorder);
        previewCopyPane.setPadding(new Insets(5));

        codesBox.setBorder(boxBorder);
        codesBox.setPadding(new Insets(5));
        codesBox.setAlignment(Pos.CENTER);
        codesBox.setSpacing(3);

        //Unlock fields based on filled codes
        unlockFields();

        colorPickerBox.setAlignment(Pos.CENTER);
        colorPickerBox.setMinHeight(160);
        colorPickerBox.setMinWidth(160);
        colorPickerBox.setPrefWidth(220);
        colorPickerBox.setBorder(boxBorder);
        colorPickerUI.setOnAction(e -> colorCircle.setFill(colorPickerUI.getValue()));
        colorPickerBox.getChildren().addAll(colorCircle, pickerAndCopyButtonBox);
        colorPickerBox.setSpacing(10);

        codesAndPickerBox.getChildren().addAll(codesBox, colorPickerBox);
        codesAndPickerBox.setBorder(boxBorder);
        
        nickInputBox.setAlignment(Pos.CENTER);
        nickInputBox.setPadding(new Insets(5));
        nickInputBox.setBorder(boxBorder);

        mainBHBBox.getChildren().addAll(codesAndPickerBox, nickInputBox, previewCopyPane);

        menuBar.toBack();
        mainBHBBox.toFront();

        logStatic(Level.INFO, "BHB Box init completed", null);
    }

    /**
     * Called by {@link #buildBHB(Stage) buildBHB} 
     * 
     * Creates misc. UI elements not created by other BHB "factories", this is the last called method before initialization of BHB is complete.
     * 
     * @see LimitedTextField
     * @see Circle
     */
    private void buildMiscBHB(){

        enterNicknameBHB.setPrefWidth(275);
        enterNicknameBHB.textProperty().addListener((observable, oldValue, newValue) -> updatePreviewBHB());
        
        colorCircle.setRadius(75);
        colorCircle.setFill(colorPickerUI.getValue());

        logStatic(Level.INFO, "BHB misc. init completed.", null);
    }

    //======================================================
    //|                 FACTORIES                          |
    //======================================================

    //Factory for code boxes in BHB
    private LimitedTextField makeCodeEnterField(Label codeColorLabel, int id){

        LimitedTextField colorCodeEnterField = new LimitedTextField(true, "[a-fA-F0-9]", 6);      
        colorCodeEnterField.setPrefWidth(75);
        colorCodeEnterField.setFont(new Font("Arial", 14));

        colorCodeEnterField.pressedProperty().addListener(listener -> lastEnteredField = colorCodeEnterField);
        colorCodeEnterField.focusedProperty().addListener(listener -> lastEnteredField = colorCodeEnterField);
        colorCodeEnterField.setOnKeyPressed(e -> {if(ctrlZ.match(e)) undoChange();});
        colorCodeEnterField.textProperty().addListener((observable, oldValue, newValue) -> {

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
            updatePreviewBHB();
            updateTextFieldFontSize(colorCodeEnterField);
        });

        codeFields[id - 1] = colorCodeEnterField;
        logStatic(Level.INFO, "Created enterCodeField with id: " + Integer.toString(id), null);
        return colorCodeEnterField;
    }

    //Factory for code box containers in BHB
    private HBox makeCodeBox(int id){
        Label codeId = new Label("Code " + id + ": ");
        Label codeColorLabel = new CodeColorLabel();
        previewColorLabels[id - 1] = codeColorLabel;
        previewColorLabels[id - 1].setTooltip(new Tooltip("Clear code"));
        LimitedTextField codeField = makeCodeEnterField(codeColorLabel, id);

        codeColorLabel.setOnMouseClicked(e -> {
            oldColorQueue.offerFirst(codeField.getText());
            referencedFieldsQueue.offerFirst(id - 1);
            updateUndoButton();
            codeField.clear();
        });

        Button upButton = new Button("↑");
        upButton.setTooltip(new Tooltip("Move code up"));
        upButton.setFont(new Font("Arial", 14));
        upButton.setStyle("-fx-font-weight: bold");
        upButton.setOnAction(e -> {
            if(id - 1 > 0 && isHexOk(codeField.getText())){
                String currentText = codeField.getText();
                String newText = codeFields[id - 2].getText();

                codeField.setText(newText);
                codeFields[id - 2].setText(currentText);
            }
        });
        if(id == 1) upButton.setDisable(true);
        upButtons[(id - 1)] = upButton;

        HBox newBox = new HBox(codeId, codeField, upButton, codeColorLabel);
        newBox.setAlignment(Pos.CENTER);
        newBox.setSpacing(6);
        newBox.setMinWidth(200);
        newBox.setMinHeight(30);

        logStatic(Level.INFO, "Created coebox with id: " + Integer.toString(id), null);
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
        Label prompt1 = new Label("Enter text: ");

        enterNicknameColorscheme.setPrefWidth(275);
        enterNicknameColorscheme.textProperty().addListener((observable, oldValue, newValue) -> {
            if(schemes.getValue() != null) updatePreviewColorscheme();
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

        logStatic(Level.INFO, "ColorScheme init completed.", null);
    }

    //======================================================
    //|                 UI CHANGES                         |
    //======================================================

    //Unlock or lock the undo button if there are items in the queue
    private void updateUndoButton(){
        undoItem.setDisable(referencedFieldsQueue.isEmpty());
    }

    private Object oldCenter;

    //Switch between BHB and Colorscheme
    private void switchStages(Stage stage){
        if(rootPane.getCenter().equals(mainBHBBox)){
            rootPane.setCenter(mainColorschemeBox);
            stage.setTitle("Colorscheme V2");
            //Disable un-used buttons in scene
            saveItem.setDisable(true);
            loadItem.setDisable(true);
            slotMachineColorsItem.setDisable(true);
            switchStagesItem.setText("Switch to BHB");
            logStatic(Level.INFO, "Switched to ColorScheme V2", null);
        }
        else{
            rootPane.setCenter(mainBHBBox);
            stage.setTitle("Blazin's Hex Blender");
            //Re-enable used buttons in scene
            saveItem.setDisable(false);
            loadItem.setDisable(false);
            slotMachineColorsItem.setDisable(false);
            switchStagesItem.setText("Switch to Colorscheme V2");
            logStatic(Level.INFO, "Switched to BHB", null);
        }
    }

    private void toggleSettings(){
        if(rootPane.getCenter().equals(mainBHBBox) || rootPane.getCenter().equals(mainColorschemeBox)){
            oldCenter = rootPane.getCenter();
            rootPane.setCenter(settingsPane);
            //Disable items not used in scene
            saveItem.setDisable(true);
            loadItem.setDisable(true);
            switchStagesItem.setDisable(true);
            slotMachineColorsItem.setDisable(true);
            logStatic(Level.INFO, "Switched to settings", null);
            settingsItem.setText("Hide Settings");
        }
        else{
            if(oldCenter instanceof VBox){
                //Handles rendering nickname labels after justification was changed L->R or R->L
                if(renderOnHook){
                    renderOnHook = false;
                    updatePreviewBHB();
                }
                //Show the main box again
                rootPane.setCenter(mainBHBBox);
                switchStagesItem.setText("Switch to Colorscheme V2");
            }
            else if(oldCenter instanceof BorderPane){
                rootPane.setCenter(mainColorschemeBox);
                switchStagesItem.setText("Switch to BHB");
            }
            //Re-enable used buttons in scene
            saveItem.setDisable(false);
            loadItem.setDisable(false);
            switchStagesItem.setDisable(false);
            slotMachineColorsItem.setDisable(false);
            settingsItem.setText("Settings");
            
            logStatic(Level.INFO, "Switched to BHB", null);
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
    public void updatePreviewBHB(){

        int userInputLength = enterNicknameBHB.getText().length();

        //Need to be at least 2 codes
        int validCodes = 0;
        for(int i = 0; i <= 5; i++) if(!codeFields[i].isDisabled() && isHexOk(codeFields[i].getText())) validCodes++;
        if (userInputLength >=3 && validCodes >= 2 && (userInputLength >= ((validCodes * 2) - 1))){
            String[] codeArray = new String[validCodes];
            for(int i = 0; i < validCodes; i++) codeArray[i] = codeFields[i].getText();

            previewLabelsBHBBox.setPrefHeight(defaultPreviewHeight);
            currentNickBHB = Blend.blendMain(validCodes, enterNicknameBHB.getText(), codeArray, justificationPriority.getValue().equals("Right"));
            parseNickToLabel(currentNickBHB, previewLabelsBHBBox, selectedScheme, true);
            return;        
        }
        previewLabelsBHBBox.getChildren().clear();
        previewLabelsBHBBox.setPrefHeight(0);
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
            if(enterNicknameColorscheme.getText().charAt(i) == ' ') counter --;
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
    private void changeTheme(Scene mainScene, Label[] labelColorPreviews){
        if(currentTheme.equals("LIGHT")) goDark(mainScene,labelColorPreviews);
        else goLight(mainScene, labelColorPreviews);
    }

    //Dark mode
    private void goDark(Scene mainScene, Label[] labelColorPreviews){
        for(Button b : upButtons) b.setTextFill(Color.WHITE);
        mainScene.getStylesheets().add(getClass().getClassLoader().getResource("dark.css").toString());
        for(int i = 0; i <= 5; ++i){
            if(codeFields[i].getText().equals("")){
                labelColorPreviews[i].setBackground(new Background(new BackgroundFill(Color.rgb(92, 100, 108), CornerRadii.EMPTY, Insets.EMPTY)));
            }
        }
        currentTheme = "DARK";
        copyButtonBHB.setGraphic(new CopyButtonIcon(false));
        copyButtonColorscheme.setGraphic(new CopyButtonIcon(false));
    }

    //Light mode
    private void goLight(Scene mainScene, Label[] labelColorPreviews){
        for(Button b : upButtons) b.setTextFill(Color.BLACK);
        mainScene.getStylesheets().remove(getClass().getClassLoader().getResource("dark.css").toString());
        for(int i = 0; i <=5; i++){
            if(codeFields[i].getText().equals("")){
                labelColorPreviews[i].setBackground(new Background(new BackgroundFill(Color.rgb(
                    Integer.parseInt("F2",16),
                    Integer.parseInt("F2",16),
                    Integer.parseInt("F2",16)), CornerRadii.EMPTY, Insets.EMPTY)));
            }
        }
        currentTheme = "LIGHT";
        copyButtonBHB.setGraphic(new CopyButtonIcon(true));
        copyButtonColorscheme.setGraphic(new CopyButtonIcon(true));
    }

    //======================================================
    //|                 UI INTEGRATED HELPERS              |
    //======================================================

    private void doClipboardCopy(){
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        StringSelection stringSelection = new StringSelection("");
        if(rootPane.getCenter().equals(mainBHBBox) && currentNickBHB != null && !currentNickBHB.equals("")) stringSelection = new StringSelection(currentNickBHB);
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
        if(loadedSchemes.get(0) == null) loadedSchemes.set(0, new Scheme("Random", randomArray));
        else loadedSchemes.get(0).setScheme(randomArray);
    }

    //Allows for new generation mid program
    private void generateNewRandomHexScheme(){
        
        String[] randomHexArray = new String[32];
        for(int i = 0; i < 32; ++i) randomHexArray[i] = new RandomHexGenerator().generate();

        //Create the scheme
        if(loadedSchemes.get(1) == null) loadedSchemes.set(1, new Scheme("Random Hex", randomHexArray));
        else loadedSchemes.get(1).setScheme(randomHexArray);
    }

    //======================================================
    //|                 UPDATERS                           |
    //======================================================

    private void startSelfUpdate(){

        //Create buttons for alert
        ButtonType no = new ButtonType("No", ButtonBar.ButtonData.OK_DONE);
        ButtonType yes = new ButtonType("Yes", ButtonBar.ButtonData.CANCEL_CLOSE);

        Alert updateAlert = new Alert(AlertType.CONFIRMATION, "An updated version of BHB is available. Current: " 
            + CURRENT_VERSION + ", New: " + LATEST_VERSION + ". Update now? This will restart your program.", no, yes);
        updateAlert.setHeaderText("Out of date");
        updateAlert.setTitle("Updates found");
        Optional<ButtonType> result = updateAlert.showAndWait();

        if(result.orElse(no) == yes){
            String osName = System.getProperty("os.name");
            String res;
            Thread saveThread = new Thread(this::saveApplicationState);
            saveThread.start();

            if(osName.length() >= 7 &&  osName.substring(0, 7).equals("Windows")){
                try { saveThread.join();}
                catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    logStatic(Level.SEVERE, "Exception in updateSelfWindows(); Stacktrace: " + ex.getStackTrace(), ex);
                }
                alreadySaved = true;
                res = updater.windowsUpdater();
            }
            else if(osName.substring(0,5).equals("Linux")){
                try { saveThread.join();}
                catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    logStatic(Level.SEVERE, "Exception in updateSelfLinux(); Stacktrace: " + ex.getStackTrace(), ex);
                }
                alreadySaved = true;
                res = updater.linuxUpdater();
            }
            else {
                try { saveThread.join();}
                catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    logStatic(Level.SEVERE, "Exception in updateSelfMacOS(); Stacktrace: " + ex.getStackTrace(), ex);
                }
                res = updater.macOSUpdater();   
            }
            if(!(res.equals(""))) logStatic(Level.SEVERE, res, null);
            System.exit(0);
        }
    }

    //======================================================
    //|                 MISCELLANEOUS HELPERS              |
    //======================================================

    //Static logger referencing
    private static void logStatic(Level logLevel, String logMessage, Throwable thrown){
        //Do not log if not error if verbose is off
        if(!verboseLogging && logLevel.equals(Level.INFO)) return;
        //Do not log "thrown" with info, as it will be null
        if(logLevel.equals(Level.INFO)) Logger.getGlobal().log(logLevel, logMessage);
        else Logger.getGlobal().log(logLevel,  logMessage, thrown);
        ++loggedItems;
    }

    //Initalize the static logger accessed during runtime
    private void initLogger(){
        
        if(logFile.exists()) logFile.delete();
        try {  
            // This block configure the logger with handler and formatter  
            FileHandler fh = new FileHandler("log.txt");  
            log.addHandler(fh);
            SimpleFormatter formatter = new SimpleFormatter();  
            fh.setFormatter(formatter);
            logStatic(Level.INFO, "Logger init", null);
    
        } catch (SecurityException | IOException e) {
            //Usually would be logged, however log init failed here (! fatal)
            e.printStackTrace();  
        }
    }

    private static void killOldLinuxProcess(String[] args){
        //Solely for testing purposes
        if(args[0].equals("0")) return;

        logStatic(Level.INFO, "Old process found with pid" + args[0] +  ", killing...", null);
        ProcessBuilder processBuilder = new ProcessBuilder().command("nohup", "kill", "-9", args[0]);
        try {
            processBuilder.directory(new File(System.getProperty("user.dir")));
            processBuilder.redirectErrorStream(false);
            processBuilder.start();
    
        } catch (IOException e) {
            logStatic(Level.SEVERE, "Exception in determining old process. Stacktrace: " + e.getStackTrace(), e);
            return;
        }
        logStatic(Level.INFO, "Old process killed", null);
    }

    //Non-intrusive loading, will not force
    private boolean tryLoad(Stage stage){

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
                        if(isHexOk(line) || line.equals("")){
                            codeFields[i].setText(line);
                        }
                        else {
                            //Error in file reading - invalid chars reached
                            logStatic(Level.SEVERE, "Error in file reading, invalid characters reached", null);
                            return false;
                        }
                    }    
                }

                //Set justification priority from tempstore, default val is "Left"
                String justificationSaved = bReader.readLine();
                justificationPriority.setValue((justificationSaved != null) ? justificationSaved : "Left");

                logStatic(Level.INFO, "tryLoad(); execution was succesful", null);
                return true;
            }
            //Catch various IO errors from reading the files
            catch(IOException | StringIndexOutOfBoundsException ex)
            {
                logStatic(Level.SEVERE, "Error during tryLoad(); Stacktrace: " + ex.getStackTrace(), ex);
            }
        }
        return false;
    }

    //Intrusive loading technique, will disrupt the program's functionality temporarily
    private void forceLoad(Scene mainScene, Label[] labelColorPreviews){

        //Create a temporary reference to the file
        File tempStore = new File(System.getProperty("user.dir") + "/tempstore.txt");
        if(!tempStore.exists()) return;

        //try to make the file readable ~ Will only work on Windows
        try{
            Runtime.getRuntime().exec("attrib -H \"" +  System.getProperty("user.dir") + "/tempstore.txt\"" );
        }
        catch(IOException ex){
            logStatic(Level.INFO, "Not a Windows machine... attrib failed", ex);
        }

        //Create a new instances of the reader
        try( FileReader fReader = new FileReader(tempStore); //Basic reader, throws FileNotFoundException
        BufferedReader bReader = new BufferedReader(fReader);)
        { 
            String line;
            String[] codes = new String[6];
            //Read the codes
            for(int i = 0; i < 6; i++) if((line = bReader.readLine()) != null && isHexOk(line)) codes[i] = line;
            for(int i = 0; i < 6; i++) if(codes[i] != null) codeFields[i].setText(codes[i]);
            enterNicknameBHB.setText(bReader.readLine().replace("\n", ""));

            //Set theme based on config
            String savedTheme = bReader.readLine();
            if(savedTheme.equals("DARK")) {
                changeTheme(mainScene, labelColorPreviews);
                darkMode.setValue("On");
            }

            String savedJustification = bReader.readLine();
            if(savedJustification != null) justificationPriority.setValue(savedJustification);
            else justificationPriority.setValue("Left");
        }
        catch(IOException | StringIndexOutOfBoundsException e)
        {
            logStatic(Level.SEVERE, "Exception in forceLoad(); Stacktrace: " + e.getStackTrace(), e);
        }

        //Delete the file once loaded
        logStatic(Level.INFO, "tempStore.txt deleted?: " + Boolean.toString(tempStore.delete()), null);
    }

    //Non-intrusive saving, will not force
    private void saveCodes(){

        int goodCodes = 0;
        for(int i = 0; i < 6; i++) if(isHexOk(codeFields[i].getText())) goodCodes++;

        if(goodCodes >= 1){
            FileChooser configChooser = new FileChooser();
            configChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("BHB Configs (.txt)", "*.txt"));
            configChooser.setTitle("Save configuration file");
            //Parses open windows and passes the first index to the save dialog, this should never error
            File saveFile = new File("");
            Object[] openWindows = javafx.stage.Window.getWindows().stream().filter(Window::isShowing).toArray();
            for(Object iterO : openWindows) if(iterO != null) saveFile = configChooser.showSaveDialog((Window)iterO);

            if(saveFile != null){

                String[] codes = new String[6];
                for(int i = 0; i < 6; i++) if(isHexOk(codeFields[i].getText())) codes[i] = codeFields[i].getText();

                try( FileWriter fWriter = new FileWriter(saveFile); //Basic reader, throws FileNotFoundException
                BufferedWriter bWriter = new BufferedWriter(fWriter);)
                { 
                    for(int i = 0; i < 6; i++){
                        bWriter.write(codes[i]);
                        if(i != 5) bWriter.newLine();
                    }
                }
                catch(IOException | StringIndexOutOfBoundsException e)
                {
                    new BHBAlert(AlertType.ERROR, "There was a problem saving the config.", "", "Error: " + e.getLocalizedMessage()).showAndWait();
                    logStatic(Level.SEVERE, "Exception in trySave(); Stacktrace: " + e.getStackTrace(), e);
                }
            }
        }
        else{
            new BHBAlert(AlertType.ERROR, "You do not have any codes to save. No file was created.", "", "Nothing to save").showAndWait();
            logStatic(Level.INFO, "Empty save was attempted, and intercepted", null);
        }   
    }

    //Intrusive saving technique, will disrupt the program's functionality temporarily
    private void saveApplicationState(){

        //Flag used during updates so that the JVM doesn't terminate and error out
        if(!alreadySaved){
            File tempStore = new File(System.getProperty("user.dir") + "/tempstore.txt");
            String[] codes = new String[6];
            for(int i = 0; i < 6; i++) codes[i] = codeFields[i].getText();
            try( FileWriter fWriter = new FileWriter(tempStore); //Basic reader, throws FileNotFoundException
            BufferedWriter bWriter = new BufferedWriter(fWriter);)
            {
                //Write the 6 code from boxes (including empty lines)
                for(int i = 0; i < 6; i++) bWriter.write(codes[i] + "\n");
                bWriter.write(enterNicknameBHB.getText() + "\n");
                bWriter.write(currentTheme + "\n");
                bWriter.write(justificationPriority.getValue());

                //Hides the file from the user - only works on Windows
                String osName = System.getProperty("os.name");
                if(osName.contains("Windows")){
                    Runtime.getRuntime().exec("attrib +H \"" +  System.getProperty("user.dir") + "/tempstore.txt\"" );
                }
            }
            catch(IOException | StringIndexOutOfBoundsException e)
            {
                new BHBAlert(AlertType.ERROR, "There was a problem saving the config.", "", "Error: " + e.getLocalizedMessage()).showAndWait();
                logStatic(Level.SEVERE, "Exception in forceSave(); Stacktrace: " + e.getStackTrace(), e);
            }
        }
    }

    //Parses JSON from a url
    private String getJSON(String url) {

        try{
            return(new JSONObject(IOUtils.toString(new URL(url), StandardCharsets.UTF_8)).toString());
        }
        catch(IOException ex){
            new BHBAlert(AlertType.ERROR, "An unexpected error occurred.", "", "Error: " + ex.getLocalizedMessage()).showAndWait();
            logStatic(Level.SEVERE, "IOException during getJSON(); Stacktrace: " + ex.getStackTrace(), ex);
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

        loadedSchemes = new ArrayList<>(
            Arrays.asList(
                null, //Reserved for random
                null, //Reserved for random hex
                new Scheme("Rainbow", "4c6eab9d5".split("")), //Rainbow
                new Scheme("Ordered", "0123456789abcdef".split("")) //Ordered
            ) 
        );

        //Make the random scheme different every time
        generateNewRandomScheme();

        //Make the random hex scheme different every time
        generateNewRandomHexScheme();
    }

    //Boolean is the hex valid; ie, does it contain any invalid chars, is it 6 chars, etc.
    public static boolean isHexOk(String hex){
        try{
            return(Integer.parseInt(hex, 16) >= 0) && (hex.length() == 6);
        }
        catch(Exception e){
            return false;
        }
    }
    
}