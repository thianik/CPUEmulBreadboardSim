package sk.uniza.fri.cp.CPUEmul;

/**
 * @author Moris
 * @version 1.0
 * @created 07-feb-2017 18:40:27
 */
public class Instruction {

	private String firstParameter;
	private String secondParameter;
	private enumInstructionsSet type;

	public Instruction(enumInstructionsSet typeOfInstruction){
		this.type = typeOfInstruction;
		this.firstParameter = null;
		this.secondParameter = null;
	}

	public Instruction(enumInstructionsSet typeOfInstruction, String firstParameter){
		this.type = typeOfInstruction;
		this.firstParameter = firstParameter;
		this.secondParameter = null;
	}

	public Instruction(enumInstructionsSet typeOfInstruction, String firstParameter, String secondParameter){
		this.type = typeOfInstruction;
		this.firstParameter = firstParameter;
		this.secondParameter = secondParameter;
	}

	public String getFirstParameter() {
		return firstParameter;
	}

	public void setFirstParameter(String firstParameter) {
		this.firstParameter = firstParameter;
	}

	public String getSecondParameter() {
		return secondParameter;
	}

	public void setSecondParameter(String secondParameter) {
		this.secondParameter = secondParameter;
	}

	public enumInstructionsSet getType(){
		return type;
	}

}//end Instruction