package sk.uniza.fri.cp.BreadboardSim.Devices.Chips.Gates;

import javafx.scene.layout.Pane;
import sk.uniza.fri.cp.BreadboardSim.Board;
import sk.uniza.fri.cp.BreadboardSim.Devices.Chips.Chip;
import sk.uniza.fri.cp.BreadboardSim.InputPin;
import sk.uniza.fri.cp.BreadboardSim.OutputPin;
import sk.uniza.fri.cp.BreadboardSim.Pin;

/**
 * Created by Moris on 29.3.2017.
 */
public class Gen7400 extends Chip {

    private static final String NAME = "7400";
    private static final String DESCRIPTION = "QUADRUPLE 2-INPUT POSITIVE-NAND GATES";
    private static final int PINS_COUNT = 14;
    private static final int _1A = 1;
    private static final int _1B = 2;
    private static final int _1Y = 3;
    private static final int _2A = 4;
    private static final int _2B = 5;
    private static final int _2Y = 6;
    private static final int _GND = 7;
    private static final int _3Y = 8;
    private static final int _3A = 9;
    private static final int _3B = 10;
    private static final int _4Y = 11;
    private static final int _4A = 12;
    private static final int _4B = 13;
    private static final int _VCC = 14;

    public Gen7400(){}

    public Gen7400(Board board) {
        super(board, PINS_COUNT);
    }

    private void updateGate(int A, int B, int Y){
        if(this.isHigh(A) && this.isHigh(B)){
            this.setPin(Y, Pin.PinState.LOW);
        } else {
            this.setPin(Y, Pin.PinState.HIGH);
        }
    }

    @Override
    protected void fillPins() {
        registerPin(_1A, new InputPin(this, "1A"));
        registerPin(_1B, new InputPin(this));
        registerPin(_1Y, new OutputPin(this));
        registerPin(_2A, new InputPin(this));
        registerPin(_2B, new InputPin(this));
        registerPin(_2Y, new OutputPin(this));
        registerPin(_GND, new InputPin(this));
        registerPin(_3Y, new OutputPin(this));
        registerPin(_3A, new InputPin(this));
        registerPin(_3B, new InputPin(this));
        registerPin(_4Y, new OutputPin(this));
        registerPin(_4A, new InputPin(this));
        registerPin(_4B, new InputPin(this));
        registerPin(_VCC, new InputPin(this));
    }

    @Override
    public void simulate() {
        if(isPowered(_VCC, _GND)){
            this.updateGate(_1A, _1B, _1Y);
            this.updateGate(_2A, _2B, _2Y);
            this.updateGate(_3A, _3B, _3Y);
            this.updateGate(_4A, _4B, _4Y);
        } else {
            this.setPin(_1Y, Pin.PinState.NOT_CONNECTED);
            this.setPin(_2Y, Pin.PinState.NOT_CONNECTED);
            this.setPin(_3Y, Pin.PinState.NOT_CONNECTED);
            this.setPin(_4Y, Pin.PinState.NOT_CONNECTED);
        }
    }

    @Override
    public void reset() {

    }


    @Override
    public String getName() {
        return NAME;
    }

    public Pane getImage(){
        return generateItemImage(NAME, PINS_COUNT);
    }
}
