package sk.uniza.fri.cp.CPUEmul;

import java.util.ArrayList;
import java.util.TreeMap;

/**
 * Uchovava instrukcie programu, navestia, breaky a umoznuje k nim pristup
 * @author Moris
 * @version 1.0
 * @created 07-feb-2017 18:40:27
 */
public class Program {

	/**
	 * indexy instrukcii na ktorych je break
	 */
	private TreeMap<Integer, Integer> breaks;
	/**
	 * navestia pre riadenie preruseni... nemozu sa opakovat
	 */
	private TreeMap<String, Integer> interuptionLabels;
	/**
	 * prevodnik medzi riadkom v editore a indexom instrukcie v programe pre
	 * zachytenie breaku
	 * <riadok, index instrukcie>
	 */
	private TreeMap<Integer, Integer> lineOfInstruction;
	private ArrayList<Byte> memory;
	private ArrayList<Instruction> instructions;


	public Program(){

	}

	/**
	 * 
	 * @param instruction
	 * @param lineInCode
	 */
	public void addInstruction(Instruction instruction, int lineInCode){

	}

	public void clearBreaks(){

	}

	/**
	 * 
	 * @param address
	 */
	public void getByte(int address){

	}

	/**
	 * 
	 * @param address
	 */
	public void getInstruction(int address){

	}

	/**
	 * 
	 * @param value
	 */
	public void saveByte(byte value){

	}

	/**
	 * 
	 * @param lineInCode
	 */
	public boolean setBreak(int lineInCode){
		return false;
	}

	/**
	 * 
	 * @param lineInCode
	 */
	public boolean unsetBreak(int lineInCode){
		return false;
	}
}//end Program