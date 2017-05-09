package sk.uniza.fri.cp.BreadboardSim.Devices;

import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.shape.Shape;
import sk.uniza.fri.cp.BreadboardSim.Board.Board;
import sk.uniza.fri.cp.BreadboardSim.Devices.Pin.InputPin;
import sk.uniza.fri.cp.BreadboardSim.Devices.Pin.Pin;
import sk.uniza.fri.cp.BreadboardSim.LightEmitter;

import java.util.LinkedList;
import java.util.List;

/**
 * LED dióda.
 *
 *
 * @author Tomáš Hianik
 * @version 1.0
 * @created 17-mar-2017 16:16:35
 */
public class LED extends Device {

    private InputPin anode;
    private InputPin cathode;

    private boolean on; //zapnuta / vypnuta
    private boolean inverseAnodeLogic;
    private boolean inverseCathodeLogic;

    private LightEmitter emitter;

    /**
     * Vytvorenie objektu pre plochu simulátora.
     *
     * @param board Plocha simulátora.
     * @param glowingShape Tvar ledky, ktorý má svietiť.
     * @param onColor Farba tvaru pri zapnutí.
     */
    public LED(Board board, Shape glowingShape, Color onColor) {
        super(board);
        Color offColor = (glowingShape.getFill() instanceof Color) ? (Color) glowingShape.getFill() : null;
        this.emitter = new LightEmitter(board, glowingShape, onColor, offColor, 10);
        Group background = new Group();

        //piny
        this.anode = new InputPin(this);
        this.cathode = new InputPin(this);
        this.on = false;

        this.getChildren().addAll(background, glowingShape);
    }

    /**
     * Vráti anódu LED diódy.
     *
     * @return Pin anódy.
     */
    public Pin getAnode(){
        return this.anode;
    }

    /**
     * Vráti katódu LED diódy.
     *
     * @return Pin katódy.
     */
    public Pin getCathode(){
        return this.cathode;
    }

    /**
     * Invertuje logiku anódy. Svieti keď je na anóde hodnota LOW.
     *
     * @param value True - invertovaná logika, false - pôvodná logika.
     */
    public void setInverseAnodeLogic(boolean value) {
        this.inverseAnodeLogic = value;
    }

    /**
     * Invertuje logiku katódy. Svieti keď je na katóde hodnota HIGH.
     *
     * @param value True - invertovaná logika, false - pôvodná logika.
     */
    public void setInverseCathodeLogic(boolean value) {
        this.inverseCathodeLogic = value;
    }

    @Override
    public List<Pin> getPins() {
        List<Pin> list = new LinkedList<>();
        list.add(anode);
        list.add(cathode);
        return list;
    }

    @Override
    public void simulate() {
        if(!isConnected(anode) || !isConnected(cathode)){
            //kontrola zapojenia oboch pinov, ak jeden nie je zapojeny, vypni ledku
            this.on = false;
        } else {
            //splnenie kriterii na zapnutie
            boolean anodeState = this.inverseAnodeLogic != isHigh(anode);
            boolean cathodeState = this.inverseCathodeLogic != isLow(cathode);

            this.on = anodeState && cathodeState;
        }

        if (this.on) this.emitter.turnOn();
        else this.emitter.turnOff();
    }

    @Override
    public void reset() {
        this.on = false;
        this.emitter.turnOff();
    }

    @Override
    public void delete() {
        super.delete();
        this.emitter.delete();
    }
}