package sk.uniza.fri.cp.CPUEmul.Exceptions;

/**
 * Výnimka pri neexistujúcom návestí prerušenia.
 *
 * @author Tomáš Hianik
 * @created 21.2.2017.
 */
public class NonExistingInterruptLabelException extends Exception {

    /**
     * Vytvorenie výnimky s číslom prerušenia v hexadecimálnej sústave.
     *
     * @param intNumber Číslo prerušenia od 0 do F.
     */
    public NonExistingInterruptLabelException(String intNumber) {
        super("Neexistujúce návestie prerušenia INT0" + intNumber);
    }
}
