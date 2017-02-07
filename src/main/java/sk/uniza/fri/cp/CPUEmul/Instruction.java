package sk.uniza.fri.cp.CPUEmul;

/**
 * @author Moris
 * @version 1.0
 * @created 07-feb-2017 18:40:27
 */
public class Instruction {

	private String firstOperand;
	private String secondOperand;
	private enumInstructionsSet type;

	public Instruction(){

	}

	public void finalize() throws Throwable {

	}
	public String getfirstOperand(){
		return firstOperand;
	}

	public String getsecondOperand(){
		return secondOperand;
	}

	public enumInstructionsSet gettype(){
		return type;
	}

	/**
	 * 
	 * @param newVal
	 */
	public void setfirstOperand(String newVal){
		firstOperand = newVal;
	}

	/**
	 * 
	 * @param newVal
	 */
	public void setsecondOperand(String newVal){
		secondOperand = newVal;
	}

	/**
	 * 
	 * @param newVal
	 */
	public void settype(enumInstructionsSet newVal){
		type = newVal;
	}
}//end Instruction