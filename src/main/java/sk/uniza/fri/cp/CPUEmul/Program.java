package sk.uniza.fri.cp.CPUEmul;

import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;

import java.util.ArrayList;
import java.util.TreeMap;
import java.util.TreeSet;

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
	private TreeSet<Integer> breaks;

	/**
	 * navestia pre riadenie preruseni... nemozu sa opakovat
	 */
	private TreeMap<String, Integer> interruptionLabels;

	/**
	 * prevodnik medzi riadkom v editore a indexom instrukcie v programe pre
	 * zachytenie breaku
	 * <riadok, index instrukcie>
	 */

	private TreeMap<Integer, Integer> lineIndexToInstructionIndex;
	private ArrayList<Byte> memory;
	private ArrayList<Instruction> instructions;


	public Program(ArrayList<Instruction> instructions, ArrayList<Byte> memory, TreeMap<Integer, Integer> lineIndexToInstructionIndex, TreeMap<String, Integer> interruptionLabels){
        this.instructions = instructions;
        this.memory = memory;
        this.lineIndexToInstructionIndex = lineIndexToInstructionIndex;
        this.interruptionLabels = interruptionLabels;

        this.breaks = new TreeSet<>();
	}

	/**
	 * Metoda zavadza listenera na zmenu v breakpointoch, ktore prevadza na indexy instrukcii clenskom liste triedy
	 * @param obsBreakIndexes
	 */
	public void setListenerOnBreakpointsChange(ObservableList<Integer> obsBreakIndexes){
		//vycistenie zoznamu
		breaks.clear();
		//pridanie vsetkych aktualnych breakpointov
		obsBreakIndexes.forEach(index ->
				breaks.add(lineIndexToInstructionIndex.get(index)));

		//registracia listenera na zmenu v breakpointoch
		obsBreakIndexes.addListener(
				(ListChangeListener<Integer>) c ->{
					breaks.clear();
					obsBreakIndexes.forEach(index ->
							breaks.add(lineIndexToInstructionIndex.get(index)));
				});
	}

	/**
	 * Vracia instrukciu na adrese (indexe)
	 * @param address Adresa instrukcie v pamati (index instrukcie)
	 */
	public Instruction getInstruction(int address){
		try {
			return instructions.get(address);
		} catch (IndexOutOfBoundsException e){
			return null;
		}
	}

	/**
	 * Vrcia bajt na adrese v pamati programu
	 * @param address Adresa bajtu v pamati programu
	 */
	public byte getByte(int address){
		if(address > memory.size()-1) return 0;
		return memory.get(address);
	}

	public boolean isSetBreak(int instructionAddress){
		return breaks.contains(instructionAddress);
	}

}