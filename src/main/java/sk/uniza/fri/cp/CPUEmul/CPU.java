package sk.uniza.fri.cp.CPUEmul;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.concurrent.Task;
import javafx.scene.input.KeyEvent;
import sk.uniza.fri.cp.Bus.Bus;
import sk.uniza.fri.cp.Bus.BusSimulated;
import sk.uniza.fri.cp.CPUEmul.Exceptions.NonExistingInterruptLabelException;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.security.InvalidParameterException;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Hlavná trieda CPU emulátora.
  * TODO SYNCHRONIZACIA!!!! ... f_key, key
 * @author Moris
 * @version 1.0
 * @created 07-feb-2017 18:40:27
 */
public class CPU extends Task<Void> {

    /** Konstanty */
    private final int SYNCH_WAIT_TIME_MS = 50;     //cas cakania na nastavenie dat pri synchronnej komunikacii
    //private final int ASYNCH_WAIT_TIMEOUT_MS = 500; //cas max. cakania na zmenu signalu RY pri asynchronnej komukinacii

	private OutputStream console; // odkaz na konzolu pre vypis sprav
	private Program program;

	/** Flagy */
    //vonkajsi pristup
	volatile private boolean f_async;
    volatile private boolean f_microstep;
    volatile private boolean f_int_level; //priznak, ci je prerusenie vyvolane od urovne (ak nie tak od zmeny)
    private boolean f_key; //priznak, ci bola stlacena klavesa  //nastavuje aj CPU Thread aj Application Thread
    //vnutorne
    private boolean f_int_level_old;    //minula uroven
    private boolean f_pause;

	private ObjectProperty<CPUStates> state;    //status CPU

	/** Synchronizacne nastroje */
	private CountDownLatch cdlHalt;
	private CountDownLatch cdlRY;   //cakanie na signal RDY

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
	volatile private byte[] RAM;
	volatile private byte[] stack; // Zasobnik (LIFO) ma velkost podla velkosti registra SP. Pri 16bit SP je to 65536 bajtov.
    private KeyEvent key;

    /** Zbernica */
    private Bus bus;
	/**
	 * Konstruktor
	 * @param program Program s instrukciami pre vykonanie
	 * @param console Stream na konzolu pre vypis
	 */
	public CPU(Program program, OutputStream console, boolean async, boolean intByLevel, boolean microstep){
		this.program = program;
		this.console = console;

		//flags
		this.f_async = async;
		this.f_microstep = microstep;
		this.f_pause = false;
		this.f_int_level = intByLevel;

		state = new SimpleObjectProperty<>(CPUStates.Paused);

		//synchro
		this.cdlHalt = new CountDownLatch(1);
        this.cdlRY = new CountDownLatch(1);
        this.semKey = new Semaphore(0);

		//pamat
		this.RAM = new byte[256];
		this.stack = new byte[65536];

		//zbernica
        this.bus = BusSimulated.getBus();   //TODO Zmenit simulovany Bus v CPU pri nasadeni
	}

	/**
	 * Vykonavanie programu
	 */
	public Void call() throws NonExistingInterruptLabelException {
        Instruction nextInstruction = null;
        boolean it = false; //interrupt

        state.setValue(CPUStates.Running);
		//vykonavaj pokial nie je vlakno ukoncene alebo nie je koniec programu
		while (!isCancelled() && (nextInstruction = program.getInstruction(Short.toUnsignedInt(regPC++))) != null){
            try {
                if(program.isSetBreak(regPC-1) || f_pause){
                    state.setValue(CPUStates.Paused);
                    haltAwait();
                }

                state.setValue(CPUStates.Running);
                execute(nextInstruction);

                //ak je povolene prerusenie a aj vyvolane
                it = bus.isIT();
                if(flagIE && it){
                    if (f_int_level) { //preusenie od urovne
                        handleInterrupt();
                    } else { //prerusenie od zmeny
                        if (!f_int_level_old) {   //ak bola nulova
                            handleInterrupt();
                        }
                    }
                }
                f_int_level_old = it;

            } catch (InterruptedException e) {
                if(isCancelled()) return null;
            }

        }

		return null;
	}

    /**
	 * Obsluha vykonavania
	 */

	/** Pokracovanie vo vykonavani programu */
    public void continueExecute(){
	    f_pause = false;
        cdlHalt.countDown();
	}

    public void pause(){
        f_pause = true;
	}

    public void step(){
	    f_pause = true; //pri krokovani zastavit pred vykonanim dalsej inst.
        cdlHalt.countDown();
	}

    public void stop(){
        this.cancel();
	}

    public void reset(){
        regA = regB = regC = regD = 0;
        regMP = regPC = regSP = 0;
        flagCY = flagIE = flagZ = f_key =  false;

        for (int i = 0; i < RAM.length; i++) RAM[i] = 0;
        for (int i = 0; i < stack.length; i++) stack[i] = 0;
    }

    public byte getRegA() {
        return regA;
    }

    public byte getRegB() {
		return regB;
	}

    public byte getRegC() {
		return regC;
	}

    public byte getRegD() {
		return regD;
	}

    public short getRegMP() {
		return regMP;
	}

    public short getRegPC() {
        return regPC;
    }

    public short getRegSP() {
		return regSP;
	}

    public boolean isFlagCY() {
        return flagCY;
    }

    public boolean isFlagIE() {
        return flagIE;
    }

    public boolean isFlagZ() {
        return flagZ;
    }

    public byte[] getRAM() {
		return RAM;
	}

    public byte[] getStack() {
		return stack;
	}

    public byte[] getProgMemory() {
        return program.getMemory();
    }

    public void setKeyPressed(KeyEvent event){
        synchronized (this){
            this.key = event;
        }

        if(semKey.availablePermits() == 0)
            semKey.release();
    }

    public void setAsync(boolean value){
        this.f_async = value;
    }

    public void setIntLevel(boolean intByLevel){
        f_int_level = intByLevel;
    }

    public void setMicrostep(boolean value){
        this.f_microstep = value;
    }

    public Property<CPUStates> statesProperty(){
        return state;
    }



    /**
     * Vykonanie konkretnej instrukcie
     */
    private void execute(Instruction instruction) throws InterruptedException {
        int Rd;
        int Rs;
        int K;
        short addr = 0;

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
                Rd -= Rs - (flagCY?1:0);
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
                flagCY = ((Rd & (1<<(K-1))) == 1);							//TODO riadne overit posuvy a rotacie
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
                flagCY = ((Rd & 0x80) == 1);
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
                if(instruction.getFirstParameter().toUpperCase() == "C"){
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
            case LMI:   //nacitanie pamate do registra s opuzitim priamej adresy
                addr = (short) Parser.parseConstant(instruction.getSecondParameter());
                read(enumInstructionsSet.LMI, addr, instruction.getFirstParameter());
                break;
            case LMR:   //nacitanie pamate do registra s opuzitim adresy v reg MP
                read(enumInstructionsSet.LMI, regMP, instruction.getFirstParameter());
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
                read(enumInstructionsSet.INN, addr, instruction.getSecondParameter());
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
                flagIE = true;
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
                        }

                        synchronized (this) {
                            if (key.getCode().isLetterKey() || key.getCode().isDigitKey())
                                setRegisterVal("d", key.getText().charAt(0));
                            else
                                setRegisterVal("d", key.getCode().impl_getCode());
                        }

                        break;
                    case "KPR":
                        if(semKey.availablePermits() > 0) flagCY = true;
                        else flagCY = false;
                        break;
                    case "DSP":
                        try {
                            console.write(regD);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        break;
                }
                break;
        }
    }

    private void handleInterrupt() throws InterruptedException, NonExistingInterruptLabelException {
        //vypnutie preruseni
        flagIE = false;

        //synchronizovane citanie
        bus.setIA_(false);
        Thread.sleep(SYNCH_WAIT_TIME_MS);
        byte data = bus.getDataBus();
        bus.setIA_(true);

        //odlozenie PC na zasobnik
        push(regPC);

        //nastavenie adresy instrukcie prerusenia
        int intNumber = data & 0x0f;
        regPC = (short) program.getAddressOfInterrupt(intNumber);
    }

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

    private void push(byte value){
        int SP = Short.toUnsignedInt(regSP);
        stack[SP--] = value;
        regSP = (short) SP;
    }

    private void push(short value){
        int L = value & 0xFF;
        int H = value >> 8 & 0xFF;
        push((byte)H);
        push((byte)L);
    }

    private byte pop(){
        int SP = Short.toUnsignedInt(regSP);
        if(SP == 65535) SP = 0;
        else SP++;
        regSP = (short) SP;
        return stack[SP];
    }

    private short popShort(){
        int L = Byte.toUnsignedInt(pop());
        int H = Byte.toUnsignedInt(pop())<<8;
        return (short) (H|L);
    }

    private void microstepAwait(String msg) throws InterruptedException {
        if(f_microstep) {
            String syncMethod = f_async?"Asynchronne - ":"Synchronne - ";
            updateMessage(syncMethod + msg);
            state.setValue(CPUStates.MicroStep);
            haltAwait();
            state.setValue(CPUStates.Running);
        }
    }

    private void read(enumInstructionsSet inst, short addr, String destRegName) throws InterruptedException {
        //nastavenie adresy
        microstepAwait("Nastavenie adresy");
        bus.setAddressBus(addr);

        microstepAwait("Nastavenie priznaku " + (inst == enumInstructionsSet.INN?"IOR":"MR") + " = 0");
        if(f_async){ //ak je nastavene asynchronne vykonavanie
            //nastavenie priznaku citania do nuly
            if(inst == enumInstructionsSet.INN)
                bus.setIR_(false, cdlRY);
            else
                bus.setMR_(false, cdlRY);

            //cakaj na RDY
            updateMessage("Cakanie na RY");
            readyAwait();
            microstepAwait("Cakanie na RY - prijate");

        } else { //synchronne vykonavanie
            //nastavenie priznaku citania
            if(inst == enumInstructionsSet.INN)
                bus.setIR_(false);
            else
                bus.setMR_(false);

            //cakanie na nastavenie dat na datovej zbernici
            updateMessage("Cakanie na nastavenie dat");
            Thread.sleep(SYNCH_WAIT_TIME_MS);
        }

        //nacitaj data
        microstepAwait("Nacitanie dat");
        byte Rd = bus.getDataBus();
        setRegisterVal(destRegName, Rd);

        //zrus priznak citania
        microstepAwait("Zrusenie priznaku " + (inst == enumInstructionsSet.INN?"IOR":"MR"));
        if(inst == enumInstructionsSet.INN)
            bus.setIR_(true);
        else
            bus.setMR_(true);

        //zrus adresu
        microstepAwait("Zrusenie adresy");
        bus.setRandomAddress();
    }

    private void write(enumInstructionsSet inst, short addr, byte data) throws InterruptedException {
        //nastavenie adresy
        microstepAwait("Nastavenie adresy");
        bus.setAddressBus(addr);

        //nastavenie dat
        microstepAwait("Nastavenie dat");
        bus.setDataBus(data);

        //nastavenie priznaku
        microstepAwait("Nastavenie priznaku " + (inst == enumInstructionsSet.OUT?"IOW":"MW") + " = 0");
        if(f_async){ //asynchronne
            //nastavenie priznaku
            if (inst == enumInstructionsSet.OUT)
                bus.setIW_(false, cdlRY);
            else
                bus.setMW_(false, cdlRY);

            //cakaj na RDY
            updateMessage("Cakanie na RY");
            readyAwait();
            microstepAwait("Cakanie na RY - prijate");
        } else { //synchronne
            //nastavenie priznaku
            if (inst == enumInstructionsSet.OUT)
                bus.setIW_(false);
            else
                bus.setMW_(false);

            updateMessage("Cakanie na nastavenie dat");
            Thread.sleep(SYNCH_WAIT_TIME_MS);
        }

        //zrusenie priznaku
        microstepAwait("Zrusenie priznaku");
        if(inst == enumInstructionsSet.OUT)
            bus.setIW_(true);
        else
            bus.setMW_(true);

        //zrusenie dat
        microstepAwait("Zrusenie dat");
        bus.setRandomData();

        //zrusenie adresy
        microstepAwait("Zruesnie adresy");
        bus.setRandomAddress();
    }

    private void haltAwait() throws InterruptedException {
	    cdlHalt.await();
	    cdlHalt = new CountDownLatch(1);
    }

    private void readyAwait() throws InterruptedException {
	    cdlRY.await();
	    cdlRY = new CountDownLatch(1);
    }

}//end CPU