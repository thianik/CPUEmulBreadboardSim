package sk.uniza.fri.cp.CPUEmul;

import javafx.application.Platform;
import javafx.concurrent.Task;
import sk.uniza.fri.cp.CPUEmul.Exceptions.InvalidCodeLinesException;
import sun.reflect.generics.tree.Tree;

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Parsuje prikazy z editoru do pola pre vykonavanie
 * @author Moris
 * @version 1.0
 * @created 07-feb-2017 18:40:27
 */
public class Parser extends Task<Program>{

    private static final String LABEL_PATTERN = "^\\s*\\w+:"; //po zaciatku riadku ziadna alebo viac whitespace znakov, [a-zA-Z_0-9] a dvojbodka
    private static final String COMMENT_PATTERN = "(?m);.+$"; //vsetko za bodkociarkou po koniec riadku

    //KOD
    private String code;
    private boolean hasError; //je v kode chyba?
    private int numOfCodeLines;

    //PROGRAM
    private ArrayList<Instruction> instructions;
    private TreeMap<Integer, Integer> lineOfInstruction; //prevodnik medzi riadkom a instrukciou
    private ArrayList<Byte> progMemory;
    private TreeMap<String, Integer> interruptionLabels;


    //POMOCNE
    //TreeMap pre zapisanie navestia a indexu instrukcie na ktoru odkazuje
    private TreeMap<String, Integer> labels = new TreeMap<>();
    //pole pre uchovanie indexov instrukcii, ktore obsahuju navestie pre jeho nahradenie indexom skoku
    private ArrayList<Integer> instructionsWithLabels;

    //CHYBY
    //uchovavanie chyb pre vypis na konzolu
    private ArrayList<String> errors;
    private ArrayList<Integer> errorLines;
    private PrintWriter printWriter;


	public Parser(String code, int numberOfCodeLines, PrintWriter pw){
        this.code = code;
        numOfCodeLines = numberOfCodeLines;
        printWriter = pw;
        instructions = new ArrayList<>(numberOfCodeLines);
        instructionsWithLabels = new ArrayList<>();
        progMemory = new ArrayList<>();
        lineOfInstruction = new TreeMap<>();
        interruptionLabels = new TreeMap<>();

        hasError = false;
	}

    /**
     * Parsovanie kodu programu
     */

    @Override
    protected Program call() throws InvalidCodeLinesException {
        int lineIndex = 0;

        //odstranenie vsetkych komentarov
        Pattern pat = Pattern.compile(COMMENT_PATTERN);
        code = pat.matcher(code).replaceAll("");

        //pattern pre navestia
        Pattern patternLabel = Pattern.compile(LABEL_PATTERN);

        //citanie po riadkoch kodu
        try(BufferedReader br = new BufferedReader(new StringReader(code))){
            String line = br.readLine();

            //citaj pokial nebolo vlakno ukoncene alebo nie je koniec kodu
            while (!isCancelled() && line != null){

                //nastavenie progresu
                updateProgress(lineIndex+1, numOfCodeLines);

                //kontrola ci riadok obsahuje navestie
                Matcher matcherLabel = patternLabel.matcher(line);
                if(matcherLabel.find()){
                    String label = matcherLabel.group();
                    label = label.substring(0, label.length()-1).trim(); //odstranenie dvojbodky a medzier

                    //existuje uz dane navestie?
                    if(!labels.containsKey(label)) {
                        //kontrola, ci sa jedna o navestie pre prerusenie
                        if(label.matches("int.*")){
                            //navestie zacina int -> musi byt int00 az int0f
                            if(label.matches("int0\\p{XDigit}")){
                                //splna podmienky navestia pre prerusenie
                                interruptionLabels.put(label, instructions.size()); //index nasledujucej instrukcie, ktora bude pridana
                            } else {
                                //asi to malo byt prerusenie ale je v zlom tvare
                                addError("Nesprávny tvar návestia pre prerušenie '" + label + "'", lineIndex);
                            }
                        } else {
                            //obycajne navestie
                            labels.put(label, instructions.size()); //index nasledujucej instrukcie, ktora bude pridana
                        }
                    } else {
                        addError("Návestie " + label + " už existuje na riadku " + labels.get(label), lineIndex);
                    }

                    //odstran navestie z riadku
                    line = matcherLabel.replaceFirst("");
                }

                //idealne by v riadku mala ostat uz iba instrukcia
                line = line.trim();
                //ak je riadok prazdny, chod na dalsi
                if(line.isEmpty()){
                    lineIndex++;
                    line = br.readLine();
                    continue;
                }

                //inak sa pokus rozdelit instrukciu a parametre
                String[] instructionParts = line.split("\\s*,\\s*|\\s+"); //rozdelenie instrukcie (medzery a ciarka)

                //je instrukcia platna?
                try {
                    enumInstructionsSet codeInstruction = enumInstructionsSet.valueOf(instructionParts[0].toUpperCase());

                    //ak je platna
                    //kontrola poctu parametrov
                    if(codeInstruction.getNumOfParameters() == instructionParts.length-1) { //porovnanie pozadovanych parametrov s poctom nacitanych (-1 lebo instrukcia samotna)
                        boolean parametersError = false;

                        //kontrola spravnosti parametrov
                        if(codeInstruction.getNumOfParameters() > 0 && !instructionParts[1].matches(codeInstruction.getFirstRegex())){ //kontrola prveho parametra
                            //ak nezodpoveda predpisu
                            addError("Nesprávny prvý parameter inštrukcie '" + instructionParts[0] + "'", lineIndex);
                            parametersError = true;
                        }

                        if(codeInstruction.getNumOfParameters() > 1 && !instructionParts[2].matches(codeInstruction.getSecondRegex())){ //kontrola druheho parametra
                            //ak nezodpoveda predpisu
                            addError("Nesprávny druhý parameter inštrukcie '" + instructionParts[0] + "'", lineIndex);
                            parametersError = true;
                        }

                        //ak je cela instrukcia aj s parametrami v poriadku, mozeme ju zaviest do programu (ak je aj chybny, kontroluje sa este spravnost navesti v instrukciach)
                        if(!parametersError){
                            switch (codeInstruction.getNumOfParameters()){
                                case 0:
                                    instructions.add(new Instruction(codeInstruction));
                                    break;
                                case 1:
                                    //ak je instrukcia BYTE, zaved konstantu do pamate ale nie instrukciu do programu
                                    if(codeInstruction == enumInstructionsSet.BYTE){
                                        byte b = (byte) Integer.parseInt(instructionParts[1]);
                                        progMemory.add(b);
                                    } else {
                                        instructions.add(new Instruction(codeInstruction, instructionParts[1]));
                                    }
                                    break;
                                case 2:
                                    instructions.add(new Instruction(codeInstruction, instructionParts[1], instructionParts[2]));
                                    break;
                            }

                            //ak instrukcia pouziva navestie
                            if(codeInstruction.usesLabel()){
                                //pridanie indexu inst. pre jej zmenu (navestie -> index)
                                instructionsWithLabels.add(instructions.size()-1);
                            }

                            //zradenie do pervodnika medzi riadkami kodu a indexami instrukcii (pre breaky)
                            lineOfInstruction.put(lineIndex, instructions.size()-1);
                        }

                    } else { //pocet parametrov neodpoveda
                        addError("Zlý počet parametrov inštrukcie '" + instructionParts[0] + "', požadovaný počet '" + codeInstruction.getNumOfParameters() + "'", lineIndex);
                    }

                } catch (IllegalArgumentException e){
                    //ak instrukcia nie je platna (nie je v zozname instrukcii)
                    addError("Neplatná inštrukcia '" + instructionParts[0] + "'", lineIndex);
                }

                lineIndex++;
                line = br.readLine();
            }

            updateProgress(numOfCodeLines, numOfCodeLines);
        } catch (IOException e) {
            //chyba so streamom stringu z kodu
            e.printStackTrace();
        }

        //ak je program v poriadku, preved navestia na indexy
        if(!hasError && labels.size() > 0) transformLabelsToIndexes();

        //zmensenie pola arraylistu na minimum
        instructions.trimToSize();

        //zostavenie programu
        if(!hasError){
            return new Program(instructions, progMemory, lineOfInstruction, interruptionLabels);
        } else {
            //kod obsahuje chyby
            //vypisat chyby na konzolu
            printErrors();

            //vyhodit vynimku s chybnymi riadkami
            int[] arr = errorLines.stream().filter(Objects::nonNull).mapToInt(i -> i).toArray();
            throw new InvalidCodeLinesException(arr);
        }
    }

    private void transformLabelsToIndexes(){
	    //prechadzaj vsetky instrukcie, ktore pouzivaju navestie
        for (Integer indexOfInstruction: instructionsWithLabels) {
            //zober instrukciu s navestim
            Instruction inst = instructions.get(indexOfInstruction);
            //zober jej navestie
            String label = inst.getFirstParameter();

            if(label != null){
                //pokus sa najst odpovedajuci index instrukcie, na ktore navestie ukazuje
                Integer jumpIndex = labels.get(label);
                //ak sa navestie naslo
                if(jumpIndex != null){
                    //zmen navestie instrukcie na index skoku
                    inst.setFirstParameter(Integer.toString(jumpIndex));
                } else {
                    //ak sa navestie nenaslo
                    addError("Neplatné návestie '" + label + "'", getLineOfInstruction(indexOfInstruction));
                }
            } else {
                addError("Chyba v inštrukcií", getLineOfInstruction(indexOfInstruction));
            }
        }
    }

    /**
     * hlada a vracia riadok instrukcie
     * @param instructionIndex Index instrukcie ku ktorej sa hlada riaok
     * @return Index riadku na ktorom je instrukcia, ak sa nenasla, vracia -1
     */
    private int getLineOfInstruction(int instructionIndex){
        for (Map.Entry<Integer, Integer> entry : lineOfInstruction.entrySet()) {
            if (Objects.equals(instructionIndex, entry.getValue())) {
                return entry.getKey();
            }
        }
        return -1;
    }

    private void addError(String err, int lineIndex){
        if(!hasError){
            errors = new ArrayList<>();
            errorLines = new ArrayList<>();
        }

        errors.add("["+(lineIndex+1)+"] " + err);
        errorLines.add(lineIndex);
        hasError = true;
    }

    private void printErrors(){
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                printWriter.println("");
                printWriter.println("---------------------------------------");
                printWriter.println("[Parser] NEPODARILO SA ZAVIESŤ PROGRAM!");
                for (String err :
                        errors) {
                    printWriter.println("[Parser] " + err);
                }
            }
        });
    }

}//end Parser