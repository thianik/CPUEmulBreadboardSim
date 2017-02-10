package sk.uniza.fri.cp.App;

/**
 * OBsahuje mozne stavy zobrazenie stavov registrov a obsahu zasobnika
 * Dec
 * Hexa
 * Bin
 * Ascii
 * @author Moris
 * @version 1.0
 * @created 07-feb-2017 18:40:27
 */
public enum enumDisplayOptions {
    D('D'), H('H'), B('B'), A('A');

    private char character;

    enumDisplayOptions(char ch){
        character = ch;
    }

    @Override
    public String toString() {
        return String.valueOf(character);
    }
}