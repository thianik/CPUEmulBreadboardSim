package sk.uniza.fri.cp.CPUEmul.Exceptions;

/**
 * Created by Moris on 8.2.2017.
 */
public class InvalidCodeLinesException extends Exception {

    int[] lines;

    public InvalidCodeLinesException(int[] lines, String msg){
        super(msg);
        this.lines = lines.clone();
    }

    public InvalidCodeLinesException(int[] lines){
        super("Some lines of code are invalid!");
        this.lines = lines.clone();
    }

    @Override
    public String toString() {
        String out = "Lines [";
        for (int line: lines) {
            out += line + ",";
        }
        out = out.substring(0, out.length()-2);
        out += "] are invalid";
        return out;
    }

    public int[] getLines() {
        return lines;
    }
}
