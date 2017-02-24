package sk.uniza.fri.cp.App.io;

import javafx.application.Platform;
import org.fxmisc.richtext.InlineCssTextArea;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Created by Moris on 8.2.2017.
 */
public class ConsoleOutputStream extends OutputStream {

    private InlineCssTextArea console;
    private String color;
    private boolean isUsed; //indikator, ci bol vypis pouzity

    public ConsoleOutputStream(InlineCssTextArea console){
        this.console = console;
        this.color = "black";
    }

    public ConsoleOutputStream(InlineCssTextArea console, String color){
        this.console = console;
        this.color = color;
    }

    public boolean isUsed(){ return isUsed; }

    private void writeToConsole(int b){
        if(!isUsed) {
            console.setStyle(console.getCurrentParagraph(), "-fx-fill: red");
            console.appendText("[CPU KONZOLA]\n");
            isUsed = true;
        }

        int paragraph = console.getCurrentParagraph();
        if( b == 10) { //LF - novy riadok, rovnaka pozicia
            int carretCol = console.getCaretColumn();
            console.appendText("\n");
            console.appendText(String.format("%" + carretCol + "s", ""));
        } else if( b == 13) { //CR - vratenie na zaciatok riadku
            console.moveTo(console.getCurrentParagraph(), 0);
        } else {
            int pos = console.getCaretPosition();
            console.replaceText(pos, pos+1, String.valueOf((char) b));
        }

        console.setStyle(paragraph, "-fx-fill: " + color + "; -fx-font-family: monospace;");
        console.requestFollowCaret();
    }

    @Override
    public void write(int b) throws IOException {
        if(Platform.isFxApplicationThread()) {
            writeToConsole(b);
        } else {
            Platform.runLater(()->{
                writeToConsole(b);
            });
        }
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
