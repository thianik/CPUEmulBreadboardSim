package sk.uniza.fri.cp.CPUEmul;

import javafx.concurrent.Task;

import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.security.InvalidParameterException;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Hlavná trieda CPU emulátora.
 *
 * @author Moris
 * @version 1.0
 * @created 07-feb-2017 18:40:27
 */
public class CPU extends Task<Void> {

	private OutputStream console; // odkaz na konzolu pre vypis sprav
	private Program program;

	/** Flagy */
	private boolean f_async;
	private boolean f_microstep;
	private boolean f_pause;
	//private boolean f_stop; //-> isCanceled()

	/** Synchronizacne nastroje */
	private ReentrantLock lock_asynchro;
	private ReentrantLock lock_halt;

	/** Registre */
	private byte regA;
	private byte regB;
	private byte regC;
	private byte regD;
	private short regMP;
	private short regPC;
	private short regSP;
	private boolean flagCY;
	private boolean flagIE;
	private boolean flagZ;

	/** Pamat */
	private byte[] RAM;
	private byte[] stack; // Zasobnik (LIFO) ma velkost podla velkosti registra SP. Pri 16bit SP je to 65536 bajtov.

	/**
	 * Konstruktor
	 * @param program Program s instrukciami pre vykonanie
	 * @param console Stream na konzolu pre vypis
	 */
	public CPU(Program program, OutputStream console, boolean async, boolean microstep){
		this.program = program;
		this.console = console;

		//flags
		this.f_async = async;
		this.f_microstep = microstep;
		this.f_pause = false;
		//this.f_stop = false;

		//synchro
		this.lock_asynchro = new ReentrantLock();
		this.lock_halt = new ReentrantLock();

		//pamat
		this.RAM = new byte[256];
		this.stack = new byte[65536];
	}

	/**
	 * Vykonavanie programu
	 */
	public Void call(){
        Instruction nextInstruction = null;
		//vykonavaj pokial nie je vlakno ukoncene alebo nie je koniec programu
		while (!isCancelled() && (nextInstruction = program.getInstruction(Short.toUnsignedInt(regPC++))) != null){
			execute(nextInstruction);

		}


		return null;
	}

	/**
	 * Vykonanie konkretnej instrukcie
	 */
	public void execute(Instruction instruction){
		int Rd;
		int Rs;
		int K;
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
			case LMI:
			case LMR:
			case SMI:
			case SMR:	//TODO SPRAVIT INSTRUKCIE KOMUNIKUJUCE SO ZBERNICOU
			case INN:   //TODO Konstrany nemusia byt v desiatkovej sustave
			case OUT:
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
			case SCALL:	//TODO Implementovat SCALL
				break;
		}
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

	/**
	 * Obsluha vykonavania
	 */

	/** Pokracovanie vo vykonavani programu */
	public void continueExecute(){

	}

	public void pause(){

	}

	public void step(){

	}

	public void stop(){

	}

	/**
	 * Nastavenie asynchronneho vykonavania
	 */
	public void setAsync(boolean value){
		this.f_async = value;
	}

	/**
	 * Nastavenie mikro krokovania
	 */
	public void setMicrostep(boolean value){
		this.f_microstep = value;
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
}//end CPU