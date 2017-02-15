package sk.uniza.fri.cp.CPUEmul;

/**
 * @author Moris
 * @version 1.0
 * @created 07-feb-2017 18:40:27
 */
public enum enumInstructionsSet implements iRegexes{

    //artimeticke a logicke inst.
    ADD("(?i)A|B|C|D", "(?i)A|B|C|D"),
    ADC("(?i)A|B|C|D", "(?i)A|B|C|D"),
    ADI("(?i)A|B|C|D", rByte),
    SUB("(?i)A|B|C|D", "(?i)A|B|C|D"),
    SUC("(?i)A|B|C|D", "(?i)A|B|C|D"),
    SBI("(?i)A|B|C|D", rByte),
    AND("(?i)A|B|C|D", "(?i)A|B|C|D"),
    ANI("(?i)A|B|C|D", rByte),
    ORR("(?i)A|B|C|D", "(?i)A|B|C|D"),
    ORI("(?i)A|B|C|D", rByte),
    XOR("(?i)A|B|C|D", "(?i)A|B|C|D"),
    XRI("(?i)A|B|C|D", rByte),
    INC("(?i)A|B|C|D"),
    INX("(?i)S|M"),
    DEC("(?i)A|B|C|D"),
    DCX("(|?i)S|M"),
    CMP("(?i)A|B|C|D", "(?i)A|B|C|D"),
    CMI("(?i)A|B|C|D", rByte),

    //inst. presunu a rotacie
    SHL("(?i)A|B|C|D", "[1-8]"),
    SHR("(?i)A|B|C|D", "[1-8]"),
    SCR("(?i)A|B|C|D", "[1-8]"),
    RTL("(?i)A|B|C|D", "[1-8]"),
    RCL("(?i)A|B|C|D", "[1-8]"),
    RTR("(?i)A|B|C|D", "[1-8]"),
    RCR("(?i)A|B|C|D", "[1-8]"),

    //inst. prenosu dat
    MOV("(?i)A|B|C|D", "(?i)A|B|C|D"),
    MVI("(?i)A|B|C|D", rByte),
    MXI("(?i)S|M", rShort),
    MVX("(?i)S|M|C", "(?i)A|S|M"),
    MMR("(?i)A|B|C|D", "(?i)A|B|C|D"),
    LMI("(?i)A|B|C|D", rShort),
    LMR("(?i)A|B|C|D"),
    SMI(rShort, "(?i)A|B|C|D"),
    SMR("(?i)A|B|C|D"),
    INN("(?i)A|B|C|D", rShort),
    OUT(rShort, "(?i)A|B|C|D"),
    PUS("(?i)A|B|C|D|F|M"),
    POP("(?i)A|B|C|D|F|M"),
    STR("(?i)A|B|C|D", "(?i)A|B|C|D"),
    LDR("(?i)A|B|C|D", "(?i)A|B|C|D"),

    //inst. vetvenia
    JMP("\\w+", true),
    JZR("\\w+", true),
    JNZ("\\w+", true),
    JCY("\\w+", true),
    JNC("\\w+", true),
    CAL("\\w+", true),
    CZR("\\w+", true),
    CNZ("\\w+", true),
    CCY("\\w+", true),
    CNC("\\w+", true),
    RET(),

    //specialne instrukcie
    EIT(),DIT(),
    SCALL("(?i)KPR|KEY|DSP"),
    BYTE(rByte);

    private int numOfParameters;
    private String firstRegex;
    private String secondRegex;
    private boolean usesLabel;

    enumInstructionsSet(){
        numOfParameters = 0;
    }

    enumInstructionsSet(String firstArg){
        numOfParameters = 1;
        firstRegex = firstArg;
    }

    enumInstructionsSet(String firstArg, boolean usesLabel){
        numOfParameters = 1;
        firstRegex = firstArg;
        this.usesLabel = usesLabel;
    }

    enumInstructionsSet(String firstArg, String secondArg){
        numOfParameters = 2;
        firstRegex = firstArg;
        secondRegex = secondArg;
    }

    public int getNumOfParameters() {
        return numOfParameters;
    }

    public String getFirstRegex() {
        return firstRegex;
    }

    public String getSecondRegex() {
        return secondRegex;
    }

    public boolean usesLabel() {
        return usesLabel;
    }
}