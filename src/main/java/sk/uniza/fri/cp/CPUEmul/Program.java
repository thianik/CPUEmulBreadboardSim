package sk.uniza.fri.cp.CPUEmul;

import javafx.collections.ObservableSet;
import javafx.collections.SetChangeListener;
import org.apache.commons.lang3.ArrayUtils;
import sk.uniza.fri.cp.CPUEmul.Exceptions.NonExistingInterruptLabelException;

import java.util.*;

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
	private TreeMap<Integer, Integer> interruptionLabels; //<cislo prerusenia 0-15, adresa nasledujucej instrukcie>

	/**
	 * prevodnik medzi riadkom v editore a indexom instrukcie v programe pre
	 * zachytenie breaku
	 * <riadok, index instrukcie>
	 */

	private TreeMap<Integer, Integer> lineIndexToInstructionIndex;
	private ArrayList<Byte> memory;
	private ArrayList<Instruction> instructions;


	public Program(ArrayList<Instruction> instructions, ArrayList<Byte> memory, TreeMap<Integer, Integer> lineIndexToInstructionIndex, TreeMap<Integer, Integer> interruptionLabels){
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
	public void setListenerOnBreakpointsChange(ObservableSet<Integer> obsBreakIndexes){
		//vycistenie zoznamu
		breaks.clear();
		//pridanie vsetkych aktualnych breakpointov
		obsBreakIndexes.forEach(index ->
				breaks.add(lineIndexToInstructionIndex.get(index)));

		//registracia listenera na zmenu v breakpointoch
		obsBreakIndexes.addListener(
				(SetChangeListener<Integer>) c ->{
					breaks.clear(); //todo tu da deje nieco co by sa asi nemalo - mnohonasobne volanie
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

	public byte[] getMemory(){
		return ArrayUtils.toPrimitive( memory.toArray(new Byte[0]) );
	}

	public boolean isSetBreak(int instructionAddress){
		return breaks.contains(instructionAddress);
	}

	public int getAddressOfInterrupt(int intNumber) throws NonExistingInterruptLabelException {
	    if(interruptionLabels.containsKey(intNumber)){
	        return interruptionLabels.get(intNumber);
        } else {
            throw new NonExistingInterruptLabelException(Integer.toString(intNumber));
        }
	}

	/**
	 * hlada a vracia riadok instrukcie
	 * @param instructionAddress Index instrukcie ku ktorej sa hlada riaok
	 * @return Index riadku na ktorom je instrukcia, ak sa nenasla, vracia -1
	 */
	public int getLineOfInstruction(int instructionAddress){
		for (Map.Entry<Integer, Integer> entry : lineIndexToInstructionIndex.entrySet()) {
			if (Objects.equals(instructionAddress, entry.getValue())) {
				return entry.getKey();
			}
		}
		return -1;
	}

}