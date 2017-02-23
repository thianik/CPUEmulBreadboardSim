package sk.uniza.fri.cp.CPUEmul.Exceptions;

/**
 * Created by Moris on 21.2.2017.
 */
public class NonExistingInterruptLabelException extends Exception {

    public NonExistingInterruptLabelException() {
        super("Neexistujúce návestie prerušenia!");
    }

    public NonExistingInterruptLabelException(String intNumber) {
        super("Neexistujúce návestie prerušenia INT0" + intNumber);
    }
}
