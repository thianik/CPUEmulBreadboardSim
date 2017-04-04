package sk.uniza.fri.cp.App.CPUControl.io;

import javafx.application.Platform;
import org.fxmisc.richtext.InlineCssTextArea;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Výstupný stream ako obal pre CPU konzolu aka. InlineCssTextArea.
 *
 * @author Tomáš Hianik
 * @created  8.2.2017.
 * @version 0.3
 */
public class ConsoleOutputStream extends OutputStream {

    private InlineCssTextArea console;
    private String color;
    private boolean isUsed; //indikator, ci bol vypis od posledneho nastavenia pouzity

    /**
     * Vytvorenie výstupého streamu nad InlineCssTextArea.
     *
     * @param console Konzola, do ktorej sa bude presmerovávať výstup.
     */
    public ConsoleOutputStream(InlineCssTextArea console){
        this.console = console;
        this.color = "black";
    }

    /**
     * Vytvorenie výstupného streamu nad InlineCssTextArea s určenou farbou písma.
     *
     * @param console Konzola, do ktorej sa bude presmerovávať výstup.
     * @param color Farba písama.
     */
    public ConsoleOutputStream(InlineCssTextArea console, String color){
        this.console = console;
        this.color = color;
    }

    /**
     * Bol už tento stream od poseldného nastavenia použitý?
     * Používa sa pri výpise na konzolu pred a po spustení CPU, aby sa zistilo či niečo vypísal.
     *
     * @return Truea ak bol použitý, false inak.
     */
    public boolean isUsed(){ return isUsed; }

    /**
     * Nastavenie, že stream ešte nebol použitý. Pred spustením vykonávania programu na CPU.
     */
    public void setUnused(){ this.isUsed = false; }

    /**
     * Výpis znaku na koznolu.
     * Ak sa jedná o prvý znak, vypíše sa najprv reťazec [CPU KONZOLA].
     *
     * @param b Znak, ktorý sa má vypísať na konzolu.
     */
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
            write(b[i]);
        }
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        if(off < 0 || off > b.length-1 || len > b.length-1 - off) throw new IOException("Invalid argument");

        for (int i = off; i < len - off; i++){
            write(b[i]);
        }
    }
}
