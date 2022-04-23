package com;

//======================================================
//|                 IMPORTS                            |
//======================================================

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.*;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.image.*;
import javafx.scene.input.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.*;
import javafx.stage.*;

import java.awt.datatransfer.StringSelection;
import java.awt.Toolkit;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.net.URL;
import java.util.*;
import java.util.logging.*;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;
import org.json.JSONObject;
import com.google.gson.*;

import com.gui.*;
import com.util.gui_helper.*;
import com.util.updater.Updater;

public class BHBMainGUI extends Application {

    //======================================================
    //|                 GLOBAL VARS                        |
    //======================================================

    //Global current version indicator
    private static final Property<String> CURRENT_VERSION = new SimpleStringProperty("1.5.3"){
        @Override
        public String toString(){ return this.getValue();}
    };
    //Gets the latest version number from the git repo
    private static final Property<String> LATEST_VERSION = new SimpleStringProperty(getTagFromGitJson("tag_name")){
        @Override
        public String toString(){ return this.getValue();}
    };

    //Enable verbose logging
    private static final Property<Boolean> verboseLogging = new SimpleBooleanProperty(false);
    //Count logged items
    private static final Property<Number> loggedItems = new SimpleIntegerProperty(0);
    //Init log file
    private final File logFile = new File("log.txt");
    
    //Global updater object
    private final Updater updater = new Updater(LATEST_VERSION.getValue());
    //Default theme is light
    private final Property<String> currentTheme = new SimpleStringProperty("LIGHT");
    //Stores the current nick for BHB
    private final Property<String> currentNickBHB = new SimpleStringProperty();
    //Stores the current nick for Colorscheme
    private final Property<String> currentNickColorScheme = new SimpleStringProperty();
    //Creates a logger for debug
    private final Logger log = Logger.getLogger(BHBMainGUI.class.getSimpleName());
    //Prevents threading errors in some cases
    private final Property<Boolean> alreadySaved = new SimpleBooleanProperty(false);
    //Passed around for use in different UI elements in colorscheme
    private Scheme selectedScheme;
    //Store values when sending colors around
    private final Deque<String> oldColorQueue = new ArrayDeque<>();
    private final Deque<Integer> referencedFieldsQueue = new ArrayDeque<>();
    private final Property<Boolean> lastActionClearAll = new SimpleBooleanProperty(false);

    //Key binding defs
    private final KeyCodeCombination ctrlZ = new KeyCodeCombination(KeyCode.Z, KeyCombination.CONTROL_ANY);

    //Statics for themes
    private static final Background DARK_BACKGROUND = new Background(new BackgroundFill(Color.rgb(92, 100, 108), CornerRadii.EMPTY, Insets.EMPTY));
    private static final Background DEF_BACKGROUND = new Background(new BackgroundFill(Color.web("F2F2F2"), CornerRadii.EMPTY, Insets.EMPTY));
    private static final Border DEF_BORDER = new Border(new BorderStroke(Color.BLACK, BorderStrokeStyle.SOLID, CornerRadii.EMPTY, BorderStroke.THIN));

    //For global access, changing disabling/editing between scenes
    private final BorderPane rootPane = new BorderPane();
    private static Object oldCenter;
    private final MenuItem switchStagesItem = new MenuItem("Switch to Colorscheme V2");
    private final MenuBar menuBar = new MenuBar();
    private final MenuItem saveItem = new MenuItem("Save");
    private final MenuItem undoItem = new MenuItem("Undo");
    private final MenuItem loadItem = new MenuItem("Load");
    private Scene mainScene;

    //Colorscheme 
    private final BorderPane mainColorschemeBox = new BorderPane();
    private final Button copyButtonColorscheme = new Button("", new CopyButtonIcon(true));
    private final HBox previewLabelsColorscheme = new HBox();
    private final ComboBox<Scheme> schemes = new ComboBox<>();
    private final LimitedTextField enterNicknameColorscheme = new LimitedTextField(false, "[A-Za-z0-9_\\[\\] ]", -1);
    private ArrayList<Scheme> loadedSchemes = new ArrayList<>();

    //BHB
    private final VBox mainBHBBox = new VBox();   
    private final List<Button> upButtons = new ArrayList<>(6);
    private final Button copyButtonBHB = new Button("", new CopyButtonIcon(true));
    private final Button clearAllCodes = new Button("Clear All");
    private final Button copyToFirstEmpty = new Button("<<<");

    private LimitedTextField lastEnteredField;
    private final List<LimitedTextField> codeFields = new ArrayList<>(6);
    private final LimitedTextField enterNicknameBHB = new LimitedTextField(false, "[A-Za-z0-9_\\[\\]]", -1);

    private final HBox previewLabelsBHBBox = new HBox();
    private final HBox codesAndPickerBox = new HBox();
    private final HBox pickerAndCopyButtonBox = new HBox();
    private final HBox nickInputBox = new HBox(new Label("Enter text: "), enterNicknameBHB);

    private final List<Label> previewColorLabels = new ArrayList<>(6);
    
    // Each individual menu item for the submenus - any var ending in ..item is a MenuItem, these are all theoretically global,
    // however not all are used in both modes
    private static final MenuItem slotMachineColorsItem = new MenuItem("Slot Machine (Seizure Warning)");
    private static final MenuItem settingsItem = new MenuItem("Settings");
    private static final MenuItem updateCheckerItem = new MenuItem("Check For Updates");
    private static final MenuItem aboutItem = new MenuItem("About");
    private static final MenuItem gitHubItem = new MenuItem("Visit the GitHub page");

    //Sub menus displayed in the MenuBar
    private static final Menu menuEdit = new Menu("Edit");
    private static final Menu menuTools = new Menu("Tools");
    private static final Menu menuHelp = new Menu("Help");
     
    private final VBox codesBox = new VBox(makeCodeBox(1), makeCodeBox(2), makeCodeBox(3), makeCodeBox(4), makeCodeBox(5), makeCodeBox(6), clearAllCodes);
    private final VBox colorPickerBox = new VBox();

    private final Circle colorCircle = new Circle();
    private final ColorPicker colorPickerUI = new ColorPicker(Color.BLACK);
    private final BorderPane previewCopyPane = new BorderPane();

    //Controls whether or not to re-render the preview labels on the next render of the BHB GUI
    //This is used when the justification priority is changed
    private final Property<Boolean> renderOnHook = new SimpleBooleanProperty(false);

    //Settings
    private final BorderPane settingsPane = new BorderPane();
    private final Setting justificationPriority = new Setting("Justification Priority", "When a string cannot evenly be split, there will need to be longer strings on one side.\nThis setting changes which side gets longer strings.", 
    "Left", Arrays.asList("Left", "Right")){
        @Override
        public void execute(){
            renderOnHook.setValue(true);
        }   
    };
    private final Setting darkMode = new Setting("Dark Mode", "Enable dark mode for the GUI.", "Off", Arrays.asList("Off", "On")){
        @Override
        public void execute(){
            changeTheme(mainScene, previewColorLabels);
        }
    };
    private final Setting delimitInputs = new Setting("De-limit Inputs", "Allow use of all characters in the ColorScheme 2 and BHB input fields.", "Off", Arrays.asList("Off", "On")){
        @Override
        public void execute(){
            enterNicknameColorscheme.setRestrict(this.getValue().equals("Off") ? "[A-Za-z0-9_\\[\\] ]" : ".");
            enterNicknameBHB.setRestrict(this.getValue().equals("Off") ? "[A-Za-z0-9_\\[\\] ]" : ".");
        }
    };
    
    //======================================================
    //|                 MAIN METHODS                       |
    //======================================================

    //Main runtime
    public static void main(String[] args) {
        
        //Catch flag for old linux process (used in update thread)
        if(args.length >=1 && args[0] != null) killOldLinuxProcess(args);

        //Catch flag for verbose logging mode
        if(args.length >=2 && args[1] != null && args[1].equalsIgnoreCase("-v")) verboseLogging.setValue(true);

        //Start the application
        launch();    
    }

    //Start the program
    @Override
    public void start(Stage stage) throws Exception {

        //Initialize the logger
        initLogger();

        drawFrames(stage);

        //Check for updates when the program launches
        new Thread(() -> Platform.runLater(() -> {
            if(checkForUpdates()) startSelfUpdate();
        })).start();
    }

    public void drawFrames(Stage stage){
        //Create the pane scene for BHB
        buildBHB(stage);

        //Create the pane scene for Colorscheme
        buildColorscheme();

        rootPane.setTop(menuBar);
        rootPane.setCenter(rootPane.getCenter() == null ? mainBHBBox : rootPane.getCenter());
        mainScene = new Scene(rootPane);

        stage.setMinHeight(400);
        stage.setMinWidth(436);
        stage.setScene(mainScene);
        stage.setTitle("Hex Blender");
        stage.show();
        //stage.setResizable(false);

        //Load from tempstore
        forceLoad(mainScene, previewColorLabels);

        //Build the settings page
        buildSettingsBox();

        //Init update
        updateUndoButton();
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
            if(loggedItems.getValue().intValue() < 1 && !Boolean.TRUE.equals(verboseLogging.getValue())) logFile.delete();
        }));

        //Delete old update files that exist
        new File(System.getProperty("user.dir") + "/Update.sh").delete();

        buildMenuItemsBHB(stage);
        buildMenusBHB();
        buildButtonsBHB();
        buildMiscBHB();
        buildBoxesBHB();

        logStatic(Level.INFO, "BHB init completed", null);
    }

    private void buildSettingsBox(){
        settingsPane.getChildren().clear();

        ImageView exitSettingsIcon = new ImageView(new Image(getClass().getClassLoader().getResource("exit.png").toString()));

        exitSettingsIcon.setFitWidth(30);
        exitSettingsIcon.setFitHeight(30);

        Button exitSettingsButton = new Button();
        exitSettingsButton.setMinWidth(mainScene.getWidth());
        exitSettingsButton.setTooltip(new Tooltip("Exit the settings menu"));
        exitSettingsButton.setGraphic(exitSettingsIcon);
        exitSettingsButton.setOnAction(e -> toggleSettings());

        HBox justificationBox = buildSetting(justificationPriority);
        HBox darkModeBox = buildSetting(darkMode);
        HBox delimitInputsBox = buildSetting(delimitInputs);
        VBox settingsBox = new VBox(justificationBox, darkModeBox, delimitInputsBox);

        settingsPane.setTop(settingsBox);
        settingsPane.setBottom(exitSettingsButton);
        BorderPane.setAlignment(exitSettingsButton, Pos.CENTER);
    }

    private HBox buildSetting(Setting setting){

        HBox masterBox = new HBox();
        masterBox.setMinHeight(45);
        masterBox.setAlignment(Pos.CENTER);
        masterBox.setBorder(DEF_BORDER);
        ImageView helpIcon = new ImageView(new Image(getClass().getClassLoader().getResource("help.png").toString()));
        helpIcon.setFitWidth(15);
        helpIcon.setFitHeight(15);

        Label helpLabel = new Label();
        helpLabel.setAlignment(Pos.BASELINE_RIGHT);
        helpLabel.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        helpLabel.setGraphic(helpIcon);
        Tooltip helpTooltip = new Tooltip(setting.getDescription());
        helpTooltip.setStyle("-fx-font-size: 12");
        helpLabel.setTooltip(helpTooltip);

        HBox newBox = new HBox();
        newBox.setAlignment(Pos.CENTER);
        newBox.setSpacing(6);
        newBox.setMinWidth(250);
        newBox.setMinHeight(30);

        //Create the label
        Label settingName = new Label(setting.getName() + ":");
        RadioButton rb1 = new RadioButton(setting.getOptions().get(0));
        RadioButton rb2 = new RadioButton(setting.getOptions().get(1));  

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

        if(setting.getValue().equals(setting.getOptions().get(0))) rb1.setSelected(true);
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

        undoItem.setOnAction(e -> undoChange());

        switchStagesItem.setOnAction(e -> switchStages(stage));

        settingsItem.setOnAction(e -> toggleSettings());

        slotMachineColorsItem.setOnAction(e -> new SlotMachineColors(codeFields).start());  

        aboutItem.setOnAction(e -> {
            String publishDate = getTagFromGitJson("published_at");
            String releaseDate = compareVersions(CURRENT_VERSION.getValue(), LATEST_VERSION.getValue()) == -1 ? 
                "Release date unknown" : "Release date: " + publishDate.substring(0, publishDate.length() - 1).replace("T", " ").split(" ")[0];

            new BHBAlert(AlertType.INFORMATION, releaseDate, "About BHB", "Version: " + CURRENT_VERSION).showAndWait();
        });

        gitHubItem.setOnAction(e -> getHostServices().showDocument("https://github.com/DavidArthurCole/bhb"));

        updateCheckerItem.setOnAction(e -> {
            
            if(checkForUpdates()) startSelfUpdate();
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
        menuEdit.getItems().addAll(undoItem);
        menuHelp.getItems().addAll(aboutItem, gitHubItem, updateCheckerItem);
        menuTools.getItems().addAll(slotMachineColorsItem, switchStagesItem, settingsItem);

        menuBar.getMenus().addAll(menuEdit, menuTools, menuHelp);
        menuBar.setBorder(DEF_BORDER);

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
        copyButtonBHB.setTooltip(new Tooltip("Copy nickname to clipboard"));
        copyButtonBHB.setOnAction(e -> doClipboardCopy());

        copyToFirstEmpty.setTooltip(new Tooltip("Move colorpicker color to codes"));
        copyToFirstEmpty.setFont(Font.font("Arial", FontWeight.SEMI_BOLD, 14));
        copyToFirstEmpty.setOnAction( e -> {

            int findMe = codeFields.indexOf(lastEnteredField);

            if(lastEnteredField != null && !lastEnteredField.isDisabled()){

                oldColorQueue.offerFirst(codeFields.get(findMe).getText());
                referencedFieldsQueue.offerFirst(findMe);
                updateUndoButton();

                lastEnteredField.setText(colorPickerUI.getValue().toString().substring(2,8).toUpperCase());
                if(findMe + 1 < 6) lastEnteredField = codeFields.get(findMe + 1);
            }
            else{
                for(LimitedTextField lt : codeFields){
                    if(!isHexOk(lt.getText())){
                        oldColorQueue.offerFirst(lt.getText());
                        referencedFieldsQueue.offerFirst(codeFields.indexOf(lt));
                        updateUndoButton();

                        lt.setText(colorPickerUI.getValue().toString().substring(2,8).toUpperCase());
                        break;
                    }
                }
            } 
            
            lastEnteredField.requestFocus();
        });

        clearAllCodes.setOnAction(e -> {
            for(LimitedTextField lt : codeFields){
                referencedFieldsQueue.offerFirst(codeFields.indexOf(lt));
                oldColorQueue.offerFirst(lt.getText());
                lt.clear();
            }
            lastActionClearAll.setValue(true);
            updateUndoButton();
            codeFields.get(0).requestFocus();
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

        previewLabelsBHBBox.setAlignment(Pos.CENTER);
    
        previewCopyPane.setMinHeight(50);
        previewCopyPane.setLeft(copyButtonBHB);
        BorderPane.setAlignment(copyButtonBHB, Pos.CENTER_LEFT);
        previewCopyPane.setCenter(previewLabelsBHBBox);
        previewCopyPane.setBorder(DEF_BORDER);
        previewCopyPane.setPadding(new Insets(5));

        previewCopyPane.prefWidthProperty().bind(rootPane.widthProperty()); // Dynamic resizing
        previewCopyPane.prefHeightProperty().bind(rootPane.heightProperty()); //Dynamic resizing

        codesBox.setBorder(DEF_BORDER);
        codesBox.setPadding(new Insets(5));
        codesBox.setAlignment(Pos.CENTER);
        codesBox.setSpacing(3);

        //Unlock fields based on filled codes
        unlockFields();

        colorPickerBox.setAlignment(Pos.CENTER);
        colorPickerBox.setPrefSize(220, 160);
        colorPickerBox.setBorder(DEF_BORDER);
        colorPickerUI.setOnAction(e -> colorCircle.setFill(colorPickerUI.getValue()));
        colorPickerBox.getChildren().addAll(colorCircle, pickerAndCopyButtonBox);
        colorPickerBox.setSpacing(10);

        // Dynamic resizing for BHB Boxes
        // This is cascading. Each box binds to its parent
        mainBHBBox.prefHeightProperty().bind(rootPane.heightProperty());
        mainBHBBox.prefWidthProperty().bind(rootPane.widthProperty());

        codesAndPickerBox.prefHeightProperty().bind(mainBHBBox.heightProperty()); //Dynamic resizing
        codesAndPickerBox.prefWidthProperty().bind(mainBHBBox.widthProperty()); //Dynamic resizing
        
        codesBox.prefHeightProperty().bind(codesAndPickerBox.heightProperty()); //Dynamic resizing
        codesBox.prefWidthProperty().bind(codesAndPickerBox.widthProperty()); //Dynamic resizing
        colorPickerBox.prefHeightProperty().bind(codesAndPickerBox.heightProperty()); //Dynamic resizing
        colorPickerBox.prefWidthProperty().bind(codesAndPickerBox.widthProperty()); //Dynamic resizing
        nickInputBox.prefHeightProperty().bind(codesAndPickerBox.heightProperty()); //Dynamic resizing
        nickInputBox.prefWidthProperty().bind(codesAndPickerBox.widthProperty()); //Dynamic resizing

        codesAndPickerBox.getChildren().addAll(codesBox, colorPickerBox);
        codesAndPickerBox.setBorder(DEF_BORDER);
        
        nickInputBox.setAlignment(Pos.CENTER);
        nickInputBox.setPadding(new Insets(5));
        nickInputBox.setBorder(DEF_BORDER);

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

        LimitedTextField colorCodeEnterField = new LimitedTextField(true, "[a-fA-F0-9]", 6);;
        colorCodeEnterField.setPrefWidth(75);
        colorCodeEnterField.setFont(new Font("Arial", 14));

        colorCodeEnterField.pressedProperty().addListener(listener -> lastEnteredField = colorCodeEnterField);
        colorCodeEnterField.focusedProperty().addListener(listener -> lastEnteredField = colorCodeEnterField);
        colorCodeEnterField.setOnKeyPressed(e -> {if(ctrlZ.match(e)) undoChange();});
        colorCodeEnterField.textProperty().addListener((observable, oldValue, newValue) -> {
            if(isHexOk(newValue)) codeColorLabel.setBackground(new Background(new BackgroundFill(Color.web(newValue), CornerRadii.EMPTY, Insets.EMPTY)));
            else codeColorLabel.setBackground(currentTheme.getValue().equals("DARK") ? DARK_BACKGROUND : DEF_BACKGROUND);   
            unlockFields();
            updatePreviewBHB();
            updateTextFieldFontSize(colorCodeEnterField);
        });

        codeFields.add((id - 1), colorCodeEnterField);
        logStatic(Level.INFO, "Created enterCodeField with id: " + Integer.toString(id), null);
        return colorCodeEnterField;
    }

    //Factory for code box containers in BHB
    private HBox makeCodeBox(int id){
        Label codeId = new Label("Code " + id + ": ");
        Label codeColorLabel = new CodeColorLabel();
        previewColorLabels.add((id - 1), codeColorLabel);
        previewColorLabels.get(id - 1).setTooltip(new Tooltip("Clear code"));
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
                codeField.setText(codeFields.get(id - 2).getText());
                codeFields.get(id - 2).setText(currentText);
            }
        });
        if(id == 1) upButton.setDisable(true);
        upButtons.add((id - 1), upButton);

        HBox codeBox = new HBox(codeId, codeField, upButton, codeColorLabel);
        codeBox.setAlignment(Pos.CENTER);
        codeBox.setSpacing(6);
        codeBox.setMinSize(200, 30);

        logStatic(Level.INFO, "Created codebox with id: " + Integer.toString(id), null);
        return codeBox;
    }

    //Builds components for colorscheme
    private void buildColorscheme(){
        copyButtonColorscheme.setTooltip(new Tooltip("Copy nickname to clipboard"));
        copyButtonColorscheme.setOnAction(e -> doClipboardCopy());

        Label prompt = new Label("Choose a scheme: ");
        schemes.setMinWidth(140);

        schemes.valueProperty().addListener((observable, oldValue, newValue) -> {
            if(enterNicknameColorscheme.getText().length() > 0){
                generateNewRandomScheme();
                generateNewRandomHexScheme();
                updatePreviewColorscheme();
            }  
        });

        enterNicknameColorscheme.setPrefWidth(275);
        enterNicknameColorscheme.textProperty().addListener((observable, oldValue, newValue) -> {
            if(schemes.getValue() != null) updatePreviewColorscheme();
        });

        initSchemes();
        reloadSchemes();

        Label previewLabel = new Label("Nickname preview:");
        previewLabelsColorscheme.setAlignment(Pos.CENTER);

        BorderPane nickPreview = new BorderPane();
        nickPreview.setBorder(DEF_BORDER);
        nickPreview.setTop(previewLabel);
        nickPreview.setCenter(previewLabelsColorscheme);
        nickPreview.setBottom(copyButtonColorscheme);
        nickPreview.setMinHeight(new Label("L").getHeight() + 10.0);
        nickPreview.setPadding(new Insets(5));
        BorderPane.setAlignment(previewLabel, Pos.CENTER);
        BorderPane.setAlignment(copyButtonColorscheme, Pos.CENTER);


        //Add everything for the "second scene" to the VBox
        mainColorschemeBox.setTop(new BHBHBox(new Label("Enter text: "), enterNicknameColorscheme));
        mainColorschemeBox.setCenter(nickPreview);
        mainColorschemeBox.setBottom(new BHBHBox(prompt, schemes));

        logStatic(Level.INFO, "ColorScheme init completed.", null);
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

        boolean bFlag = rootPane.getCenter().equals(mainBHBBox);

        rootPane.setCenter(bFlag ? mainColorschemeBox : mainBHBBox);
        stage.setTitle(bFlag ? "ColorScheme V2" : "Blazin's Hex Blender");
        saveItem.setDisable(bFlag);
        loadItem.setDisable(bFlag);
        slotMachineColorsItem.setDisable(bFlag);
        switchStagesItem.setText(bFlag ? "Switch to BHB" : "Switch to ColorScheme");
        logStatic(Level.INFO, "Switched to " + (bFlag ? "BHB" : "ColorScheme V2"), null);
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
                if(Boolean.TRUE.equals(renderOnHook.getValue())){
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
            if(codeFields.get(i).getText() != null && codeFields.get(i).getText().length() == 6 && !codeFields.get(i).isDisable() && i + 1 < 6 && isHexOk(codeFields.get(i).getText())) codeFields.get(i + 1).setDisable(false);
            else if (i + 1 < 6) codeFields.get(i + 1).setDisable(true);
        }
    }

    //Update font size based on how much text there is
    private static void updateTextFieldFontSize(LimitedTextField field){
        if(field.getText() != null && field.getText().length() > 0){
            int fontSize = (field.widthProperty().intValue()) / field.getText().length();
            if (fontSize > 12) fontSize = 14;
            field.setFont(new Font("Arial", fontSize));
        }    
    }

    //Update the preview with new text or new codes
    public void updatePreviewBHB(){

        int userInputLength = enterNicknameBHB.getText().length();

        //Reset hook handler
        renderOnHook.setValue(false);

        //Need to be at least 2 codes
        int validCodes = 0;
        for(LimitedTextField lt : codeFields) if(!lt.isDisable() && isHexOk(lt.getText())) validCodes++;
        if (validCodes >= 2 && (userInputLength >= ((validCodes * 2) - 1))){
            List<String> codeList = codeFields.stream().map(TextInputControl::getText).collect(Collectors.toList());

            currentNickBHB.setValue(Blend.blendMain(validCodes, enterNicknameBHB.getText(), codeList, justificationPriority.getValue().equals("Right")));
            parseNickToLabel(currentNickBHB, previewLabelsBHBBox, selectedScheme, true);
            return;        
        }

        previewLabelsBHBBox.getChildren().clear();
        previewLabelsBHBBox.setPrefHeight(0);
        currentNickBHB.setValue("");
    }

    //Colorscheme version of ^
    public void updatePreviewColorscheme(){

        currentNickColorScheme.setValue("");

        StringBuilder nickBuilder = new StringBuilder();

        int userInputLength = enterNicknameColorscheme.getText().length();

        //Gets the currently selected scheme from the dropdown menu
        selectedScheme = schemes.getValue();

        //Gets the codes that correspond to the scheme
        List<String> schemeCodes = selectedScheme.getScheme();

        int counter = 0;
        //Fills the codearray with the codes
        for(int i = 0; i < userInputLength; ++i, ++counter){
            //Next char to be added
            if(enterNicknameColorscheme.getText().charAt(i) == ' ') counter --;
            if(counter >= schemeCodes.size()) counter = 0;
            if(selectedScheme.toString().equals("Random Hex")){
                nickBuilder.append("&#" + schemeCodes.get(counter) + enterNicknameColorscheme.getText().charAt(i));
            }
            else{
                nickBuilder.append("&" + schemeCodes.get(counter) + enterNicknameColorscheme.getText().charAt(i));
            }         
        }
        currentNickColorScheme.setValue(nickBuilder.toString());

        parseNickToLabel(currentNickColorScheme, previewLabelsColorscheme, selectedScheme, false);
    }

    //Bool swapping between light & dark
    private void changeTheme(Scene mainScene, List<Label> labelColorPreviews){
        boolean isLight = currentTheme.getValue().equals("LIGHT");

        for(Button b : upButtons) b.setTextFill(isLight ? Color.WHITE : Color.BLACK);
        if(isLight) mainScene.getStylesheets().add(getClass().getClassLoader().getResource("dark.css").toString());
        else mainScene.getStylesheets().remove(getClass().getClassLoader().getResource("dark.css").toString());
        for(LimitedTextField lt : codeFields.stream().filter(lt -> lt.getText().equals("")).collect(Collectors.toList())){
            labelColorPreviews.get(codeFields.indexOf(lt)).setBackground(isLight ? DARK_BACKGROUND : DEF_BACKGROUND);
        }
        darkMode.setValue(isLight ? "On" : "Off");
        currentTheme.setValue(isLight ? "DARK" : "LIGHT");
        copyButtonBHB.setGraphic(new CopyButtonIcon(!isLight));
        copyButtonColorscheme.setGraphic(new CopyButtonIcon(!isLight));
    }

    //======================================================
    //|                 UI INTEGRATED HELPERS              |
    //======================================================

    private void doClipboardCopy(){
        StringSelection stringSelection = new StringSelection("");
        if(rootPane.getCenter().equals(mainBHBBox) && !currentNickBHB.getValue().equals("")) stringSelection = new StringSelection(currentNickBHB.getValue());
        else if (!currentNickColorScheme.getValue().equals("")) stringSelection = new StringSelection(currentNickColorScheme.getValue());
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(stringSelection, null);
    }

    //Undo changes in the order they were made
    private void undoChange(){
        //If the queue is empty, gtfo
        if(referencedFieldsQueue.isEmpty()) return;

        //If the last action was a clear all, undo all 6 clears, else, undo 1
        for(int i = 0; i < (Boolean.TRUE.equals(lastActionClearAll.getValue()) ? 6 : 1); i++){
            Integer codeFieldsID = referencedFieldsQueue.removeFirst();
            codeFields.get(codeFieldsID).setText(oldColorQueue.removeFirst());
            codeFields.get(codeFieldsID).requestFocus();
        }
        lastActionClearAll.setValue(false);
        updateUndoButton();
    }

    //Create preview label coundaries based on a formatted nick
    private static void parseNickToLabel(Property<String> nick, HBox previewLabels, Scheme selectedScheme, boolean isBhb){
        //Clear existing labels
        previewLabels.getChildren().clear();

        //Split nick into usable form for both hex and color codes
        List<String> comp = Arrays.asList(nick.getValue().replace("#", "").split("&")).stream().filter(s -> !s.equals("")).collect(Collectors.toList());

        boolean hexFlag = (isBhb || selectedScheme.toString().equals("Random Hex"));
        for(String s : comp) previewLabels.getChildren().add(new PreviewLabel(s.charAt(hexFlag ? 6 : 1), s.substring(0, (hexFlag ? 6 : 1)), comp.size() - 1));
    }
    //Allows for new generation mid program
    private void generateNewRandomScheme(){

        //Create random colors
        Random random = new Random(new Random().nextInt(Integer.MAX_VALUE));
        List<String> randomList = new ArrayList<>(32);
        for(int i = 0; i < 32; ++i) randomList.add(Character.toString("0123456789abcdef".charAt(random.nextInt(15))));

        //Create the scheme
        if(loadedSchemes.get(0) == null) loadedSchemes.set(0, new Scheme("Random", randomList));
        else loadedSchemes.get(0).setScheme(randomList);
    }

    //Allows for new generation mid program
    private void generateNewRandomHexScheme(){
        
        List<String> randomHexList = new ArrayList<>(32);
        for(int i = 0; i < 32; ++i) randomHexList.add(new RandomHexGenerator().generate());

        //Create the scheme
        if(loadedSchemes.get(1) == null) loadedSchemes.set(1, new Scheme("Random Hex", randomHexList));
        else loadedSchemes.get(1).setScheme(randomHexList);
    }

    //======================================================
    //|                 UPDATERS                           |
    //======================================================

    private boolean checkForUpdates(){
        return (compareVersions(CURRENT_VERSION.getValue(), LATEST_VERSION.getValue()) == -1);
    }

    private void startSelfUpdate(){

        //Create buttons for alert
        ButtonType no = new ButtonType("No", ButtonBar.ButtonData.OK_DONE);
        ButtonType yes = new ButtonType("Yes", ButtonBar.ButtonData.CANCEL_CLOSE);

        Optional<ButtonType> result = new BHBAlert(AlertType.CONFIRMATION, 
            "An updated version of BHB is available. Current: " + CURRENT_VERSION + ", New: " + LATEST_VERSION + ". Update now? This will restart BHB.",
            "Updates found", "Out of date", no, yes).showAndWait();

        if(result.orElse(no) == yes){
            Thread saveThread = new Thread(this::saveApplicationState);
            saveThread.start();

            try{ saveThread.join(); }
            catch (InterruptedException e){ 
                Thread.currentThread().interrupt();
                logStatic(Level.SEVERE, "Exception in pre-update save; Stacktrace: " + e.getStackTrace(), e);
            }
            alreadySaved.setValue(true);
            if(!(updater.update(System.getProperty("os.name")).equals(""))) logStatic(Level.SEVERE, "Error in updater thread.", null);
            System.exit(0);
        }
    }

    //======================================================
    //|                 MISCELLANEOUS HELPERS              |
    //======================================================

    //Static logger referencing
    private static void logStatic(Level logLevel, String logMessage, Throwable thrown){
        //Do not log if not error if verbose is off
        if(!Boolean.TRUE.equals(verboseLogging.getValue()) && logLevel.equals(Level.INFO)) return;
        //Do not log "thrown" with info, as it will be null
        if(logLevel.equals(Level.INFO)) Logger.getGlobal().log(logLevel, logMessage);
        else Logger.getGlobal().log(logLevel,  logMessage, thrown);
        loggedItems.setValue(loggedItems.getValue().intValue() + 1);
    }

    //Initalize the static logger accessed during runtime
    private void initLogger(){
        //Delete existing log file
        if(logFile.exists()) logFile.delete();
        try {  
            // This block configures the logger with handler and formatter  
            FileHandler fh = new FileHandler("log.txt"); 
            log.addHandler(fh);
            fh.setFormatter(new SimpleFormatter());
            logStatic(Level.INFO, "Logger init", null);
    
        } catch (SecurityException | IOException e) {
            //Usually would be logged, however log init failed here (! fatal)
            e.printStackTrace();  
        }
    }

    private static void killOldLinuxProcess(String[] args){
        logStatic(Level.INFO, "Old process found with pid" + args[0] +  ", killing...", null);
        ProcessBuilder processBuilder = new ProcessBuilder().command("nohup", "kill", "-9", args[0]);
        try {
            processBuilder.directory(new File(System.getProperty("user.dir")));
            processBuilder.redirectErrorStream(false);
            processBuilder.start();
            logStatic(Level.INFO, "Old process killed", null);
    
        } catch (IOException e) {
            logStatic(Level.SEVERE, "Exception in determining old process. Stacktrace: " + e.getStackTrace(), e);
        }
    }

    //Intrusive loading technique, will disrupt the program's functionality temporarily
    private void forceLoad(Scene mainScene, List<Label> labelColorPreviews){

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
            List<String> codes = new ArrayList<>(Arrays.asList("","","","","",""));
            //Read the codes
            for(int i = 0; i < 6; i++) if((line = bReader.readLine()) != null && isHexOk(line)) codes.add(i, line);
            for(int i = 0; i < 6; i++) if(codes.get(i) != null) codeFields.get(i).setText(codes.get(i));
            enterNicknameBHB.setText(bReader.readLine().replace("\n", ""));

            //Set theme based on config
            if(bReader.readLine().equals("DARK")) changeTheme(mainScene, labelColorPreviews);

            //Read saved justification priority and set it accordingly
            String savedJustification = bReader.readLine();
            if(savedJustification != null) justificationPriority.setValue(savedJustification);
            else justificationPriority.setValue("Left");

            //Read saved "delimitInputs" preference (default is "Off") - and execute
            String savedDelimit = bReader.readLine();
            if(savedDelimit != null) delimitInputs.setValue(savedDelimit);
            else delimitInputs.setValue("Off");
            delimitInputs.execute();
        }
        catch(Exception e){
            logStatic(Level.SEVERE, "Exception in forceLoad(); Stacktrace: " + e.getStackTrace(), e);
            logStatic(Level.INFO, "tempStore.txt deleted?: " + Boolean.toString(tempStore.delete()), null);
            return;
        }

        //Delete the file once loaded
        logStatic(Level.INFO, "tempStore.txt deleted?: " + Boolean.toString(tempStore.delete()), null);
    }

    //Intrusive saving technique, will disrupt the program's functionality temporarily
    private void saveApplicationState(){

        //Flag used during updates so that the JVM doesn't terminate and error out
        if(!Boolean.TRUE.equals(alreadySaved.getValue())){
            File tempStore = new File(System.getProperty("user.dir") + "/tempstore.txt");
            List<String> codes = new ArrayList<>();
            for(int i = 0; i < 6; i++) codes.add(i, codeFields.get(i).getText());
            try( FileWriter fWriter = new FileWriter(tempStore); //Basic reader, throws FileNotFoundException
            BufferedWriter bWriter = new BufferedWriter(fWriter);)
            {
                //Write the 6 code from boxes (including empty lines)
                for(int i = 0; i < 6; i++) bWriter.write(codes.get(i) + "\n");
                bWriter.write(enterNicknameBHB.getText() + "\n");
                bWriter.write(currentTheme.getValue() + "\n");
                bWriter.write(justificationPriority.getValue() + "\n");
                bWriter.write(delimitInputs.getValue());

                //Hides the file from the user - only works on Windows
                String osName = System.getProperty("os.name");
                if(osName.contains("Windows")){
                    Runtime.getRuntime().exec("attrib +H \"" +  System.getProperty("user.dir") + "/tempstore.txt\"" );
                }
            }
            catch(IOException | StringIndexOutOfBoundsException e){
                new BHBAlert(AlertType.ERROR, "There was a problem saving the config.", "", "Error: " + e.getLocalizedMessage()).showAndWait();
                logStatic(Level.SEVERE, "Exception in forceSave(); Stacktrace: " + e.getStackTrace(), e);
            }
        }
    }

    //Parses JSON from a url
    private static String getJSON(String url) {
        try{
            return(new JSONObject(IOUtils.toString(new URL(url), StandardCharsets.UTF_8)).toString());
        }
        catch(IOException ex){
            new BHBAlert(AlertType.ERROR, "An unexpected error occurred.", "", "Error: " + ex.getLocalizedMessage()).showAndWait();
            logStatic(Level.SEVERE, "IOException during getJSON(); Stacktrace: " + ex.getStackTrace(), ex);
            return "";
        }
    }

    //Pulls one tag from a json string
    private static String getTagFromGitJson(String tagName){
        return(JsonParser.parseString(getJSON("https://api.github.com/repos/DavidArthurCole/bhb/releases/latest")).getAsJsonObject().get(tagName).getAsString());
    }

    //Compare two version numbers - returns -1 if v1 < v2
    private int compareVersions(String v1, String v2) {
        List<String> components1 = Arrays.asList(v1.split("\\."));
        List<String> components2 = Arrays.asList(v2.split("\\."));
        int length = Math.min(components1.size(), components2.size());
        for(int i = 0; i < length; i++) {
            int result = Integer.compare(Integer.parseInt(components1.get(i)), Integer.parseInt(components2.get(i)));
            if(result != 0) return result;
        }
        return Integer.compare(components1.size(), components2.size());
    }

    //Reload schemes into combobox from the array
    private void reloadSchemes(){
        for(Scheme s : loadedSchemes) schemes.getItems().add(loadedSchemes.indexOf(s), s);
    }

    //Initialize schemes for startup
    private void initSchemes(){

        loadedSchemes = new ArrayList<>(
            Arrays.asList(
                null, //Reserved for random
                null, //Reserved for random hex
                new Scheme("Rainbow", Arrays.asList("4c6eab9d5".split(""))), //Rainbow
                new Scheme("Ordered", Arrays.asList("0123456789abcdef".split(""))) //Ordered
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