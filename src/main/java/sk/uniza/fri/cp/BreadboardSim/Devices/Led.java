package sk.uniza.fri.cp.BreadboardSim.Devices;

import javafx.application.Platform;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Shape;
import sk.uniza.fri.cp.BreadboardSim.*;

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
public class Led extends Device {

    private Color offColor = Color.DARKRED;
    private Color onColor = Color.RED;

    private Shape glowingShape;
    private Group background;

    private InputPin anode;
    private InputPin cathode;

    private volatile boolean on; //zapnuta / vypnuta - update grafiky na FXThread
    private boolean inverseAnodeLogic;
    private boolean inverseCathodeLogic;

    public Led(Board board) {
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
    public Led(Board board, Shape glowingShape, Color onColor) {
        super(board);
        this.glowingShape = glowingShape;
        this.onColor = onColor;
        this.offColor = (glowingShape.getFill() instanceof Color) ? (Color) glowingShape.getFill() : null;
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

    //TODO update rychlo blikajucich lediek
    private long currentTime;
    private long lastChangeTime;
    private boolean taskRunning;

    @Override
    public void simulate() {
        //kontrola zapojenia oboch pinov, ak jeden nie je zapojeny, vypni ledku
        if(!isConnected(anode) || !isConnected(cathode)){
            //ak bola vypnuta, vrat sa
            if(!this.on) return;

            this.on = false;
            updateGraphic();
            return;
        }

        //splnenie kriterii na zapnutie
        boolean anodeState = this.inverseAnodeLogic != isHigh(anode);
        boolean cathodeState = this.inverseCathodeLogic != isLow(cathode);

        this.on = anodeState && cathodeState;

//        currentTime = System.currentTimeMillis();
//        if(!this.on && !taskRunning && currentTime - lastChangeTime < 500){
//            return;
//        }

        updateGraphic();
        //lastChangeTime = System.currentTimeMillis();
    }

    @Override
    public void reset() {
        this.on = false;
        updateGraphic();
    }

    @Override
    protected void updateGraphic(){
        super.updateGraphic();

        if(this.on){
            glowingShape.setFill(onColor);
        } else {
            glowingShape.setFill(offColor);
        }
    }
}