package sk.uniza.fri.cp.App;

import javafx.beans.property.ReadOnlyIntegerWrapper;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableStringValue;
import javafx.beans.value.ObservableValue;
import javafx.collections.*;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;

import java.net.URL;
import java.util.*;

//CodeArea zvyraznovanie slov
import javafx.scene.Node;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import org.fxmisc.flowless.VirtualFlow;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.*;
import org.fxmisc.richtext.model.*;

import java.util.function.IntFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;

import sk.uniza.fri.cp.App.io.ConsoleOutputStream;
import sk.uniza.fri.cp.App.io.ConsolePrintWriter;
import sk.uniza.fri.cp.CPUEmul.CPU;
import sk.uniza.fri.cp.CPUEmul.Exceptions.InvalidCodeLinesException;
import sk.uniza.fri.cp.CPUEmul.Parser;
import sk.uniza.fri.cp.CPUEmul.Program;


/**
 * updateuje stav pri zmene stavu CPU (ChangeListener na Message Task-u)
 * @author Moris
 * @version 1.0
 * @created 07-feb-2017 18:40:27
 */
public class CPUController implements Initializable {

    private final String STYLE_PARAGRAPH_ERROR = "paragraph-error";

	private char stateDispayRegisters;
	private char stateDisplayStackAddr;
	private char stateDisplayStackData;
	private char stateDisplayProgMemoryAddr;
	private char stateDisplayProgMemoryData;

    private Program program;
    private CPU cpu;
    private boolean codeParsed; //indikator, ci je zavedeny aktualny program
    private TreeSet<Integer> breakpointLines;
    private ObservableSet<Integer> observableBreakpointLines;

    //Priznaky vykonavania programu
    private boolean CPUStart;

    //Editor kodu
    @FXML private StackPane codeAreaPane;
    private CodeArea codeEditor;

    //konzola
    @FXML private StackPane consolePane;
    private InlineCssTextArea console;
    private ConsolePrintWriter cpw_error;

	//vyber zobrazenia registrov / pamati
	@FXML private ToggleGroup btnGroupRegisters;
	@FXML private ToggleGroup btnGroupStackAddr;
	@FXML private ToggleGroup btnGroupStackData;
	@FXML private ToggleGroup btnGroupProgMemoryAddr;
	@FXML private ToggleGroup btnGroupProgMemoryData;

	//tlacidla
	@FXML private Button btnParse;
	@FXML private Button btnStart;
	@FXML private Button btnStep;
	@FXML private Button btnPause;
	@FXML private Button btnReset;
	@FXML private Button btnStop;

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

	//pamate
    @FXML private TableView<StackCell> tableStack;
    @FXML private TableColumn<StackCell, Integer> tableColumnStackAddr;
    @FXML private TableColumn<StackCell, Integer> tableColumnStackData;

	@FXML private ProgressBar progressBar;
	@FXML private Label lbStatus;

	/**
	 * Specifikacie pre zvyraznovanie slov v editore
	 */
	private static final String[] KEYWORDS_ARITHMETIC_AND_LOGIC = new String[] {
			"ADD","ADC","ADI","SUB","SBC","SBI","AND","ANI","ORR","ORI","XOR","XRI","INC","INX","DEC","DCX","CMP","CMI"	};
	private static final String[] KEYWORDS_SHIFT_AND_ROTATE = new String[] {
			"SHL","SHR","SCR","RTL","RCL","RTR","RCR" };
	private static final String[] KEYWORDS_MOVE = new String[] {
			"MOV","MVI","MXI","MVX","MMR","LMI","LMR","SMI","SMR","INN","OUT","PUS","POP","STR","LDR" };
	private static final String[] KEYWORDS_BRANCH = new String[] {
			"JMP","JZR","JNZ","JCY","JNC","CAL","CZR","CNZ","CCY","CNC","RET" };
	private static final String[] KEYWORDS_SPECIAL = new String[] {
			"EIT","DIT","SCALL","BYTE" };

	private static final String KEYWORDS_ARITHMETIC_AND_LOGIC_PATTERN = "\\b(" + String.join("|", KEYWORDS_ARITHMETIC_AND_LOGIC) + ")\\b";
	private static final String KEYWORDS_SHIFT_AND_ROTATE_PATTERN = "\\b(" + String.join("|", KEYWORDS_SHIFT_AND_ROTATE) + ")\\b";
	private static final String KEYWORDS_MOVE_PATTERN = "\\b(" + String.join("|", KEYWORDS_MOVE) + ")\\b";
	private static final String KEYWORDS_BRANCH_PATTERN = "\\b(" + String.join("|", KEYWORDS_BRANCH) + ")\\b";
	private static final String KEYWORDS_SPECIAL_PATTERN = "\\b(" + String.join("|", KEYWORDS_SPECIAL) + ")\\b";
	private static final String LABEL_PATTERN = "[a-zA-Z0-9]+:";
	private static final String COMMENT_PATTERN = ";.+[^\n]";

	private static final Pattern PATTERN = Pattern.compile(
			"(?i)(?<KEYWORDSARITHMETICANDLOGIC>" + KEYWORDS_ARITHMETIC_AND_LOGIC_PATTERN + ")"
			+"|(?<KEYWORDSSHIFTANDROTATE>" + KEYWORDS_SHIFT_AND_ROTATE_PATTERN + ")"
			+"|(?<KEYWORDSMOVE>" + KEYWORDS_MOVE_PATTERN + ")"
			+"|(?<KEYWORDSBRANCH>" + KEYWORDS_BRANCH_PATTERN + ")"
			+"|(?<KEYWORDSSPECIAL>" + KEYWORDS_SPECIAL_PATTERN + ")"
			+ "|(?<LABEL>" + LABEL_PATTERN + ")"
			+ "|(?<COMMENT>" + COMMENT_PATTERN + ")"
	);


	/**
	 * Spustene pri inicializacii okna
	 */
	public void initialize(URL location, ResourceBundle resources) {
	    //inizializacia atributov

        //inicializacia tabulky zasobnika
        tableColumnStackAddr.setCellValueFactory(val -> new ReadOnlyObjectWrapper(val.getValue().getAddress()) );
        tableColumnStackData.setCellValueFactory(val -> new ReadOnlyObjectWrapper(val.getValue().getData()) );

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
        codeEditor = new CodeArea();
        codeAreaPane.getChildren().add(new VirtualizedScrollPane<>(codeEditor));
        IntFunction<Node> breakpointFactory = new BreakpointFactory(codeEditor, observableBreakpointLines); //breakpointy
        IntFunction<Node> lineNumberFactory = LineNumberFactory.get(codeEditor); //cisla riadkov
        codeEditor.setParagraphGraphicFactory(line -> {
            HBox hbox = new HBox(
                    breakpointFactory.apply(line),
                    lineNumberFactory.apply(line)
            );

            return hbox;
        });	//zobrazenie riadkovania

		codeEditor.richChanges()
				.filter(ch -> !ch.getInserted().equals(ch.getRemoved()))
				.subscribe(change -> {
				    codeEditor.setStyleSpans(0, computeHighlighting(codeEditor.getText()));
				    codeParsed = false; //zmena v kode -> program nie je aktualny
				});


		//Listenery na zmenu zobrazenia registrov a pamati
		btnGroupRegisters.selectedToggleProperty().addListener( (observable, oldValue, newValue)->{stateDispayRegisters=onToggleGroupChange(btnGroupRegisters, oldValue, newValue); updateGUIState();});
		btnGroupStackAddr.selectedToggleProperty().addListener( (observable, oldValue, newValue)->{stateDisplayStackAddr=onToggleGroupChange(btnGroupStackAddr, oldValue, newValue); updateGUIState();});
		btnGroupStackData.selectedToggleProperty().addListener( (observable, oldValue, newValue)->{stateDisplayStackData=onToggleGroupChange(btnGroupStackData, oldValue, newValue); updateGUIState();});
		btnGroupProgMemoryAddr.selectedToggleProperty().addListener( (observable, oldValue, newValue)->{stateDisplayProgMemoryAddr=onToggleGroupChange(btnGroupProgMemoryAddr, oldValue, newValue); updateGUIState();});
		btnGroupProgMemoryData.selectedToggleProperty().addListener( (observable, oldValue, newValue)->{stateDisplayProgMemoryData=onToggleGroupChange(btnGroupProgMemoryData, oldValue, newValue); updateGUIState();});

		//inicializacia stavov zobrazenia registrov a pamati na aktualne vybrane tlacidla
		stateDispayRegisters = ( (ToggleButton) btnGroupRegisters.getSelectedToggle()).getText().charAt(0);
		stateDisplayStackAddr =  ( (ToggleButton) btnGroupStackAddr.getSelectedToggle()).getText().charAt(0);
		stateDisplayStackData = ( (ToggleButton) btnGroupStackData.getSelectedToggle()).getText().charAt(0);
		stateDisplayProgMemoryAddr = ( (ToggleButton) btnGroupProgMemoryAddr.getSelectedToggle()).getText().charAt(0);
		stateDisplayProgMemoryData = ( (ToggleButton) btnGroupProgMemoryData.getSelectedToggle()).getText().charAt(0);





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
	}

	/**
	 * HANDLERS
	 */

	/**
	 * nacita text studenta (program) do suboru
	 */
	@FXML
	private void handleLoadAction(){

	}

	@FXML
	private void handleSaveAction(){

	}

	@FXML
	private void handleButtonUnbreakAllAction(){
        System.out.println("Breaks:" + breakpointLines);
	    observableBreakpointLines.clear();

		observableBreakpointLines.addListener((SetChangeListener<Integer>) change -> System.out.println("Breaks:" + breakpointLines));
	}


	/**
	 * zoberie program studenta a pokusi sa ho parsovat na instrukcie... zaroven
	 * zablokuje moznost parsovania az pokial sa nezmeni cast studentovho kodu
	 */
	@FXML
	private void handleButtonParseAction(){
	    parseCode();
	}

	@FXML
	private void handleButtonPauseAction(){

	}

	@FXML
	private void handleButtonResetAction(){

	}

	@FXML
	private void handleButtonStartAction(){
	    CPUStart = true;

	    //ak kod este nie je zavedeny
	    if(!codeParsed) {
	        parseCode();
        } else {
	        startExecution();
        }
	}

	@FXML
	private void handleButtonStepAction(){

	}

	@FXML
	private void handleButtonStopAction(){

	}



	/**
	 * pridava a odobera debugovacie zastavenia v programe
	 */
	private void handleSetBreakAction(){

	}

	private void handleUnbreakAction(){

	}

	private void handleUnbreakAllAction(){

	}


	/**
	 * obsluha zmeny zobrazenia registrov
	 */
	private void handleRegDispGroupAction(){

	}

	private void handleStackAddrDispGroupAction(){

	}

	private void handleStackDataDispGroupAction(){

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
    private char onToggleGroupChange(ToggleGroup btnGroup, Toggle oldValue, Toggle newValue){
        ToggleButton tb;
        if(newValue != null) {
            tb = (ToggleButton) newValue;
        } else {
            btnGroup.selectToggle(oldValue);
            tb = (ToggleButton) oldValue;
        }
        return tb.getText().charAt(0);
    }


    /**
     * obsluha konzoly
     */
    @FXML
    private void handleButtonConsoleClearAction(){
        console.clear();
    }


    /**
     * Farebne zvyraznovanie v editore kodu
     */
    private StyleSpans<Collection<String>> computeHighlighting(String text) {
        Matcher matcher = PATTERN.matcher(text);
        int lastKwEnd = 0;
        StyleSpansBuilder<Collection<String>> spansBuilder
                = new StyleSpansBuilder<>();
        while(matcher.find()) {
            String styleClass =
                    matcher.group("KEYWORDSARITHMETICANDLOGIC") != null ? "keyword-arithmetic-and-logic" :
                            matcher.group("KEYWORDSSHIFTANDROTATE") != null ? "keyword-shift-and-rotate" :
                                    matcher.group("KEYWORDSMOVE") != null ? "keyword-move" :
                                            matcher.group("KEYWORDSBRANCH") != null ? "keyword-branch" :
                                                    matcher.group("KEYWORDSSPECIAL") != null ? "keyword-special" :
                                                            matcher.group("LABEL") != null ? "label" :
                                                                    matcher.group("COMMENT") != null ? "comment" :
                                                                            null; /* never happens */ assert styleClass != null;
            spansBuilder.add(Collections.emptyList(), matcher.start() - lastKwEnd);
            spansBuilder.add(Collections.singleton(styleClass), matcher.end() - matcher.start());
            lastKwEnd = matcher.end();
        }
        spansBuilder.add(Collections.emptyList(), text.length() - lastKwEnd);
        return spansBuilder.create();
    }

    private void parseCode(){
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

        //spustenie vlakna pre parsovanie kodu
        parserThread.start();

        parserTask.setOnSucceeded(new EventHandler<WorkerStateEvent>() {
            @Override
            public void handle(WorkerStateEvent event) {
                writeConsoleLn("Program úspešne zavedený!", "green");
                program = parserTask.getValue();

                codeParsed = true; //kod je uspesne prevedeny na program a moze byt vykonany
                lbStatus.setText("Program zavedený");

                //ak ma byt spustene vykonavanie CPU
                if(CPUStart)
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

                codeParsed = false;
                CPUStart = false;
                //zobrazenie stavu
                lbStatus.setText("Chyba v kóde");
            }
        });
    }

    private void startExecution(){
        //vytvorenie CPU a spustenie vykonavania
        cpu = new CPU(program, new ConsoleOutputStream(console), false, false);
        Thread cpuThread = new Thread(cpu);
        cpuThread.start();
        lbStatus.setText("Program spustený");

        //po usepsnom vykonani programu
        cpu.setOnSucceeded(new EventHandler<WorkerStateEvent>() {
            @Override
            public void handle(WorkerStateEvent event) {
                lbStatus.setText("Program ukončený");
                CPUStart = false;
                //cpu = null;
                updateGUIState();
            }
        });

        //pri zmene stavu procesora
        cpu.messageProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
                updateGUIState();
            }
        });
    }


    private void updateGUIState(){
        if(cpu != null) {
            tfRegA.setText(getDisplayRepresentation(cpu.getRegA(), stateDispayRegisters));
            tfRegB.setText(getDisplayRepresentation(cpu.getRegB(), stateDispayRegisters));
            tfRegC.setText(getDisplayRepresentation(cpu.getRegC(), stateDispayRegisters));
            tfRegD.setText(getDisplayRepresentation(cpu.getRegD(), stateDispayRegisters));

            tfRegPC.setText(getDisplayRepresentation(cpu.getRegPC(), stateDispayRegisters));
            tfRegSP.setText(getDisplayRepresentation(cpu.getRegSP(), stateDispayRegisters));
            tfRegMP.setText(getDisplayRepresentation(cpu.getRegMP(), stateDispayRegisters));

            tfFlagCY.setText(cpu.isFlagCY() ? "1" : "0");
            tfFlagZ.setText(cpu.isFlagZ() ? "1" : "0");
            tfFlagIE.setText(cpu.isFlagIE() ? "1" : "0");

            updateStack();
        } else {
            tfRegA.setText(getDisplayRepresentation((byte) 0, stateDispayRegisters));
            tfRegB.setText(getDisplayRepresentation((byte) 0, stateDispayRegisters));
            tfRegC.setText(getDisplayRepresentation((byte) 0, stateDispayRegisters));
            tfRegD.setText(getDisplayRepresentation((byte) 0, stateDispayRegisters));

            tfRegPC.setText(getDisplayRepresentation((short) 0, stateDispayRegisters));
            tfRegSP.setText(getDisplayRepresentation((short) 0, stateDispayRegisters));
            tfRegMP.setText(getDisplayRepresentation((short) 0, stateDispayRegisters));

            tfFlagCY.setText("-");
            tfFlagZ.setText("-");
            tfFlagIE.setText("-");
        }
    }

    private void updateStack(){
        byte[] stack = cpu.getStack();
        int SP = Short.toUnsignedInt(cpu.getRegSP());

        tableStack.getItems().clear();
        //nulta adresa
        tableStack.getItems().add(new StackCell(0, stack[0]));
        //pridavanie poloziek tabulky
        if(SP != 0){
            int i = 65535;
            while(i >= SP){
                tableStack.getItems().add(new StackCell(i, Byte.toUnsignedInt(stack[i])));
                i--;
            }
        }
    }

    /*
    mvi a,20
mvi b,5

start:
	sub a,b
	pus a
	jnz start
     */

    private String getDisplayRepresentation(byte value, char displayState){
        switch (displayState){
            case 'D':
                return Integer.toString(value & 0xFF );
            case 'B':
                return Integer.toBinaryString((value & 0xFF)  + 0x100).substring(1);
            case 'H':
                return "0x" + Integer.toHexString((value & 0xFF)  + 0x100).substring(1);
            case 'A':
                return String.valueOf((char) value);
        }
        return "";
    }
//    mvi a,5
//    mvi b,10
//    mvi c, 15
//    mvi d,20
//    add a,b

    private String getDisplayRepresentation(short value, char displayState){
        switch (displayState){
            case 'D':
                return Integer.toString(value & 0xFFFF);
            case 'B':
                return Integer.toBinaryString((value & 0xFFFF) + 0x10000).substring(1);
            case 'H':
                return "0x" + Integer.toHexString((value & 0xFFFF) + 0x10000).substring(1);
            case 'A':
                char L = (char) value;
                char H = (char) (value >> 4);
                return String.format("%c%c", H, L);
        }
        return "";
    }

    private static class StackCell{
        private final int address;
        private final int data;

        StackCell(int address, int data){
            this.address = address;
            this.data = data;
        }

        public int getAddress() {
            return address;
        }

        public int getData() {
            return data;
        }
    }


}//end CPUController