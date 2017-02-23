package sk.uniza.fri.cp.App.io;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;

/**
 * TODO PREROBIT ConsoleWriter a Stream PODLA KONVENCII A UNIVERZALNE
 *
 *
 * Created by Moris on 8.2.2017.
 */
public class ConsolePrintWriter extends PrintWriter {

    OutputStream os;

    public ConsolePrintWriter(OutputStream out) {
        super(out);
        os = out;
    }

    public ConsolePrintWriter(OutputStream out, boolean autoFlush) {
        super(out, autoFlush);
    }

    @Override
    public void print(String s) {
        for (char ch :
                s.toCharArray()) {
            try {
                os.write(ch);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void println(String x) {
        print(x + "\n");
    }
}
