package sk.uniza.fri.cp.CPUEmul;

import java.io.OutputStream;

/**
 * Parsuje prikazy z editoru do pola pre vykonavanie
 * @author Moris
 * @version 1.0
 * @created 07-feb-2017 18:40:27
 */
public class Parser {

	public Instruction m_Instruction;
	public Program m_Program;

	public Parser(){

	}

	public void finalize() throws Throwable {

	}
	/**
	 * 
	 * @param code
	 * @param consoleStream
	 * @param CPUController
	 */
	public static Program parseCode(String code, OutputStream consoleStream, CPUController CPUController){
		return null;
	}
}//end Parser