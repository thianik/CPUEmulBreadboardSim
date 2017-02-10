package sk.uniza.fri.cp.CPUEmul;

/**
 * @author Moris
 * @version 1.0
 * @created 07-feb-2017 18:40:27
 */
public enum enumInstructionsSet {
    //REGEX
    //0-255 -> ^([0-1]?\\d?\\d|2[0-4]\\d|25[0-5])$|^0[xX]\\\\p{XDigit}{1,2}$
    //0x0-0xff -> ^0[xX]\\p{XDigit}{1,2}$
    //0-65535 -> ^([0-5]?\\d?\\d?\\d?\\d|6[0-4]\\d\\d\\d|65[0-4]\\d\\d|655[0-2]\\d|6553[0-5])$
    //0x0-0xffff -> ^0[xX]\\p{XDigit}{1,4}$

    //artimeticke a logicke inst.
    ADD("(?i)A|B|C|D", "(?i)A|B|C|D"),
    ADC("(?i)A|B|C|D", "(?i)A|B|C|D"),
    ADI("(?i)A|B|C|D", "^([0-1]?\\d?\\d|2[0-4]\\d|25[0-5])$|^0[xX]\\p{XDigit}{1,2}$"),
    SUB("(?i)A|B|C|D", "(?i)A|B|C|D"),
    SBC("(?i)A|B|C|D", "(?i)A|B|C|D"),
    SBI("(?i)A|B|C|D", "^([0-1]?\\d?\\d|2[0-4]\\d|25[0-5])$|^0[xX]\\p{XDigit}{1,2}$"),
    AND("(?i)A|B|C|D", "(?i)A|B|C|D"),
    ANI("(?i)A|B|C|D", "^([0-1]?\\d?\\d|2[0-4]\\d|25[0-5])$|^0[xX]\\p{XDigit}{1,2}$"),
    ORR("(?i)A|B|C|D", "(?i)A|B|C|D"),
    ORI("(?i)A|B|C|D", "^([0-1]?\\d?\\d|2[0-4]\\d|25[0-5])$|^0[xX]\\p{XDigit}{1,2}$"),
    XOR("(?i)A|B|C|D", "(?i)A|B|C|D"),
    XRI("(?i)A|B|C|D", "^([0-1]?\\d?\\d|2[0-4]\\d|25[0-5])$|^0[xX]\\p{XDigit}{1,2}$"),
    INC("(?i)A|B|C|D"),
    INX("(?i)S|M"),
    DEC("(?i)A|B|C|D"),
    DCX("(|?i)S|M"),
    CMP("(?i)A|B|C|D", "(?i)A|B|C|D"),
    CMI("(?i)A|B|C|D", "^([0-1]?\\d?\\d|2[0-4]\\d|25[0-5])$|^0[xX]\\p{XDigit}{1,2}$"),

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
    MVI("(?i)A|B|C|D", "^([0-1]?\\d?\\d|2[0-4]\\d|25[0-5])$|^0[xX]\\p{XDigit}{1,2}$"),
    MXI("(?i)S|M", "^([0-5]?\\d?\\d?\\d?\\d|6[0-4]\\d\\d\\d|65[0-4]\\d\\d|655[0-2]\\d|6553[0-5])$|^0[xX]\\p{XDigit}{1,4}$"),
    MVX("(?i)S|M|C", "(?i)A|S|M"),
    MMR("(?i)A|B|C|D", "(?i)A|B|C|D"),
    LMI("(?i)A|B|C|D", "^([0-5]?\\d?\\d?\\d?\\d|6[0-4]\\d\\d\\d|65[0-4]\\d\\d|655[0-2]\\d|6553[0-5])$|^0[xX]\\p{XDigit}{1,4}$"),
    LMR("(?i)A|B|C|D"),
    SMI("^([0-5]?\\d?\\d?\\d?\\d|6[0-4]\\d\\d\\d|65[0-4]\\d\\d|655[0-2]\\d|6553[0-5])$|^0[xX]\\p{XDigit}{1,4}$", "(?i)A|B|C|D"),
    SMR("(?i)A|B|C|D"),
    INN("(?i)A|B|C|D", "^([0-5]?\\d?\\d?\\d?\\d|6[0-4]\\d\\d\\d|65[0-4]\\d\\d|655[0-2]\\d|6553[0-5])$|^0[xX]\\p{XDigit}{1,4}$"),
    OUT("^([0-5]?\\d?\\d?\\d?\\d|6[0-4]\\d\\d\\d|65[0-4]\\d\\d|655[0-2]\\d|6553[0-5])$|^0[xX]\\p{XDigit}{1,4}$", "(?i)A|B|C|D"),
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
    BYTE("^([0-1]?\\d?\\d|2[0-4]\\d|25[0-5])$|^0[xX]\\p{XDigit}{1,2}$");

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