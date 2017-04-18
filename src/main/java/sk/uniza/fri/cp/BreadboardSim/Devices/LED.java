package sk.uniza.fri.cp.BreadboardSim.Devices;

import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Shape;
import sk.uniza.fri.cp.BreadboardSim.Board.Board;
import sk.uniza.fri.cp.BreadboardSim.Board.GridSystem;
import sk.uniza.fri.cp.BreadboardSim.Devices.Pin.InputPin;
import sk.uniza.fri.cp.BreadboardSim.Devices.Pin.Pin;
import sk.uniza.fri.cp.BreadboardSim.LightEmitter;

import java.util.LinkedList;
import java.util.List;

/**
 *
 * pin 1 - vcc
 * pin 2 - gnd
 *
 * @author Moris
 * @version 1.0
 * @created 17-mar-2017 16:16:35
 */
public class LED extends Device {

    private Color offColor = Color.DARKRED;
    private Color onColor = Color.RED;

    private Shape glowingShape;
    private Group background;

    private InputPin anode;
    private InputPin cathode;

    private volatile boolean on; //zapnuta / vypnuta - update grafiky na FXThread
    private boolean inverseAnodeLogic;
    private boolean inverseCathodeLogic;

    private LightEmitter emitter;

    public LED(Board board) {
        super(board);

        //piny
        this.anode = new InputPin(this);
        this.cathode = new InputPin(this);

        //grafika
        GridSystem grid = board.getGrid();

        glowingShape = new Circle(grid.getSizeMin() / 2.0, offColor);

        this.getChildren().addAll(glowingShape);
    }

    /**
     * @param glowingShape
     */
    public LED(Board board, Shape glowingShape, Color onColor) {
        super(board);
        this.glowingShape = glowingShape;
        this.onColor = onColor;
        this.offColor = (glowingShape.getFill() instanceof Color) ? (Color) glowingShape.getFill() : null;
        this.emitter = new LightEmitter(board, this.glowingShape, this.onColor, this.offColor, 50);
        this.background = new Group();

        //piny
        this.anode = new InputPin(this);
        this.cathode = new InputPin(this);
        this.on = false;

        this.getChildren().addAll(this.background, this.glowingShape);
    }

    /*public Group getBackground() {
        return background;
    }*/

    public Pin getAnode(){
        return this.anode;
    }

    public Pin getCathode(){
        return this.cathode;
    }

    @Override
    public List<Pin> getPins() {
        List<Pin> list = new LinkedList<>();
        list.add(anode);
        list.add(cathode);
        return list;
    }

    public void setInverseAnodeLogic(boolean value) {
        this.inverseAnodeLogic = value;
    }

    public void setInverseCathodeLogic(boolean value) {
        this.inverseCathodeLogic = value;
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