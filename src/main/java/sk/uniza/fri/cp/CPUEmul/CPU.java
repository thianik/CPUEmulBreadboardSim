package sk.uniza.fri.cp.CPUEmul;

import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.control.Alert;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import sk.uniza.fri.cp.Bus.Bus;
import sk.uniza.fri.cp.CPUEmul.Exceptions.NonExistingInterruptLabelException;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.security.InvalidParameterException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;

/**
 * Emulátor procesora použitého pri výučbe predmetu Číslicové počítače na fakulte riadenia a informatiky.
 * Pred spustením vykonávania programu je potrebné parsovaný program zaviesť.
 * //TODO hlbsi popis
 * @author Tomáš Hianik
 * @version 1.0
 * @created 07-feb-2017 18:40:27
 */
public class CPU extends Thread {

    /** Konstanty */
    private static final int SYNCH_WAIT_TIME_MS = 10;     //cas cakania na nastavenie dat pri synchronnej komunikacii

	private OutputStream console; //vystupny stream pre vypis znakov na terminal
	private Program program; //zavedeny program s instrukciami a konstantami programu

	/** Flagy */
    //vonkajsi pristup
    volatile private boolean isCancelled; //zrusenie vlakna
    volatile private boolean isExecuting; //spustenie/zastavenie vykonavania CPU
    volatile private boolean f_microstep; //povolenie mikrokrokovania
    volatile private boolean f_int_level; //priznak, ci je prerusenie vyvolane od urovne (ak nie tak od zmeny)
    volatile private boolean f_pause;    //pozastavenie vykonavania CPU
    //vnutorne
    private boolean f_int_level_old;    //minula uroven
    private boolean f_eit;  //priznak, ci boli v predchadzajuciej instrukcii povolene prerusenia

	private ObjectProperty<CPUStates> state;    //status CPU
    volatile private String message; //správa cpu o vykonávaní (povodne Task, ktory mal updateMessage)

    private int UsbITCheckSkipped = 0; //preskocenie kazdych x cyklov pred citanim IT cez USB (USB je prilis pomale)

	/** Synchronizacne nastroje */
	private CountDownLatch cdlHalt; //pasivne cakanie pri pozastaveni vykonavania alebo cakani na spustenie
    private Semaphore semKey;   //semafor na pristup ku stlacenej klavese

	/** Registre */
	volatile private byte regA;
    volatile private byte regB;
    volatile private byte regC;
    volatile private byte regD;
    volatile private short regMP;
    volatile private short regPC;
    volatile private short regSP;
    volatile private boolean flagCY;
    volatile private boolean flagIE;
    volatile private boolean flagZ;

	/** Pamat */
	private byte[] RAM;
	private byte[] stack; // Zasobnik (LIFO) ma velkost podla velkosti registra SP. Pri 16bit SP je to 65536 bajtov.
    private KeyEvent key;

    /** Zbernica */
    private Bus bus;

    /**
     * Konštruktor prijíma OutputStream, na ktorý sa vypisujú znaky pomocou inštrukcie SCALL a zbernicu pomocou
     * ktorej komunikuje s vývojovou doskou.
     *
     * @param console OutputStream pre výpis znakov na terminál.
     * @param bus Zbernica pre komunikáciu s vývojovou doskou.
     */
	public CPU(OutputStream console, Bus bus){
		this.console = console;

		state = new SimpleObjectProperty<>(CPUStates.Paused);

		//synchro
		//this.cdlHalt = new CountDownLatch(1);
        this.semKey = new Semaphore(0);

		//pamat
		this.RAM = new byte[256];
		this.stack = new byte[65536];

		//zbernica
        this.bus = bus;

        this.setName("CPU_Thread");
	}

	/**
	 * Spustenie vykonávania vlákna.
     * Obsahuje dve slučky. Vonkajšia pre beh vlákna s CPU emulátorom a vnútorná pre beh programu.
	 */
	public void run() {

        //pokial nie je cele vlakno zrusene
        threadLoop:
        while(!isCancelled) {
            state.setValue(CPUStates.Idle);
            //cakaj na spustenie vykonavania
            while(!isExecuting){
                try {
                    haltAwait();
                } catch (InterruptedException e) {
                    //ak bolo vlakno ukoncene, opusti hlavny loop
                    if(this.isCancelled) break threadLoop;
                }
            }

            Instruction nextInstruction;
            boolean usbConnected;
            boolean it = false; //interrupt

            state.setValue(CPUStates.Running);

            bus.setRandomAddress();
            bus.setRandomData();

            //vykonavaj pokial vlakno nebolo zrusene, je spustene vykonavanei a nie je koniec programu
            while (!isCancelled
                    && isExecuting
                    && (nextInstruction = program.getInstruction(Short.toUnsignedInt(regPC++))) != null) {
                try {
                    //kontrola nastavenia breaku v programe
                    if (program.isSetBreak(regPC - 1) || f_pause) {
                        state.setValue(CPUStates.Paused);
                        this.f_pause = true;
                        haltAwait();
                    }

                    state.setValue(CPUStates.Running);

                    //povolenie preruseni ak predchadzajuca instrukcia bola EIT
                    if (f_eit) {
                        flagIE = true;
                        f_eit = false;
                    }

                    //vykonanie instrukcie
                    execute(nextInstruction);

                    //kontrola vyvolania prerusenia
                    usbConnected = bus.isUsbConnected();
                    //ak nie je pripojenie cez USB alebo bolo vykonanych 16 cyklov (USB komunikacia je prilis pomala)
                    if (!usbConnected || UsbITCheckSkipped >= 16) {
                        it = bus.isIT();
                        UsbITCheckSkipped = 0;
                    } else UsbITCheckSkipped++;

                    //ak je povolene prerusenie a aj vyvolane
                    if (flagIE && it) {
                        if (f_int_level) { //preusenie od urovne
                            handleInterrupt();
                        } else { //prerusenie od zmeny
                            if (!f_int_level_old) {   //ak bola nulova
                                handleInterrupt();
                            }
                        }
                    }
                    f_int_level_old = it;

                } catch (Exception e ) {
                    //ak doslo k vynimke

                    //ak nastala vynimka z dovodu neexistujuceho navestia prerusenia
                    if(e instanceof NonExistingInterruptLabelException) {
                        Platform.runLater(()->{
                            Alert alert = new Alert(Alert.AlertType.ERROR);
                            alert.setTitle("Chyba");
                            alert.setContentText(e.getMessage());
                            alert.setHeaderText("Chyba prerušenia");
                            alert.show();
                        });
                    }

                    this.isExecuting = false;
                }
            }
            this.isExecuting = false;
        }
        System.out.println("CPU koniec");

	}

    /**
     * Začatie vykonávania programu, ak je zavedený.
     */
    public void startExecute(boolean startPaused){
        if(!this.isExecuting && this.program != null) {
            this.regPC = 0;
            this.isExecuting = true;
            this.f_pause = startPaused;
            if (this.cdlHalt != null) cdlHalt.countDown();
        }
    }

    /**
     * Pozastavenie vykonávania CPU.
     */
    public void pauseExecute(){
        f_pause = true;
    }


    /**
     * Pokračovanie vo vykonávaní programu, ak bolo pozastavené.
     */
    public void continueExecute(){
        if(this.f_pause) {
            f_pause = false;
            cdlHalt.countDown();
        }
	}

	/**
     * Skok na ďalšiu inštrukciu v programe.
     */
    public void step(){
        f_pause = true; //pri krokovani zastavit pred vykonanim dalsej inst.
        cdlHalt.countDown();
    }

    /**
     * Ukončenie vykonávania zavedeného programu.
     */
    public void stopExecute(){
        if(this.isExecuting) {
            this.isExecuting = false;
            if(this.cdlHalt != null) cdlHalt.countDown();
        }
    }

    /**
     * Ukončenie vykonávania vlákna s CPU.
     */
    public void cancel(){
        this.isCancelled = true;
        //if(this.cdlHalt != null) this.cdlHalt.countDown();
        interrupt();
	}

    /**
     * Resetovanie stavu CPU.
     */
    public void reset(){
        regA = regB = regC = regD = 0;
        regMP = regPC = regSP = 0;
        flagCY = flagIE = flagZ = false;

        for (int i = 0; i < RAM.length; i++) RAM[i] = 0;
        for (int i = 0; i < stack.length; i++) stack[i] = 0;

        if (semKey.availablePermits() > 0)
            semKey.drainPermits();
    }

    /**
     * Zavedenie nového programu určeného na vykonávanie.
     * @param programToLoad Program, ktorý ma CPU vykonávať.
     */
    public void loadProgram(Program programToLoad){
        this.program = programToLoad;
    }

    /**
     * Vráti hodnotu všeobecného registra A.
     * @return Aktuálna hodnota registra A.
     */
    public byte getRegA() {
        return regA;
    }

    /**
     * Vráti hodnotu všeobecného registra B.
     * @return Aktuálna hodnota registra B.
     */
    public byte getRegB() {
		return regB;
	}

    /**
     * Vráti hodnotu všeobecného registra C.
     * @return Aktuálna hodnota registra C.
     */
    public byte getRegC() {
		return regC;
	}

    /**
     * Vráti hodnotu všeobecného registra D.
     * @return Aktuálna hodnota registra D.
     */
    public byte getRegD() {
		return regD;
	}

    /**
     * Vráti hodnotu registra ukazujúceho na pamäť.
     * @return Aktuálna hodnota registra MP.
     */
    public short getRegMP() {
		return regMP;
	}

    /**
     * Vráti hodnotu registra obsahujúceho adresu nasledujúcej inštrukcie.
     * @return Aktuálna hodnota registra PC.
     */
    public short getRegPC() {
        return regPC;
    }

    /**
     * Vráti hodnotu registra ukazujúceho na vrchol zásobníka.
     * @return Aktuálna hodnota registra SP.
     */
    public short getRegSP() {
		return regSP;
	}

    /**
     * Vráti príznak Carry.
     * @return Aktuálna hodnota príznaku Carry.
     */
    public boolean isFlagCY() {
        return flagCY;
    }

    /**
     * Vráti príznak interruption enable.
     * @return Aktuálna hodnota príznaku IE.
     */
    public boolean isFlagIE() {
        return flagIE;
    }

    /**
     * Vráti príznak Zero.
     * @return Aktuálna hodnota príznaku Zero.
     */
    public boolean isFlagZ() {
        return flagZ;
    }

    /**
     * Vráti obsah špeciálnej pamäte procesora.
     * @return Obsah pamäte.
     */
    public byte[] getRAM() {
        synchronized (this) {
            return RAM.clone();
        }
	}

    /**
     * Vráti obsah zásobníka.
     * @return Obsah zásobníka.
     */
    public byte[] getStack() {
        synchronized (this) {
            return stack.clone();
        }
	}

    /**
     * Nastavenie stlačenej klávesy pre inštrukciu scall.
     * @param event Event vyvolaný stlačenou klávesou.
     */
    public void setKeyPressed(KeyEvent event){
        synchronized (this){
            this.key = event;
        }

        if(semKey.availablePermits() == 0)
            semKey.release();
    }

    /**
     * Nastavenie spôsobu vyvolania prerušenia.
     * @param intByLevel True - prerušenie podľa úrovne, false - prerušenie od zmeny
     */
    public void setIntLevel(boolean intByLevel){
        this.f_int_level = intByLevel;
    }

    /**
     * Povolenie alebo zakázanie mikrokrokovania.
     * @param value True ak má byť mikrokrokovanie povolené, false ak má byť zakázané.
     */
    public void setMicrostep(boolean value){
        this.f_microstep = value;
    }

    public Property<CPUStates> stateProperty(){
        return state;
    }

    /**
     * Správa o stave, v ktorom sa procesor nachádza.
     *
     * @return Správa o stave procesora.
     */
    public String getMessage(){
        return this.message;
    }

    /**
     * Vykonanie konkretnej instrukcie
     */
    private void execute(Instruction instruction) throws InterruptedException {
        int Rd;
        int Rs;
        int K;
        short addr;

        switch (instruction.getType()){
            case ADD:
                Rd = getRegisterVal(instruction.getFirstParameter());
                Rs = getRegisterVal(instruction.getSecondParameter());
                Rd += Rs;
                setRegisterVal(instruction.getFirstParameter(), Rd);
                flagZ = ((Rd & 0xFF) == 0);
                flagCY = (Rd > 255);
                break;
            case ADC:
                Rd = getRegisterVal(instruction.getFirstParameter());
                Rs = getRegisterVal(instruction.getSecondParameter());
                Rd += Rs + (flagCY?1:0);
                setRegisterVal(instruction.getFirstParameter(), Rd);
                flagZ = ((Rd & 0xFF) == 0);
                flagCY = (Rd > 255);
                break;
            case ADI:
                Rd = getRegisterVal(instruction.getFirstParameter());
                K = Parser.parseConstant(instruction.getSecondParameter());
                Rd += K;
                setRegisterVal(instruction.getFirstParameter(), Rd);
                flagZ = ((Rd & 0xFF) == 0);
                flagCY = (Rd > 255);
                break;
            case SUB:
                Rd = getRegisterVal(instruction.getFirstParameter());
                Rs = getRegisterVal(instruction.getSecondParameter());
                Rd -= Rs;
                setRegisterVal(instruction.getFirstParameter(), Rd);
                flagZ = ((Rd & 0xFF) == 0);
                flagCY = (Rd < 0);
                break;
            case SUC:
                Rd = getRegisterVal(instruction.getFirstParameter());
                Rs = getRegisterVal(instruction.getSecondParameter());
                Rd -= Rs + (flagCY?1:0);
                setRegisterVal(instruction.getFirstParameter(), Rd);
                flagZ = ((Rd & 0xFF) == 0);
                flagCY = (Rd < 0);
                break;
            case SBI:
                Rd = getRegisterVal(instruction.getFirstParameter());
                K = Parser.parseConstant(instruction.getSecondParameter());
                Rd -= K;
                setRegisterVal(instruction.getFirstParameter(), Rd);
                flagZ = ((Rd & 0xFF) == 0);
                flagCY = (Rd < 0);
                break;
            case AND:
                Rd = getRegisterVal(instruction.getFirstParameter());
                Rs = getRegisterVal(instruction.getSecondParameter());
                Rd &= Rs;
                setRegisterVal(instruction.getFirstParameter(), Rd);
                flagZ = (Rd == 0);
                break;
            case ANI:
                Rd = getRegisterVal(instruction.getFirstParameter());
                K = Parser.parseConstant(instruction.getSecondParameter());
                Rd &= K;
                setRegisterVal(instruction.getFirstParameter(), Rd);
                flagZ = (Rd == 0);
                break;
            case ORR:
                Rd = getRegisterVal(instruction.getFirstParameter());
                Rs = getRegisterVal(instruction.getSecondParameter());
                Rd |= Rs;
                setRegisterVal(instruction.getFirstParameter(), Rd);
                flagZ = (Rd == 0);
                break;
            case ORI:
                Rd = getRegisterVal(instruction.getFirstParameter());
                K = Parser.parseConstant(instruction.getSecondParameter());
                Rd |= K;
                setRegisterVal(instruction.getFirstParameter(), Rd);
                flagZ = (Rd == 0);
                break;
            case XOR:
                Rd = getRegisterVal(instruction.getFirstParameter());
                Rs = getRegisterVal(instruction.getSecondParameter());
                Rd ^= Rs;
                setRegisterVal(instruction.getFirstParameter(), Rd);
                flagZ = (Rd == 0);
                break;
            case XRI:
                Rd = getRegisterVal(instruction.getFirstParameter());
                K = Parser.parseConstant(instruction.getSecondParameter());
                Rd ^= K;
                setRegisterVal(instruction.getFirstParameter(), Rd);
                flagZ = (Rd == 0);
                break;
            case INC:
                Rd = getRegisterVal(instruction.getFirstParameter());
                setRegisterVal(instruction.getFirstParameter(), ++Rd);
                flagCY = flagZ = ((Rd & 0xFF) == 0);
                break;
            case INX:
                Rd = getRegisterVal(instruction.getFirstParameter());
                setRegisterVal(instruction.getFirstParameter(), ++Rd);
                flagCY = flagZ = ((Rd & 0xFFFF) == 0);
                break;
            case DEC:
                Rd = getRegisterVal(instruction.getFirstParameter());
                flagCY = ((Rd & 0xFF) == 0);
                setRegisterVal(instruction.getFirstParameter(), --Rd);
                flagZ = ((Rd & 0xFF) == 0);
                break;
            case DCX:
                Rd = getRegisterVal(instruction.getFirstParameter());
                flagCY = ((Rd & 0xFFFF) == 0);
                setRegisterVal(instruction.getFirstParameter(), --Rd);
                flagZ = ((Rd & 0xFFFF) == 0);
                break;
            case CMP:
                Rd = getRegisterVal(instruction.getFirstParameter());
                Rs = getRegisterVal(instruction.getSecondParameter());
                flagZ = ((Rd - Rs) == 0);
                flagCY = ((Rd - Rs) < 0);
                break;
            case CMI:
                Rd = getRegisterVal(instruction.getFirstParameter());
                K = Parser.parseConstant(instruction.getSecondParameter());
                flagZ = ((Rd - K) == 0);
                flagCY = ((Rd - K) < 0);
                break;
            //posun a rotacia
            case SHL:
                Rd = getRegisterVal(instruction.getFirstParameter());
                K = Integer.parseInt(instruction.getSecondParameter());
                Rd <<= K;
                setRegisterVal(instruction.getFirstParameter(), Rd);
                flagZ = ((Rd & 0xFF) == 0);
                break;
            case SHR:
                Rd = getRegisterVal(instruction.getFirstParameter());
                K = Integer.parseInt(instruction.getSecondParameter());
                Rd >>= K;
                setRegisterVal(instruction.getFirstParameter(), Rd);
                flagZ = ((Rd & 0xFF) == 0);
                break;
            case SCR:
                Rd = getRegisterVal(instruction.getFirstParameter());
                K = Integer.parseInt(instruction.getSecondParameter());
                flagCY = ((Rd & (1<<(K-1))) != 0);
                Rd >>= K;
                setRegisterVal(instruction.getFirstParameter(), Rd);
                flagZ = ((Rd & 0xFF) == 0);
                break;
            case RTL:
                Rd = getRegisterVal(instruction.getFirstParameter());
                K = Integer.parseInt(instruction.getSecondParameter());
                Rd = (Rd << K) | (Rd >> (8-K));
                setRegisterVal(instruction.getFirstParameter(), Rd);
                flagZ = ((Rd & 0xFF) == 0);
                break;
            case RCL:
                Rd = getRegisterVal(instruction.getFirstParameter());
                K = Integer.parseInt(instruction.getSecondParameter());
                Rd = (Rd << K) | (Rd >> (8-K));
                flagCY = ((Rd & 1) == 1);
                setRegisterVal(instruction.getFirstParameter(), Rd);
                flagZ = ((Rd & 0xFF) == 0);
                break;
            case RTR:
                Rd = getRegisterVal(instruction.getFirstParameter());
                K = Integer.parseInt(instruction.getSecondParameter());
                Rd = (Rd >> K) | (Rd << (8-K));
                setRegisterVal(instruction.getFirstParameter(), Rd);
                flagZ = ((Rd & 0xFF) == 0);
                break;
            case RCR:
                Rd = getRegisterVal(instruction.getFirstParameter());
                K = Integer.parseInt(instruction.getSecondParameter());
                Rd = (Rd >> K) | (Rd << (8-K));
                flagCY = ((Rd & 0x80) != 0);
                setRegisterVal(instruction.getFirstParameter(), Rd);
                flagZ = ((Rd & 0xFF) == 0);
                break;
            case MOV:
                setRegisterVal(instruction.getFirstParameter(), getRegisterVal(instruction.getSecondParameter()));
                break;
            case MVI:
            case MXI:
                setRegisterVal(instruction.getFirstParameter(), Parser.parseConstant( instruction.getSecondParameter() ));
                break;
            case MVX:
                if(instruction.getFirstParameter().toUpperCase().equals("C")){
                    Rs = getRegisterVal(instruction.getSecondParameter());
                    int L = Rs & 0xFF;
                    int H = Rs >> 8;
                    setRegisterVal("C", H);
                    setRegisterVal("D", L);
                } else {
                    int val = getRegisterVal("B") * 256 | getRegisterVal("A");
                    setRegisterVal(instruction.getFirstParameter(), val);
                }
                break;
            case MMR:
                Rs = getRegisterVal(instruction.getSecondParameter());
                setRegisterVal(instruction.getFirstParameter(), program.getByte(Rs));
                break;
            case LMI:   //nacitanie pamate do registra s puzitim priamej adresy
                addr = (short) Parser.parseConstant(instruction.getSecondParameter());
                read(enumInstructionsSet.LMI, addr, instruction.getFirstParameter());
                break;
            case LMR:   //nacitanie pamate do registra s puzitim adresy v reg MP
                read(enumInstructionsSet.LMR, regMP, instruction.getFirstParameter());
                break;
            case SMI:
                addr = (short) Parser.parseConstant(instruction.getFirstParameter());
                write(enumInstructionsSet.SMI, addr, (byte) getRegisterVal(instruction.getSecondParameter()));
                break;
            case SMR:
                write(enumInstructionsSet.SMR, regMP, (byte) getRegisterVal(instruction.getFirstParameter()));
                break;
            case INN:
                addr = (short) Parser.parseConstant(instruction.getSecondParameter());
                read(enumInstructionsSet.INN, addr, instruction.getFirstParameter());
                break;
            case OUT:
                addr = (short) Parser.parseConstant(instruction.getFirstParameter());
                write(enumInstructionsSet.OUT, addr, (byte) getRegisterVal(instruction.getSecondParameter()));
                break;
            case PUS:
                if (instruction.getFirstParameter().matches("(?i)M")) {
                    push((short) getRegisterVal(instruction.getFirstParameter()));
                } else {
                    push((byte) getRegisterVal(instruction.getFirstParameter()));
                }
                break;
            case POP:
                if (instruction.getFirstParameter().matches("(?i)M")) {
                    setRegisterVal(instruction.getFirstParameter(), popShort());
                } else {
                    setRegisterVal(instruction.getFirstParameter(), pop());
                }
                break;
            case STR:
                Rd = getRegisterVal(instruction.getFirstParameter());
                Rs = getRegisterVal(instruction.getSecondParameter());
                RAM[Rd] = (byte) Rs;
                break;
            case LDR:
                Rs = getRegisterVal(instruction.getSecondParameter());
                setRegisterVal(instruction.getFirstParameter(), RAM[Rs]);
                break;
            //Instrukcie vetvenia
            case JMP:
                regPC = (short) Integer.parseUnsignedInt(instruction.getFirstParameter());
                break;
            case JZR:
                if(flagZ) regPC = (short) Integer.parseUnsignedInt(instruction.getFirstParameter());
                break;
            case JNZ:
                if(!flagZ) regPC = (short) Integer.parseUnsignedInt(instruction.getFirstParameter());
                break;
            case JCY:
                if(flagCY) regPC = (short) Integer.parseUnsignedInt(instruction.getFirstParameter());
                break;
            case JNC:
                if(!flagCY) regPC = (short) Integer.parseUnsignedInt(instruction.getFirstParameter());
                break;
            case CAL:
                push(regPC);
                regPC = (short) Integer.parseUnsignedInt(instruction.getFirstParameter());
                break;
            case CZR:
                if(flagZ) {
                    push(regPC);
                    regPC = (short) Integer.parseUnsignedInt(instruction.getFirstParameter());
                }
                break;
            case CNZ:
                if(!flagZ) {
                    push(regPC);
                    regPC = (short) Integer.parseUnsignedInt(instruction.getFirstParameter());
                }
                break;
            case CCY:
                if(flagCY) {
                    push(regPC);
                    regPC = (short) Integer.parseUnsignedInt(instruction.getFirstParameter());
                }
                break;
            case CNC:
                if(!flagCY) {
                    push(regPC);
                    regPC = (short) Integer.parseUnsignedInt(instruction.getFirstParameter());
                }
                break;
            case RET:
                regPC = popShort();
                break;
            //Specialne instrukcie
            case EIT:
                f_eit = true;
                break;
            case DIT:
                flagIE = false;
                break;
            case SCALL:
                switch(instruction.getFirstParameter().toUpperCase()){
                    case "KEY":
                        if(!semKey.tryAcquire()){
                            //cakanie na stlacenie klavesy
                            updateMessage("Stlacte klavesu");
                            state.setValue(CPUStates.Waiting);
                            semKey.acquire();
                            state.setValue(CPUStates.Running);
                        }

                        synchronized (this) {
                            if (key.getCode().isLetterKey() || key.getCode().isDigitKey())
                                setRegisterVal("d", key.getText().charAt(0));
                            else if(key.getCode() == KeyCode.ENTER)
                                setRegisterVal("d", 13);
                            else
                                setRegisterVal("d", key.getCode().impl_getCode());
                        }

                        break;
                    case "KPR":
                        flagCY = semKey.availablePermits() > 0;
                        break;
                    case "DSP":
                        try {
                            console.write(regD);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        break;
                    default:
                }
                break;
        }
    }

    /**
     * Obsluha hw prerušenia ak nastalo.
     *
     * @throws NonExistingInterruptLabelException Výnimka, ak neexistuje načítané číslo prerušenia.
     */
    private void handleInterrupt() throws NonExistingInterruptLabelException {
        //vypnutie preruseni
        flagIE = false;

        //synchronizovane citanie
        bus.setIA_(false);
        this.waitForSteadySimulation(null);
        byte data = bus.getDataBus();
        bus.setIA_(true);

        //odlozenie PC na zasobnik
        push(regPC);

        //nastavenie adresy instrukcie prerusenia
        int intNumber = data & 0x0f;
        regPC = (short) program.getAddressOfInterrupt(intNumber);
    }

    /**
     * Vrátenie hodnoty registra na základe názvu registra.
     *
     * @param regName Názov registra.
     * @return Hodnota uložená v registri.
     */
    private int getRegisterVal(String regName){
        switch (regName.toUpperCase()){
            case "A": return Byte.toUnsignedInt(regA);
            case "B": return Byte.toUnsignedInt(regB);
            case "C": return Byte.toUnsignedInt(regC);
            case "D": return Byte.toUnsignedInt(regD);
            case "M": return Short.toUnsignedInt(regMP);
            case "S": return Short.toUnsignedInt(regSP);
            case "PC": return Short.toUnsignedInt(regPC);
        }
        throw new InvalidParameterException("Neplatny register");
    }

    /**
     * Nastavenie hodnoty registra na základe názvu registra.
     *
     * @param regName Názov registra.
     * @param value Nová hodnota, ktorá má byť uložená do registra. Ak je väčšia ako register, oreže sa.
     */
    private void setRegisterVal(String regName, int value){
        switch (regName.toUpperCase()){
            case "A": regA = (byte) value; break;
            case "B": regB = (byte) value; break;
            case "C": regC = (byte) value; break;
            case "D": regD = (byte) value; break;
            case "M": regMP = (short) value; break;
            case "S": regSP = (short) value; break;
            case "PC": regPC = (short) value; break;
            default: throw new InvalidParameterException("Neplatny register");
        }
    }

    /**
     * Pridanie hodnoty typu byte na vrchol zásobníka s aktualizáciou registra SP.
     *
     * @param value Hodnota, ktorá sa pridá na vrchol zásobníka.
     */
    private void push(byte value){
        int SP = Short.toUnsignedInt(regSP);
        stack[SP--] = value;
        regSP = (short) SP;
    }

    /**
     * Pridanie hodnoty typu short na vrchol zásobníka s aktualizáciou registra SP.
     * Ako prvý sa uloží horný bajt, až potom spodný bajt.
     *
     * @param value Hodnota, ktorá sa pridá na vrchol zásobníka.
     */
    private void push(short value){
        int L = value & 0xFF;
        int H = value >> 8 & 0xFF;
        push((byte)H);
        push((byte)L);
    }

    /**
     * Výber hodnoty typu byte z vrcholu zásobíka s aktualizáciou registra SP.
     *
     * @return Hodnota na vrchole zásobníka.
     */
    private byte pop(){
        int SP = Short.toUnsignedInt(regSP);
        if(SP == 65535) SP = 0;
        else SP++;
        regSP = (short) SP;
        return stack[SP];
    }

    /**
     * Výber hodnoty typu short z vrcholu zásobíka s aktualizáciou registra SP.
     * Pre výber hodnoty typu short sa spoja dve hodnoty z vrcholu zásobníka, spodný bajt sa zoberie ako prvý,
     * vrchý bajt ako druhý.
     *
     * @return Hodnota na vrchole zásobníka.
     */
    private short popShort(){
        int L = Byte.toUnsignedInt(pop());
        int H = Byte.toUnsignedInt(pop())<<8;
        return (short) (H|L);
    }

    /**
     * Ak je zapnuté mikrokrokovanie, metóda aktualizuje správu CPU, stav a pozastaví vykonávanie.
     *
     * @param msg Telo správy pre stavový riadok.
     * @throws InterruptedException Výnimka pri prerušení počas čakania na pokračovanie vykonávania.
     */
    private void microstepAwait(String msg) throws InterruptedException {
        //ak je zapnute mikrokrokovanie a zaroven sa aj krokuje
        if (isExecuting && f_microstep && f_pause) {
            //String syncMethod ="Synchronne - ";
            //updateMessage(syncMethod + msg);
            updateMessage(msg);
            state.setValue(CPUStates.MicroStep);
            haltAwait();
            state.setValue(CPUStates.Running);
        }
    }

    /**
     * Metóda zaobstaravajúca čítanie z externého zariadenia pripojeného k vývojovej doske.
     *
     * @param inst Inštrukcia, ktorá vyvolala čítanie
     * @param addr Adresa, z ktorej sa má čítať
     * @param destRegName Názov reigstra, kam sa má načítaná hodnota uložiť
     * @throws InterruptedException Prerušenie počas synchrónnej komunikácie alebo mikrokrokovania
     */
    private void read(enumInstructionsSet inst, short addr, String destRegName) throws InterruptedException {
        //nastavenie adresy
        microstepAwait("Nastavenie adresy");
        bus.setAddressBus(addr);

        microstepAwait("Nastavenie priznaku " + (inst == enumInstructionsSet.INN?"IOR":"MR") + " = 0");
        /*if(f_async){ //ak je nastavene asynchronne vykonavanie
            //nastavenie priznaku citania do nuly
            if(inst == enumInstructionsSet.INN)
                bus.setIR_(false);
            else
                bus.setMR_(false);

            //cakaj na RDY
            updateMessage("Cakanie na RY");
            readyAwait();
            microstepAwait("Cakanie na RY - prijate");

        } else {*/
            //synchronne vykonavanie
            //nastavenie priznaku citania
        System.out.println("CPU NASTAVENIE PRIZNAKU CITANIA");
            if(inst == enumInstructionsSet.INN)
                bus.setIR_(false);
            else
                bus.setMR_(false);

            //cakanie na nastavenie dat na datovej zbernici
            updateMessage("Cakanie na nastavenie dat");
        System.out.println("CPU CAKANIE NA STEADY STATE PRE CITANIE DAT");
        this.waitForSteadySimulation(inst);
        //}

        //nacitaj data
        microstepAwait("Nacitanie dat");
        System.out.println("CPU CITANIE DAT");
        byte Rd = bus.getDataBus();
        setRegisterVal(destRegName, Rd);

        //zrus priznak citania
        microstepAwait("Zrusenie priznaku " + (inst == enumInstructionsSet.INN?"IOR":"MR"));
        System.out.println("CPU ODNASTAVENIE PRIZNAKU CITANIA");
        if(inst == enumInstructionsSet.INN)
            bus.setIR_(true);
        else
            bus.setMR_(true);

        //zrus adresu
        microstepAwait("Zrusenie adresy");
        bus.setRandomAddress();
    }

    /**
     * Metóda zaobstaravajúca čítanie z externého zariadenia pripojeného k vývojovej doske
     *
     * @param inst Inštrukcia, ktorá vyvolala zápis
     * @param addr Adresa, na ktorú sa má zapisovať
     * @param data Dáta, ktoré sa majú na adresu zapísať
     * @throws InterruptedException Prerušenie počas sycnhrónneho zápisu alebo čakania pri mikrokrokovaní
     */
    private void write(enumInstructionsSet inst, short addr, byte data) throws InterruptedException {
        //nastavenie adresy
        microstepAwait("Nastavenie adresy");
        bus.setAddressBus(addr);

        //nastavenie dat
        microstepAwait("Nastavenie dat");
        System.out.println("CPU NASTAVENIE DAT PRI ZAPISE");
        bus.setDataBus(data);

        //nastavenie priznaku
        microstepAwait("Nastavenie priznaku " + (inst == enumInstructionsSet.OUT?"IOW":"MW") + " = 0");
        /*if(f_async){ //asynchronne
            //nastavenie priznaku
            if (inst == enumInstructionsSet.OUT)
                bus.setIW_(false);
            else
                bus.setMW_(false);

            //cakaj na RDY
            updateMessage("Cakanie na RY");
            readyAwait();
            microstepAwait("Cakanie na RY - prijate");
        } else {*/
            //synchronne
            //nastavenie priznaku
        System.out.println("CPU NASTAVENIE PRIZNAKU ZAPISU");
            if (inst == enumInstructionsSet.OUT)
                bus.setIW_(false);
            else
                bus.setMW_(false);

            updateMessage("Cakanie na nastavenie dat");
        System.out.println("CPU CAKANIE NA STEADY STATE PO NASTAVENI PRIZNAKU ZAPISU");
        this.waitForSteadySimulation(inst);
        //}

        //zrusenie priznaku
        microstepAwait("Zrusenie priznaku ZAPISU");
        System.out.println("CPU ZRUSENIE PRIZNAKU");
        if(inst == enumInstructionsSet.OUT)
            bus.setIW_(true);
        else
            bus.setMW_(true);

        System.out.println("CPU CAKANIE NA STEADY STATE PO ZRUSENI PRIZNAKU");
        this.waitForSteadySimulation(inst);

        //zrusenie dat
        microstepAwait("Zrusenie dat");
        System.out.println("CPU ZRUSENIE DAT");
        bus.setRandomData();

        //zrusenie adresy
        microstepAwait("Zruesnie adresy");
        bus.setRandomAddress();
    }

    /**
     * Pasívne čakanie pri pozastavení vykonávania.
     *
     * @throws InterruptedException Prerušenie počas čakania
     */
    private void haltAwait() throws InterruptedException {
        cdlHalt = new CountDownLatch(1);
        cdlHalt.await();
        cdlHalt = null;
    }

    /**
     * Pasívne čakanie na nastavenie dát na dátovej zbernici pre ich čítanie.
     *
     * @param inst Inštrukcia, ktorá požaduje čítanie.
     */
    private void waitForSteadySimulation(enumInstructionsSet inst) {
        try {
            if (!bus.waitForSteadyState()) {
                //data nie su nastavene (simulator nebezi alebo nie je sekunda na nastavenie dat dostacujuca)
                try {
                    console.write((" " + ((inst != null) ? inst.toString() : "IT")
                            + ": Data nie su nastavene! Spustite prosim simulaciu a uistite sa, ze nedochadza k zacykleniu. ")
                            .getBytes(Charset.forName("UTF-8")));
                    console.write(10);
                    console.write(13);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Aktualizácia správy procesora okoliu.
     * @param msg Nová správa
     */
    private void updateMessage(String msg){
        this.message = msg;
    }

}//end CPU