package sk.uniza.fri.cp.CPUEmul;

import javafx.concurrent.Task;

import java.io.OutputStream;
import java.util.ArrayDeque;
import java.util.ArrayList;
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
	private boolean f_stop;

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
	private byte regState;

	/** Pamat */
	private ArrayList<Byte> RAM;
	private ArrayDeque<Byte> stack; // Zasobnik (LIFO) ma velkost podla velkosti registra SP. Pri 16bit SP je to 65536 bajtov.

	/**
	 * Konstruktor
	 * @param program
	 * @param console
	 */
	public CPU(Program program, OutputStream console){
		this.program = program;
		this.console = console;

		//stack = new ArrayDeque<Byte>(65536);
	}

	public Void call(){
		return null;
	}

	/**
	 * Vykonanie konkretnej instrukcie
	 * @param instruction
	 */
	public void execute(Instruction instruction){

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
	 * Gettery pre priznaky
	 * */
	public boolean getCY(){
		return false;
	}

	public boolean getIE(){
		return false;
	}

	public boolean getZ(){
		return false;
	}

	/**
	 * Nastavenie asynchronneho vykonavania
	 * @param value
	 */
	public void setAsync(boolean value){

	}

	/**
	 * Nastavenie priznakov
	 */
	private void setCY(boolean value){

	}

	private void setIE(boolean value){

	}

	private void setZ(boolean value){

	}

	/**
	 * Nastavenie mikro krokovania
	 * @param value
	 */
	public void setMicrostep(boolean value){

	}

}//end CPU