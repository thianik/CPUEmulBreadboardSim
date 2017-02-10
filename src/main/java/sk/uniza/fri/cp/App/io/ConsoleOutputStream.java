package sk.uniza.fri.cp.App.io;

import org.fxmisc.richtext.InlineCssTextArea;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Created by Moris on 8.2.2017.
 */
public class ConsoleOutputStream extends OutputStream {

    private InlineCssTextArea console;
    private String color;

    public ConsoleOutputStream(InlineCssTextArea console){
        this.console = console;
        this.color = "black";
    }

    public ConsoleOutputStream(InlineCssTextArea console, String color){
        this.console = console;
        this.color = color;
    }

    @Override
    public void write(int b) throws IOException {
        int paragraph = console.getCurrentParagraph();
        console.appendText(String.valueOf((char) b));
        console.setStyle(paragraph, "-fx-fill: " + color);
    }

    @Override
    public void write(byte[] b) throws IOException {
        for (int i = 0; i < b.length - 1; i++){
            write(b);
        }
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        if(off < 0 || off > b.length-1 || len > b.length-1 - off) throw new IOException("Invalid argument");

        for (int i = off; i < len - off; i++){
            write(b);
        }
    }
}
