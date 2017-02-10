package sk.uniza.fri.cp.App;

import javafx.collections.ObservableList;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;

import java.net.URL;
import java.util.Collection;
import java.util.ResourceBundle;

//CodeArea zvyraznovanie slov
import javafx.scene.Node;
import javafx.scene.layout.HBox;
import org.fxmisc.richtext.*;
import org.fxmisc.richtext.model.*;
import java.util.Collections;
import java.util.function.IntFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;

import sk.uniza.fri.cp.App.io.ConsoleOutputStream;
import sk.uniza.fri.cp.App.io.ConsolePrintWriter;
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

	private char stateDispayRegisters;
	private char stateDisplayStackAddr;
	private char stateDisplayStackData;
	private char stateDisplayProgMemoryAddr;
	private char stateDisplayProgMemoryData;

    private Program program;
    private ObservableList<Integer> breakpointLines;

	@FXML private CodeArea codeEditor;

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

	//konzola
	@FXML private InlineCssTextArea console;
	private ConsolePrintWriter cpw_error;

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
	    //breakpointLines init

		//Inicializacia editora kodu
		//codeEditor.setParagraphGraphicFactory(LineNumberFactory.get(codeEditor));	//zobrazenie riadkovania

        IntFunction<Node> breakpointFactory = new BreakpointFactory();
        IntFunction<Node> lineNumberFactory = LineNumberFactory.get(codeEditor);
        codeEditor.setParagraphGraphicFactory(line -> {

            HBox hbox = new HBox(
                    breakpointFactory.apply(line),
                    lineNumberFactory.apply(line)
            );

            return hbox;
        });	//zobrazenie riadkovania

		codeEditor.richChanges()
				.filter(ch -> !ch.getInserted().equals(ch.getRemoved())) // XXX
				.subscribe(change -> codeEditor.setStyleSpans(0, computeHighlighting(codeEditor.getText())));

		//Listenery na zmenu zobrazenia registrov a pamati
		btnGroupRegisters.selectedToggleProperty().addListener( (observable, oldValue, newValue)->stateDispayRegisters=onToggleGroupChange(btnGroupRegisters, oldValue, newValue));
		btnGroupStackAddr.selectedToggleProperty().addListener( (observable, oldValue, newValue)->stateDisplayStackAddr=onToggleGroupChange(btnGroupStackAddr, oldValue, newValue));
		btnGroupStackData.selectedToggleProperty().addListener( (observable, oldValue, newValue)->stateDisplayStackData=onToggleGroupChange(btnGroupStackData, oldValue, newValue));
		btnGroupProgMemoryAddr.selectedToggleProperty().addListener( (observable, oldValue, newValue)->stateDisplayProgMemoryAddr=onToggleGroupChange(btnGroupProgMemoryAddr, oldValue, newValue));
		btnGroupProgMemoryData.selectedToggleProperty().addListener( (observable, oldValue, newValue)->stateDisplayProgMemoryData=onToggleGroupChange(btnGroupProgMemoryData, oldValue, newValue));

		//inicializacia stavov zobrazenia registrov a pamati na aktualne vybrane tlacidla
		stateDispayRegisters = ( (ToggleButton) btnGroupRegisters.getSelectedToggle()).getText().charAt(0);
		stateDisplayStackAddr =  ( (ToggleButton) btnGroupStackAddr.getSelectedToggle()).getText().charAt(0);
		stateDisplayStackData = ( (ToggleButton) btnGroupStackData.getSelectedToggle()).getText().charAt(0);
		stateDisplayProgMemoryAddr = ( (ToggleButton) btnGroupProgMemoryAddr.getSelectedToggle()).getText().charAt(0);
		stateDisplayProgMemoryData = ( (ToggleButton) btnGroupProgMemoryData.getSelectedToggle()).getText().charAt(0);

		//Inicializacia konzolovych streamov
		//ERROR STREAM
		cpw_error = new ConsolePrintWriter(new ConsoleOutputStream(console, "red"));

		//SANDBOX
		writeConsoleLn("Ahojky");
		writeConsoleLn("bauky", "red");
		writeConsoleLn("manuky", "blue");
		writeConsoleLn("manuky");

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
	 * Farebne zvyraznovanie v editore kodu
	 */
	private static StyleSpans<Collection<String>> computeHighlighting(String text) {
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

	/**
	 *
	 * @param lineIndex Index riadku pre zvyraznenie
	 */
	public void heightlightCodeLineError(int lineIndex){
        codeEditor.setParagraphStyle(lineIndex, Collections.singleton("paragraph-error"));
	}

	public void clearCodeHeightligting(){
        for (int i = 0; i < codeEditor.getParagraphs().size(); i++){
            codeEditor.clearParagraphStyle(i);
        }
    }

    public void clearCodeHeightligting(int lineIndex){
        codeEditor.clearParagraphStyle(lineIndex);
    }

	private void updateState(){

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
	private void handleLoadAction(){

	}

	private void handleSaveAction(){

	}


	/**
	 * zoberie program studenta a pokusi sa ho parsovat na instrukcie... zaroven
	 * zablokuje moznost parsovania az pokial sa nezmeni cast studentovho kodu
	 */
	@FXML
	private void handleButtonParseAction(){
	    //pocet riadkov v editore pre update progress baru
	    int lines = codeEditor.getParagraphs().size();
	    //vytvorenie parsera
        Parser parser = new Parser(codeEditor.getText(), lines, cpw_error);
        Thread parserThread = new Thread(parser);
        //zastavenie vlakna ak skonci aplikacia
        parserThread.setDaemon(true);

        //vycistenie editoru ak boli chyby
        clearCodeHeightligting();

        //zobrazenie stavu
        progressBar.progressProperty().unbind();
        progressBar.progressProperty().bind(parser.progressProperty());
        lbStatus.setText("Parsujem...");

        //spustenie vlakna pre parsovanie kodu
        parserThread.start();

        parser.setOnSucceeded(new EventHandler<WorkerStateEvent>() {
            @Override
            public void handle(WorkerStateEvent event) {
                writeConsoleLn("Program úspešne zavedený!", "green");
                program = parser.getValue();

                lbStatus.setText("Program zavedený");
            }
        });

        parser.setOnFailed(new EventHandler<WorkerStateEvent>() {
            @Override
            public void handle(WorkerStateEvent event) {
                InvalidCodeLinesException ex = (InvalidCodeLinesException) parser.getException();

                //zvyraznenie riadkov s chybovym kodom
                for (int line :
                        ex.getLines()) {
                    heightlightCodeLineError(line);
                }

                //posun konzoly na koniec
                console.setEstimatedScrollY(console.getParagraphs().size() * 50);

                //zobrazenie stavu
                lbStatus.setText("Chyba v kóde");
            }
        });
	}

	@FXML
	private void handleButtonPauseAction(){
        clearCodeHeightligting();
	}

	@FXML
	private void handleButtonResetAction(){

	}

	@FXML
	private void handleButtonStartAction(){

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
     * obsluha konzoly
     */
    @FXML
    private void handleButtonConsoleClearAction(){
        console.clear();
    }

}//end CPUController