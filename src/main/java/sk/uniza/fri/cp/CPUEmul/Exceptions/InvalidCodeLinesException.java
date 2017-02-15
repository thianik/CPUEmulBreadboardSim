package sk.uniza.fri.cp.CPUEmul.Exceptions;

/**
 * Created by Moris on 8.2.2017.
 */
public class InvalidCodeLinesException extends Exception {

    private int[] lines;
    private String[] errors;

    public InvalidCodeLinesException(int[] lines, String[] errors, String msg){
        super(msg);
        this.lines = lines.clone();
        this.errors = errors;
    }

    public InvalidCodeLinesException(int[] lines, String[] errors){
        super("Some lines of code are invalid!");
        this.lines = lines.clone();
        this.errors = errors;
    }

    @Override
    public String toString() {
        String out = "";
        for (String str :
                errors) {
            out += str + "\n";
        }

        /*
        String out = "Lines [";
        for (int line: lines) {
            out += line + ",";
        }
        out = out.substring(0, out.length()-2);
        out += "] are invalid";
        */
        return out;
    }

    public int[] getLines() {
        return lines;
    }

    public String[] getErrors() {
        return errors;
    }
}
