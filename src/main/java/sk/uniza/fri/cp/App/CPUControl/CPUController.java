package sk.uniza.fri.cp.App.CPUControl;

import javafx.animation.Animation;
import javafx.animation.Transition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.*;
import javafx.concurrent.WorkerStateEvent;
import javafx.css.PseudoClass;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;

import java.io.*;
import java.net.URL;
import java.nio.file.Paths;
import java.util.*;

//CodeArea zvyraznovanie slov
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;
import javafx.util.Duration;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.*;

import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;

import sk.uniza.fri.cp.App.CPUControl.CodeEditor.CodeEditorFactory;
import sk.uniza.fri.cp.App.CPUControl.CodeEditor.RichTextFXHelpers;
import sk.uniza.fri.cp.App.io.ConsoleOutputStream;
import sk.uniza.fri.cp.App.io.ConsolePrintWriter;
import sk.uniza.fri.cp.Bus.Bus;
import sk.uniza.fri.cp.CPUEmul.CPU;
import sk.uniza.fri.cp.CPUEmul.CPUStates;
import sk.uniza.fri.cp.CPUEmul.Exceptions.InvalidCodeLinesException;
import sk.uniza.fri.cp.CPUEmul.Exceptions.NonExistingInterruptLabelException;
import sk.uniza.fri.cp.CPUEmul.Parser;
import sk.uniza.fri.cp.CPUEmul.Program;

import static sk.uniza.fri.cp.App.CPUControl.DataRepresentation.getDisplayRepresentation;


/**
 * updateuje stav pri zmene stavu CPU (ChangeListener na Message Task-u)
 *
 * @author Moris
 * @version 1.0
 * @created 07-feb-2017 18:40:27
 */
public class CPUController implements Initializable {

        //uchovanie aktualneho zobrazenia registrov a pamati
	private DataRepresentation.eRepresentation displayFormRegisters;
	private DataRepresentation.eRepresentation displayFormStackAddr;
	private DataRepresentation.eRepresentation displayFormStackData;
	private DataRepresentation.eRepresentation displayFormProgMemoryAddr;
	private DataRepresentation.eRepresentation displayFormProgMemoryData;
    private DataRepresentation.eRepresentation displayFormRAMAddr;
    private DataRepresentation.eRepresentation displayFormRAMData;

    private CPU cpu;
    volatile private Program program;
    volatile private boolean f_async;
    volatile private boolean f_intBylevel;
    volatile private boolean f_microstep;

    private boolean f_code_parsed; //indikator, ci je zavedeny aktualny program
    private boolean f_in_execution; //indikator, ci sa program vykonava
    private boolean f_paused;    //indikator, ci je prebiehajuci program pozastaveny
    private SimpleIntegerProperty execution_line;

    //Breakpointy
    private TreeSet<Integer> breakpointLines;
    private ObservableSet<Integer> observableBreakpointLines;

    //Priznaky vykonavania programu


    //Menu
    //Nastavenia
    @FXML private CheckMenuItem chmiSettingsSync;
    @FXML private CheckMenuItem chmiSettingsAsync;
    @FXML private CheckMenuItem chmiSettingsINTLevel;
    @FXML private CheckMenuItem chmiSettingsINTChange;
    @FXML private CheckMenuItem chmiSettingsMicrostep;

    //Editor kodu
    @FXML private TitledPane titPaneCode;
    @FXML private StackPane codeAreaPane;
    @FXML private Button btnLoadCode;
    @FXML private Button btnSaveCode;
    private final String STYLE_PARAGRAPH_ERROR = "paragraph-error";
    final private String CODE_PANE_TEXT = "Kód";
    private CodeArea codeEditor;
    private File currentFile;
    private boolean fileSaved;  //true ak je aktualny subor bezpecne ulozeny

    //konzola
    @FXML private StackPane consolePane;
    private InlineCssTextArea console;
    private ConsolePrintWriter cpw_error;

	//vyber zobrazenia registrov / pamati
	@FXML private ToggleGroup btnGroupRegisters;
	@FXML private ToggleGroup btnGroupStackAddr;    //zasobnik
	@FXML private ToggleGroup btnGroupStackData;
	@FXML private ToggleGroup btnGroupProgMemoryAddr;   //konstanty v programe
	@FXML private ToggleGroup btnGroupProgMemoryData;
    @FXML private ToggleGroup btnGroupRAMAddr;  //vnutorna RAM pamat
    @FXML private ToggleGroup btnGroupRAMData;

	//tlacidla
	@FXML private Button btnParse;
	@FXML private Button btnStart;
	@FXML private Button btnStep;
	@FXML private Button btnPause;
	@FXML private Button btnReset;
	@FXML private Button btnStop;
	final private String BTN_TXT_START = "Spusti";
    final private String BTN_TXT_CONTINUE = "Pokračovať";

	//registre
	@FXML private TextField tfRegA;
	@FXML private TextField tfRegB;
	@FXML private TextField tfRegC;
	@FXML private TextField tfRegD;
	@FXML private TextField tfRegPC;
	@FXML private TextField tfRegSP;
	@FXML private TextField tfRegMP;
	@FXML private TextField tfFlagCY;
	@FXML private TextField tfFlagZ;
	@FXML private TextField tfFlagIE;

	//tabulky
    private final PseudoClass pcDataChanged = PseudoClass.getPseudoClass("data-changed");
    //--zasobnik
    @FXML private TableView<StackTableCell> tableViewStack;
    @FXML private TableColumn<StackTableCell, Integer> tableColumnStackAddr;
    @FXML private TableColumn<StackTableCell, Integer> tableColumnStackData;
    private ObservableList<StackTableCell> tableViewStackItems; //List, v ktorom su ulozene itemy tabulky
    private final PseudoClass pcStackHead = PseudoClass.getPseudoClass("stack-head");
    //--konstanty - pamat programu
    @FXML private TableView<MemoryTableCell> tableViewProgMemory;
    @FXML private TableColumn<MemoryTableCell, Integer> tableColumnProgMemoryAddr;
    @FXML private TableColumn<MemoryTableCell, Integer> tableColumnProgMemoryData;
    private ObservableList<MemoryTableCell> tableViewProgMemoryItems; //List, v ktorom su ulozene itemy tabulky
    //--RAM
    @FXML private TableView<MemoryTableCell> tableViewRAM;
    @FXML private TableColumn<MemoryTableCell, Integer> tableColumnRAMAddr;
    @FXML private TableColumn<MemoryTableCell, Integer> tableColumnRAMData;
    private ObservableList<MemoryTableCell> tableViewRAMItems; //List, v ktorom su ulozene itemy tabulky

    //Stavovy riadok
	@FXML private ProgressBar progressBar;
	@FXML private Label lbStatus;

	@FXML private SplitPane splitPaneHoriz;
	@FXML private TitledPane titPaneRegisters;
	@FXML private AnchorPane anchPaneRegisters;
    @FXML private TitledPane titPaneConsole;
    @FXML private SplitPane splitPaneVert;

    private double lastConsoleDividerPos;

	/**
	 * Spustene pri inicializacii okna
	 */
	public void initialize(URL location, ResourceBundle resources) {
	    //inizializacia atributov
        f_intBylevel = true;
        execution_line = new SimpleIntegerProperty(-1);
        //TODO pri odstraneni sample kody odpoznamkovat fileSaved
        //fileSaved = true; //aj cisty kod je kvazi ulozeny

        initializeGUITables();


        //Inicializacia konzoly
        console = new InlineCssTextArea();
        console.setEditable(false);
        consolePane.getChildren().add(new VirtualizedScrollPane<>(console));


        //ERROR STREAM
        cpw_error = new ConsolePrintWriter(new ConsoleOutputStream(console, "red"));

        //struktura pre breakpointy
        breakpointLines = new TreeSet<>();
        observableBreakpointLines = FXCollections.synchronizedObservableSet( FXCollections.observableSet(breakpointLines) );

		//Inicializacia editora kodu
        codeEditor = CodeEditorFactory.getCodeEditor(codeAreaPane, observableBreakpointLines);
        codeEditor.richChanges()
                .filter(ch -> !ch.getInserted().getText().equals(ch.getRemoved().getText()))
                .subscribe(change -> {
                    f_code_parsed = false; //zmena v kode -> program nie je aktualny
                    cpu = null;
                    btnStart.setText(BTN_TXT_START);

                    if(fileSaved) { //ozacenie v liste, ze kod nie je ulozeny
                        titPaneCode.setText(titPaneCode.getText() + "*");
                        fileSaved = false;
                    }
                });

		//Listenery na zmenu zobrazenia registrov a pamati
		btnGroupRegisters.selectedToggleProperty().addListener( (observable, oldValue, newValue)->{
            displayFormRegisters = onToggleGroupChange(btnGroupRegisters, oldValue, newValue);
            updateGUIRegisters();});
		btnGroupStackAddr.selectedToggleProperty().addListener( (observable, oldValue, newValue)->{
            displayFormStackAddr = onToggleGroupChange(btnGroupStackAddr, oldValue, newValue);
            updateGUITableForm(tableViewStackItems, displayFormStackAddr, displayFormStackData);});
		btnGroupStackData.selectedToggleProperty().addListener( (observable, oldValue, newValue)->{
            displayFormStackData = onToggleGroupChange(btnGroupStackData, oldValue, newValue);
            updateGUITableForm(tableViewStackItems, displayFormStackAddr, displayFormStackData);});
		btnGroupProgMemoryAddr.selectedToggleProperty().addListener( (observable, oldValue, newValue)->{
            displayFormProgMemoryAddr = onToggleGroupChange(btnGroupProgMemoryAddr, oldValue, newValue);
            updateGUITableForm(tableViewProgMemoryItems, displayFormProgMemoryAddr, displayFormProgMemoryData);});
		btnGroupProgMemoryData.selectedToggleProperty().addListener( (observable, oldValue, newValue)->{
            displayFormProgMemoryData = onToggleGroupChange(btnGroupProgMemoryData, oldValue, newValue);
            updateGUITableForm(tableViewProgMemoryItems, displayFormProgMemoryAddr, displayFormProgMemoryData);});
        btnGroupRAMAddr.selectedToggleProperty().addListener( (observable, oldValue, newValue)->{
            displayFormRAMAddr = onToggleGroupChange(btnGroupRAMAddr, oldValue, newValue);
            updateGUITableForm(tableViewRAMItems, displayFormRAMAddr, displayFormRAMData);});
        btnGroupRAMData.selectedToggleProperty().addListener( (observable, oldValue, newValue)->{
            displayFormRAMData = onToggleGroupChange(btnGroupRAMData, oldValue, newValue);
            updateGUITableForm(tableViewRAMItems, displayFormRAMAddr, displayFormRAMData);});

		//inicializacia stavov zobrazenia registrov a pamati na decimalne zobrazenie
		displayFormRegisters = DataRepresentation.eRepresentation.Dec;
		displayFormStackAddr = DataRepresentation.eRepresentation.Dec;
		displayFormStackData = DataRepresentation.eRepresentation.Dec;
		displayFormProgMemoryAddr = DataRepresentation.eRepresentation.Dec;
		displayFormProgMemoryData = DataRepresentation.eRepresentation.Dec;
        displayFormRAMAddr = DataRepresentation.eRepresentation.Dec;
        displayFormRAMData = DataRepresentation.eRepresentation.Dec;

        //inicializacia tlacitok
        btnParse.setDisable(false);
        btnStart.setDisable(false);
        btnStep.setDisable(true);
        btnPause.setDisable(true);
        btnReset.setDisable(true);
        btnStop.setDisable(true);

        //inicializacia pozicie oddelovacov v horizontalnom SplitPaneli
        defSplitPaneHorizDividerPositions = splitPaneHoriz.getDividerPositions();

        splitPaneVert.getDividers().get(0).setPosition(0.71);
        titPaneConsole.expandedProperty().addListener(new ChangeListener<Boolean>() {
            @Override
            public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
                if(newValue)    //ak je otvoreny
                    splitPaneVert.getDividers().get(0).setPosition(lastConsoleDividerPos);
                else {    //ak je zatvoreny
                    lastConsoleDividerPos = splitPaneVert.getDividers().get(0).getPosition();
                    splitPaneVert.getDividers().get(0).setPosition(1);
                }
            }
        });

		//SANDBOX
		writeConsoleLn("Ahojky");
		writeConsoleLn("bauky", "red");
		writeConsoleLn("manuky", "blue");
		writeConsoleLn("manuky");

		String sampleCode = "mvi a,20\n" +
                "mvi b,5\n" +
                "\n" +
                "start:\n" +
                "\tsub a,b\n" +
                "\tpus a\n" +
                "\tjnz start";
		codeEditor.replaceText(0, 0, sampleCode);
		fileSaved = true;
    }

    public void keyboardInput(KeyEvent event){
	    if(cpu != null)
            cpu.setKeyPressed(event);
    }

    public boolean isExecuting(){
        return f_in_execution;
    }


    /**
     * Inicializacia tabuliek pre pamate - Zasobnik , Konstanty programu, RAM
     */
    private void initializeGUITables(){
        //inicializacia tabulky zasobnika
        tableViewStackItems = FXCollections.observableArrayList();      //vytvorenie ArrayListu pre ukladanie itemov
        tableViewStackItems.add(new StackTableCell(0,0)); //vytvorenie nultej adresy
        for (int i = 65535; i > 0; i--)                                 //vytvorenie zvyskych adries zasobnika
            tableViewStackItems.add(new StackTableCell(i,0));
        tableViewStack.setItems(tableViewStackItems);                   //priradenie pola do tabulky

        //faktorka na riadky - pridavanie pseudotried pre vrchol zasobnika a zmenene data
        tableViewStack.setRowFactory(tv ->
            new TableRow<StackTableCell>(){
                @Override
                protected void updateItem(StackTableCell item, boolean empty) {
                    super.updateItem(item, empty);

                    pseudoClassStateChanged(pcStackHead, (!empty) && item.getAddressInt() == StackTableCell.getStackHead());
                    pseudoClassStateChanged(pcDataChanged, (!empty) && item.isChanged());

                }
            }
        );

        //priradenie atributov polozky k stlpcom
        tableColumnStackAddr.setCellValueFactory( new PropertyValueFactory<>("address"));
        tableColumnStackData.setCellValueFactory( new PropertyValueFactory<>("data"));


        //inicializacia tabulky konstant
        tableViewProgMemoryItems = FXCollections.observableArrayList();         //vytvorenie ArrayListu pre ukladanie itemov
        for (int i = 0; i <= 256; i++)                                          //vytvorenie zvyskych adries zasobnika
            tableViewProgMemoryItems.add(new MemoryTableCell(i,0));
        tableViewProgMemory.setItems(tableViewProgMemoryItems);                 //priradenie pola do tabulky

        //priradenie atributov polozky k stlpcom
        tableColumnProgMemoryAddr.setCellValueFactory( new PropertyValueFactory<>("address"));
        tableColumnProgMemoryData.setCellValueFactory( new PropertyValueFactory<>("data"));


        //inicializacia tabulky RAM
        tableViewRAMItems = FXCollections.observableArrayList();         //vytvorenie ArrayListu pre ukladanie itemov
        for (int i = 0; i <= 256; i++)                                          //vytvorenie zvyskych adries zasobnika
            tableViewRAMItems.add(new MemoryTableCell(i,0));
        tableViewRAM.setItems(tableViewRAMItems);                               //priradenie pola do tabulky

        //faktorka na riadky - pridavanie pseudotried pre vrchol zasobnika a zmenene data
        tableViewRAM.setRowFactory(tv ->
            new TableRow<MemoryTableCell>(){
                @Override
                protected void updateItem(MemoryTableCell item, boolean empty) {
                    super.updateItem(item, empty);
                    pseudoClassStateChanged(pcDataChanged, (!empty) && item.isChanged());
                }
            }
        );

        //priradenie atributov polozky k stlpcom
        tableColumnRAMAddr.setCellValueFactory( new PropertyValueFactory<>("address"));
        tableColumnRAMData.setCellValueFactory( new PropertyValueFactory<>("data"));
    }

    /**
     * Zmaze dany styl zo vsetkych paragrafov v editore
     * @param style Styl, ktory sa ma zmazat
     */
	public void clearCodeStyle(String style){
        for (int i = 0; i < codeEditor.getParagraphs().size(); i++){
            if(codeEditor.getParagraph(i)
                    .getParagraphStyle()
                    .stream()
                    .anyMatch( s -> s.contains(style))){
                RichTextFXHelpers.tryRemoveParagraphStyle(codeEditor, i, style);
            }
        }
    }

    public void clearCodeHeightligting(int lineIndex){
        codeEditor.clearParagraphStyle(lineIndex);
    }



	/**
	 * Zapis textu na konzolu
	 * @param text Text, ktory sa ma vypisat na konzolu
	 */
	public void writeConsole(String text){
		console.appendText(text);
	}

	/**
	 * Zapis riadku textu na konzolu
	 * @param text Text, ktory sa ma vypisat na konzolu
	 */
	public void writeConsoleLn(String text){
		writeConsoleLn(text, "black");
	}

	/**
	 * Zapis farebneho riadku na konzolu
	 * @param text Text, ktory sa ma vypisat na konzolu
	 * @param color Farba textu
	 */
	public void writeConsoleLn(String text, String color){
		int paragraph = console.getCurrentParagraph();
		console.setStyle(paragraph, "-fx-fill: " + color);
		console.appendText(text + "\n");
		console.moveTo(console.getText().length());
		console.requestFollowCaret();


	}

	/**
	 * HANDLERS
	 */

	//Zobrazovanie / skryvanie bocnych panelov
    private double[] defSplitPaneHorizDividerPositions;

	@FXML
    private void handleToggleRegistersPaneAction(){
        toggleSplitPaneDivider(true, 0, 0);
    }

    @FXML
    private void handleToggleMemoryPaneAction(){
        toggleSplitPaneDivider(false, 1, 1);
    }

    private void toggleSplitPaneDivider(boolean decrementToHide, int dividerIndex, double hidedAt){
        // create an animation to hide sidebar.
        final Animation hideSidebar = new Transition() {
            { setCycleDuration(Duration.millis(250)); }
            protected void interpolate(double frac) {
                double perc = splitPaneHoriz.getDividers().get(dividerIndex).getPosition();
                if(decrementToHide)
                    splitPaneHoriz.setDividerPosition(dividerIndex, (perc - frac * perc));
                else
                    splitPaneHoriz.setDividerPosition(dividerIndex, perc + frac * (1-perc));
            }
        };

        // create an animation to show a sidebar.
        final Animation showSidebar = new Transition() {
            { setCycleDuration(Duration.millis(250)); }
            protected void interpolate(double frac) {
                if(decrementToHide)
                    splitPaneHoriz.setDividerPosition(dividerIndex, frac * defSplitPaneHorizDividerPositions[dividerIndex]);
                else
                    splitPaneHoriz.setDividerPosition(dividerIndex, 1 - frac * defSplitPaneHorizDividerPositions[dividerIndex]);
            }
        };

        if (showSidebar.statusProperty().get() == Animation.Status.STOPPED && hideSidebar.statusProperty().get() == Animation.Status.STOPPED) {
            if ( Math.abs(hidedAt - splitPaneHoriz.getDividers().get(dividerIndex).getPosition()) > 0.02) {
                hideSidebar.play();
            } else {
                showSidebar.play();
            }
        }
    }

    /**
     * Menu handlers
     */

    @FXML
    private void handleMenuSettingSyncAction(){
        onChangeAsync(false);
    }

    @FXML
    private void handleMenuSettingAsyncAction(){
        onChangeAsync(true);
    }

    private void onChangeAsync(boolean newVal){
        f_async = newVal;
        if(cpu != null)
            cpu.setAsync(f_async);

        chmiSettingsSync.setSelected(!newVal);
        chmiSettingsAsync.setSelected(newVal);
    }

    @FXML
    private void handleMenuSettingsINTLevelAction(){
        onChangeIntByLevel(true);
    }

    @FXML
    private void handleMenuSettingsINTChangeAction(){
        onChangeIntByLevel(false);
    }

    private void onChangeIntByLevel(boolean newVal){
        f_intBylevel = newVal;
        if(cpu != null)
            cpu.setIntLevel(f_intBylevel);

        chmiSettingsINTLevel.setSelected(newVal);
        chmiSettingsINTChange.setSelected(!newVal);
    }

    @FXML
    private void handleMenuSettingsMicrostepAction(){
        boolean isSelected = chmiSettingsMicrostep.isSelected();
        f_microstep = isSelected;
        if (cpu != null)
            cpu.setMicrostep(isSelected);

        chmiSettingsMicrostep.setSelected(isSelected);
    }


	/**
	 * nacita text studenta (program) do suboru
	 */

    @FXML
    private void handleMenuFileNewAction(){
        if(!fileSaved && !continueIfUnsavedFile()) return;

        codeEditor.clear();
        titPaneCode.setText(CODE_PANE_TEXT);
        fileSaved = true;
    }

    /**
     * Funkcia sa spýta užívateľa na ďalší postup, ak aktuálny súbor nie je uložený.
     *
     * @return true - volajúca procedúra môže pokračovať, false - užívateľ nechce pokračovať
     */
    private boolean continueIfUnsavedFile(){
        if(codeEditor.getText().trim().isEmpty()) return true;

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Potvrdenie");
        alert.setHeaderText("Zmeny vo vašom kóde neboli uložené");
        alert.setContentText("Naozaj chcete zahodiť zmeny vo vašom kóde?");

        ButtonType btnTypeYes = new ButtonType("Áno");
        ButtonType btnTypeCancel = new ButtonType("Zrušiť");
        ButtonType btnTypeSave = new ButtonType("Uložiť");
        ButtonType btnTypeSaveAs = new ButtonType("Uložiť ako");

        alert.getButtonTypes().clear();
        alert.getButtonTypes().add(btnTypeYes);
        if(currentFile != null) alert.getButtonTypes().add(btnTypeSave);
        alert.getButtonTypes().addAll(btnTypeSaveAs, btnTypeCancel);

        Optional<ButtonType> result = alert.showAndWait();

        if(result.get() == btnTypeCancel){
            return false;
        } else if(result.get() == btnTypeSaveAs){
            return saveCode(true);
        } else  if(result.get() == btnTypeSave){
            return saveCode(false);
        }

        return true;
    }

	@FXML
	private void handleMenuFileOpenAction(){
        if(!fileSaved && !continueIfUnsavedFile()) return;

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Otvoriť súbor...");
        chooser.setInitialDirectory(new File(Paths.get("").toAbsolutePath().toString()));
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("ASM","*.asm"),
                new FileChooser.ExtensionFilter("TXT", "*.txt"));

        File file = chooser.showOpenDialog(btnLoadCode.getScene().getWindow());

        if(file != null)
            try(BufferedReader br = new BufferedReader(new FileReader(file))){
                codeEditor.clear();
                br.lines().forEach(line -> {
                    codeEditor.appendText(line + "\n");
                });
                currentFile = file;
                titPaneCode.setText("Kód - " + currentFile.getName());
                fileSaved = true;
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
    }


	@FXML
	private void handleMenuFileSaveAction(){
	    saveCode(false);
	}

	@FXML
    private void handleMenuFileSaveAsAction(){
        saveCode(true);
    }

    @FXML
    private void handleMenuFileExitAction(){
        exit();
    }

    public boolean exit(){
        if(!fileSaved && !continueIfUnsavedFile()) return false;
        Platform.exit();
        return true;
    }


	private boolean saveCode(boolean saveAs){
        File file = saveAs?null:currentFile;

        if(currentFile == null || saveAs) {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Uložiť" + (saveAs?" ako":"") + "..");
            chooser.setInitialDirectory(new File(Paths.get("").toAbsolutePath().toString()));
            chooser.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("ASM", "*.asm"),
                    new FileChooser.ExtensionFilter("TXT", "*.txt"));

            if(currentFile != null)
                chooser.setInitialFileName(currentFile.getName());

            file = chooser.showSaveDialog(btnSaveCode.getScene().getWindow());
        }

        if(file != null) {
            try (PrintWriter out = new PrintWriter(file)) {
                out.print(codeEditor.getDocument().getText());
                currentFile = file;
                titPaneCode.setText("Kód - " + currentFile.getName());
                fileSaved = true;

                return true;
            } catch (FileNotFoundException e) {
                return false;
            }
        } else {
            return false;
        }
    }

	@FXML
	private void handleButtonUnbreakAllAction(){
	    observableBreakpointLines.clear();
	}


	/**
	 * zoberie program studenta a pokusi sa ho parsovat na instrukcie... zaroven
	 * zablokuje moznost parsovania az pokial sa nezmeni cast studentovho kodu
	 */
	@FXML
	private void handleButtonParseAction(){
        parseCode(false);
	}

	@FXML
	private void handleButtonPauseAction(){
        if(cpu == null) return;
        cpu.pause();
	}

	@FXML
	private void handleButtonResetAction(){
	    if(cpu != null)
            cpu.reset();

        updateGUI();
        updateGUI();
	}

	@FXML
	private void handleButtonStartAction(){
	    if(cpu == null || !f_in_execution){
	        //nie je CPU ktore by nieco vykonavalo
            if(!f_code_parsed) {
                //este nie je zavedeny kod
                parseCode(true);
            } else {
                //kod uz je zavedeny, staci spustit CPU
                startExecution();
            }
        } else if(f_paused){
	        //ak je iba pozastavene vykonavanie
            cpu.continueExecute();
        }
	}

	@FXML
	private void handleButtonStepAction(){
        if(cpu == null) return;
            cpu.step();
	}

	@FXML
	private void handleButtonStopAction(){
        if(cpu == null) return;
            cpu.cancel();
	}

	@FXML
    private void handleButtonDevInt01Action(ActionEvent ae){
        /*Bus.getBus().setDataBus((byte)1);
        Bus.getBus().setIT(true);
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Bus.getBus().setIT(false);*/
    }

    @FXML
    private void handleButtonDevInt01ActionPressed(){
        Bus.getBus().setDataBus((byte)1);
        Bus.getBus().setIT(true);
    }

    @FXML
    private void handleButtonDevInt01ActionReleased(){
        Bus.getBus().setIT(false);
        //Bus.getBus().setRandomData();
        updateGUI();
    }

    /**
     * Funkcia vracia reprezentaciu vybraneho zobrazenia informacii v danej skupine prepinacich tlacidiel.
     * Pri odznaceni tlacidla ho opat zaznaci spat.
     *
     * @param btnGroup Skupina prepinacich tlacidiel, na ktorej bola vykonana zmena
     * @param oldValue Posledne aktivovane tlacidlo
     * @param newValue Nove aktivovane tlacidlo
     * @return Reprezentacia vybraneho zobrazenia D/H/B/A
     */
    private DataRepresentation.eRepresentation onToggleGroupChange(ToggleGroup btnGroup, Toggle oldValue, Toggle newValue){
        ToggleButton tb;
        if(newValue != null) {
            tb = (ToggleButton) newValue;
        } else {
            btnGroup.selectToggle(oldValue);
            tb = (ToggleButton) oldValue;
        }
        try {
            return DataRepresentation.eRepresentation.valueOf(tb.getText());
        } catch (IllegalArgumentException e){
            return DataRepresentation.eRepresentation.Hex;
        }
    }


    /**
     * obsluha konzoly
     */
    @FXML
    private void handleButtonConsoleClearAction(){
        console.clear();
    }

    @FXML
    private void handleButtonToStackHeadAction(){
        if(cpu != null) {
            int SP = Short.toUnsignedInt(cpu.getRegSP());
            if (SP == 0)
                tableViewStack.scrollTo(0);
            else
                tableViewStack.scrollTo(65535 - SP -5);
        }
        else
            tableViewStack.scrollTo(0);
    }



    private void parseCode(boolean startExecution){
        //pocet riadkov v editore pre update progress baru
        int lines = codeEditor.getParagraphs().size();
        //vytvorenie parsera
        Parser parserTask = new Parser(codeEditor.getText(), lines);

        //vycistenie editoru ak boli chyby
        clearCodeStyle(STYLE_PARAGRAPH_ERROR);

        //zobrazenie stavu
        progressBar.progressProperty().unbind();
        progressBar.progressProperty().bind(parserTask.progressProperty());
        lbStatus.setText("Parsujem...");

        Thread parserThread = new Thread(parserTask);
        //zastavenie vlakna ak skonci aplikacia
        parserThread.setDaemon(true);

        parserTask.setOnSucceeded(new EventHandler<WorkerStateEvent>() {
            @Override
            public void handle(WorkerStateEvent event) {
                writeConsoleLn("Program úspešne zavedený!", "green");
                program = parserTask.getValue();
                program.setListenerOnBreakpointsChange(observableBreakpointLines);

                f_code_parsed = true; //kod je uspesne prevedeny na program a moze byt vykonany
                lbStatus.setText("Program zavedený");

                //ak ma byt spustene vykonavanie CPU
                if(startExecution)
                    startExecution();
            }
        });

        parserTask.setOnFailed(new EventHandler<WorkerStateEvent>() {
            @Override
            public void handle(WorkerStateEvent event) {
                InvalidCodeLinesException ex = (InvalidCodeLinesException) parserTask.getException();

                //zvyraznenie riadkov s chybovym kodom
                for (int line :
                        ex.getLines()) {
                    RichTextFXHelpers.addParagraphStyle(codeEditor, line, STYLE_PARAGRAPH_ERROR);
                }

                //vypisanie chybnych riadkov s popoisom na konzolu
                for (String msg :
                        ex.getErrors()) {
                    writeConsoleLn("[Parser] " + msg, "red");
                }

                //posun konzoly na koniec
                console.setEstimatedScrollY(console.getParagraphs().size() * 50);

                f_code_parsed = false;
                //zobrazenie stavu
                lbStatus.setText("Chyba v kóde");
            }
        });

        //spustenie vlakna pre parsovanie kodu
        parserThread.start();
    }

    private void startExecution(){
        //vytvorenie CPU vlakna a spustenie vykonavania
        cpu = new CPU(program, new ConsoleOutputStream(console), f_async, f_intBylevel, f_microstep);
        Thread cpuThread = new Thread(cpu);
        cpuThread.setDaemon(true);


        //po uspesnom vykonani programu
        cpu.setOnSucceeded(new EventHandler<WorkerStateEvent>() {
            @Override
            public void handle(WorkerStateEvent event) {
                onCPUDone("Program ukončený");
            }
        });

        //po zastaveni vykonavania programu
        cpu.setOnCancelled(new EventHandler<WorkerStateEvent>() {
            @Override
            public void handle(WorkerStateEvent event) {
                onCPUDone("Program zrušený");
            }
        });

        cpu.setOnFailed(new EventHandler<WorkerStateEvent>() {
            @Override
            public void handle(WorkerStateEvent event) {
                Throwable ex = cpu.getException();

                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Chyba");
                alert.setContentText(ex.getMessage());

                if(ex instanceof NonExistingInterruptLabelException){
                    alert.setHeaderText("Chyba prerušenia");
                } else {
                    alert.setHeaderText("Nastala chyba pri vykonávaní programu");
                    ex.printStackTrace();
                }

                alert.showAndWait();

                onCPUDone("Program ukončený s chybou");
            }
        });

        cpu.statesProperty().addListener(new ChangeListener<CPUStates>() {
            @Override
            public void changed(ObservableValue<? extends CPUStates> observable, CPUStates oldValue, CPUStates newValue) {
                onCPUStateChanged(newValue);
            }
        });

        cpuThread.start();
    }

    private void onCPUStateChanged(CPUStates state){
        if(!Platform.isFxApplicationThread())
            Platform.runLater(()->{
                switch (state){
                    case Running:
                        onCPURunning();
                        break;
                    case Paused:
                        onCPUPaused(false);
                        break;
                    case MicroStep:
                        onCPUPaused(true);
                        break;
                    case Waiting:
                        onCPUWaiting();
                }
            });
    }

    private void onCPURunning(){
        f_paused = false;
        f_in_execution = true;

        updateExecutionLine( -1 );

        progressBar.progressProperty().unbind();
        progressBar.setProgress(-1);
        lbStatus.setText("Program sa vykonáva...");

        btnParse.setDisable(true);
        btnStart.setDisable(true);
        btnStep.setDisable(true);
        btnPause.setDisable(false);
        btnReset.setDisable(true);
        btnStop.setDisable(false);

        codeEditor.setEditable(false);
    }

    private void onCPUPaused(boolean isMicrostep){
        f_paused = true;

        updateExecutionLine( program.getLineOfInstruction(cpu.getRegPC()-1) );

        progressBar.progressProperty().unbind();
        progressBar.setProgress(0.5);
        if(isMicrostep)
            lbStatus.setText(cpu.getMessage());
        else
            lbStatus.setText("Program pozastavnený");

        btnStart.setText(BTN_TXT_CONTINUE);

        btnParse.setDisable(true);
        btnStart.setDisable(false);
        btnStep.setDisable(false);
        btnPause.setDisable(true);
        btnReset.setDisable(true);
        btnStop.setDisable(false);

        updateGUI();
    }

    private void onCPUWaiting(){
        f_paused = true;

        updateExecutionLine( program.getLineOfInstruction(cpu.getRegPC()-1) );

        progressBar.progressProperty().unbind();
        progressBar.setProgress(0.5);

        lbStatus.setText(cpu.getMessage());

        btnParse.setDisable(true);
        btnStart.setDisable(true);
        btnStep.setDisable(true);
        btnPause.setDisable(true);
        btnReset.setDisable(true);
        btnStop.setDisable(false);

    }

    private void onCPUDone(String statusText){
        f_in_execution = false;
        f_paused = false;

        updateExecutionLine( -1 );

        progressBar.progressProperty().unbind();
        progressBar.setProgress(1);
        lbStatus.setText(statusText);

        btnStart.setText(BTN_TXT_START);

        btnParse.setDisable(false);
        btnStart.setDisable(false);
        btnStep.setDisable(true);
        btnPause.setDisable(true);
        btnReset.setDisable(false);
        btnStop.setDisable(true);

        codeEditor.setEditable(true);

        updateGUI();
    }



    private void updateGUI(){
        updateGUIRegisters();
        updateGUITables();
    }

    private void updateGUIRegisters(){
        if(cpu != null) {
            tfRegA.setText(DataRepresentation.getDisplayRepresentation(cpu.getRegA(), displayFormRegisters));
            tfRegB.setText(DataRepresentation.getDisplayRepresentation(cpu.getRegB(), displayFormRegisters));
            tfRegC.setText(DataRepresentation.getDisplayRepresentation(cpu.getRegC(), displayFormRegisters));
            tfRegD.setText(DataRepresentation.getDisplayRepresentation(cpu.getRegD(), displayFormRegisters));

            tfRegPC.setText(DataRepresentation.getDisplayRepresentation(cpu.getRegPC(), displayFormRegisters));
            tfRegSP.setText(DataRepresentation.getDisplayRepresentation(cpu.getRegSP(), displayFormRegisters));
            tfRegMP.setText(DataRepresentation.getDisplayRepresentation(cpu.getRegMP(), displayFormRegisters));

            tfFlagCY.setText(cpu.isFlagCY() ? "1" : "0");
            tfFlagZ.setText(cpu.isFlagZ() ? "1" : "0");
            tfFlagIE.setText(cpu.isFlagIE() ? "1" : "0");
        } else {
            tfRegA.setText(DataRepresentation.getDisplayRepresentation((byte) 0, displayFormRegisters));
            tfRegB.setText(DataRepresentation.getDisplayRepresentation((byte) 0, displayFormRegisters));
            tfRegC.setText(DataRepresentation.getDisplayRepresentation((byte) 0, displayFormRegisters));
            tfRegD.setText(DataRepresentation.getDisplayRepresentation((byte) 0, displayFormRegisters));

            tfRegPC.setText(DataRepresentation.getDisplayRepresentation((short) 0, displayFormRegisters));
            tfRegSP.setText(DataRepresentation.getDisplayRepresentation((short) 0, displayFormRegisters));
            tfRegMP.setText(DataRepresentation.getDisplayRepresentation((short) 0, displayFormRegisters));

            tfFlagCY.setText("-");
            tfFlagZ.setText("-");
            tfFlagIE.setText("-");
        }
    }

    private void updateGUITableForm(ObservableList<? extends MemoryTableCell> list, DataRepresentation.eRepresentation addressRep, DataRepresentation.eRepresentation dataRep){
        if( list.get(0).getAddressRepresentation() != addressRep && list.get(0).getDataRepresentation() != dataRep){
            list.forEach(cell -> {
                cell.changeAddressRepresentation(addressRep);
                cell.changeDataRepresentation(dataRep);
            });
        } else if( list.get(0).getAddressRepresentation() != addressRep ){
            list.forEach(cell -> cell.changeAddressRepresentation(addressRep));
        } else {
            list.forEach(cell -> cell.changeDataRepresentation(dataRep));
        }
    }

    private void updateGUITables(){
        updateGUITableStack();
        updateGUITableProgMemory();
        updateGUITableRAM();
    }

    private void updateGUITableStack(){
        if(cpu != null) {
            byte[] stack = cpu.getStack();
            int SP = Short.toUnsignedInt(cpu.getRegSP());

            tableViewStackItems.get(0).setData(stack[0]);

            for (int i = 65535; i > 0; i--) {
                tableViewStackItems.get(65535 - i + 1).setData(stack[i]);
            }
            StackTableCell.setStackHead(SP);
            tableViewStack.getColumns().get(0).setVisible(false);
            tableViewStack.getColumns().get(0).setVisible(true);

        } else {
            for (int i = 0; i < tableViewStackItems.size(); i++) {
                tableViewStackItems.get(i).setData((byte) 0);
            }
            StackTableCell.setStackHead(0);
            tableViewStack.getColumns().get(0).setVisible(false);
            tableViewStack.getColumns().get(0).setVisible(true);
        }

        handleButtonToStackHeadAction();
    }

    private void updateGUITableProgMemory(){
        if(cpu != null) {
            byte[] progMem = cpu.getProgMemory();

            for (int i = 0; i < progMem.length; i++)
                tableViewProgMemoryItems.get(i).setData(progMem[i]);
        } else {
            for (int i = 0; i < tableViewProgMemoryItems.size(); i++)
                tableViewProgMemoryItems.get(i).setData((byte) 0);
        }
    }

    private void updateGUITableRAM(){
        if(cpu != null) {
            byte[] RAM = cpu.getRAM();

            for (int i = 0; i < RAM.length; i++)
                tableViewRAMItems.get(i).setData(RAM[i]);
        } else {
            for (int i = 0; i < tableViewRAMItems.size(); i++)
                tableViewRAMItems.get(i).setData((byte) 0);
        }
    }

    private void updateExecutionLine(int lineIndex){
        RichTextFXHelpers.tryRemoveParagraphStyle(codeEditor, execution_line.getValue(), "execution-line");
        if(lineIndex >= 0){
            RichTextFXHelpers.addParagraphStyle(codeEditor, lineIndex, "execution-line");
        }
        execution_line.setValue(lineIndex);
    }


    public static class MemoryTableCell{
        private final SimpleStringProperty address;
        private final SimpleStringProperty data;

        private short newAddress;
        private short oldAddress;
        private byte newData;
        private byte oldData;

        private DataRepresentation.eRepresentation repAddress;
        private DataRepresentation.eRepresentation repData;

        MemoryTableCell(){
            this.address = new SimpleStringProperty("0");
            this.data = new SimpleStringProperty("0");
            this.repAddress = DataRepresentation.eRepresentation.Dec;
            this.repData = DataRepresentation.eRepresentation.Dec;
        }

        MemoryTableCell(int address, int data){
            this.address = new SimpleStringProperty(Integer.toString(address));
            this.data = new SimpleStringProperty(Integer.toString(data));
            this.repAddress = DataRepresentation.eRepresentation.Dec;
            this.repData = DataRepresentation.eRepresentation.Dec;

            newAddress = oldAddress = (short) address;
            newData = oldData = (byte) data;
        }

        MemoryTableCell(int address, int data, DataRepresentation.eRepresentation defRepresentationAddress, DataRepresentation.eRepresentation defRepresentationData){
            this.address = new SimpleStringProperty(Integer.toString(address));
            this.data = new SimpleStringProperty(Integer.toString(data));
            this.repAddress = defRepresentationAddress;
            this.repData = defRepresentationData;

            newAddress = oldAddress = (short) address;
            newData = oldData = (byte) data;
        }

        public Property<String> addressProperty() {
            return address;
        }

        public Property<String> dataProperty() {
            return data;
        }

        public int getAddressInt(){
            return Short.toUnsignedInt(newAddress);
        }

        public void setAddress(short address){
            this.address.setValue(
                    DataRepresentation.getDisplayRepresentation(address, repAddress)
            );
            oldAddress = newAddress;
            newAddress = address;
        }

        public void setData(byte data){
            this.data.setValue(
                    DataRepresentation.getDisplayRepresentation(data, repData)
            );

            oldData = newData;
            newData = data;
        }

        public DataRepresentation.eRepresentation getAddressRepresentation(){
            return repAddress;
        }

        public void changeAddressRepresentation(DataRepresentation.eRepresentation newRep){
           address.setValue( DataRepresentation.getDisplayRepresentation(newAddress, newRep) );
           repAddress = newRep;
        }

        public DataRepresentation.eRepresentation getDataRepresentation(){
            return repData;
        }

        public void changeDataRepresentation(DataRepresentation.eRepresentation newRep){
           data.setValue( DataRepresentation.getDisplayRepresentation(newData, newRep) );
           repData = newRep;
        }

        public boolean isChanged(){
            return oldData != newData;
        }

    }

    public static class StackTableCell extends MemoryTableCell{
        private static SimpleIntegerProperty stackHead = new SimpleIntegerProperty(0);

        public StackTableCell(){
            super();
        }

        public StackTableCell(int address, int data){
            super(address, data);
        }

        public StackTableCell(int address, int data, DataRepresentation.eRepresentation defRepresentationAddress, DataRepresentation.eRepresentation defRepresentationData){
            super(address, data, defRepresentationAddress, defRepresentationData);
        }

        public static Property stackHeadProperty(){
            return stackHead;
        }

        public static void setStackHead(int head){
            stackHead.setValue(head);
        }

        public static int getStackHead(){
            return stackHead.getValue();
        }
    }


}//end CPUController