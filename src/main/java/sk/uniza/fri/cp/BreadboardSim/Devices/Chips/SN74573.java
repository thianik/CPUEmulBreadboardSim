package sk.uniza.fri.cp.BreadboardSim.Devices.Chips;

import javafx.application.Platform;
import javafx.scene.layout.Pane;
import sk.uniza.fri.cp.BreadboardSim.Board.Board;
import sk.uniza.fri.cp.BreadboardSim.Devices.Pin.InputPin;
import sk.uniza.fri.cp.BreadboardSim.Devices.Pin.OutputPin;
import sk.uniza.fri.cp.BreadboardSim.Devices.Pin.Pin;
import sk.uniza.fri.cp.Bus.Bus;

import java.util.Arrays;

/**
 * Created by Moris on 13.4.2017.
 */
public class SN74573 extends Chip {

    private static final String NAME = "SN74573";
    private static final String DESCRIPTION = "8bit register";
    private static final int PINS_COUNT = 20;
    private static final int _OE_ = 1;
    private static final int _1D = 2;
    private static final int _2D = 3;
    private static final int _3D = 4;
    private static final int _4D = 5;
    private static final int _5D = 6;
    private static final int _6D = 7;
    private static final int _7D = 8;
    private static final int _8D = 9;
    private static final int _GND = 10;
    private static final int _LE = 11;
    private static final int _8Q = 12;
    private static final int _7Q = 13;
    private static final int _6Q = 14;
    private static final int _5Q = 15;
    private static final int _4Q = 16;
    private static final int _3Q = 17;
    private static final int _2Q = 18;
    private static final int _1Q = 19;
    private static final int _VCC = 20;

    private static final int[] inputs = {_1D, _2D, _3D, _4D, _5D, _6D, _7D, _8D};
    private static final int[] outputs = {_1Q, _2Q, _3Q, _4Q, _5Q, _6Q, _7Q, _8Q};
    private final boolean[] savedData = new boolean[8];

    public SN74573() {
        super(PINS_COUNT);
    }

    public SN74573(Board board) {
        super(board, PINS_COUNT);
    }

    private void updateGate() {
        if (this.isHigh(_LE)) {
            //debug
            int data = 0;

            //povoleny zapis
            for (int i = 0; i < 8; i++) {
                this.savedData[i] = this.isHigh(inputs[i]);
                data += this.savedData[i] ? 1 << (7 - i) : 0;
            }

//            System.out.println("Zapisane data na chip: " + data + " \t\t\t\t\t\t\t\t\t\t" + Thread.currentThread().getName());

        }

        if (this.isHigh(_OE_)) {
            Arrays.stream(outputs).forEach(output -> this.setPin(output, Pin.PinState.HIGH_IMPEDANCE));
        } else if (this.isLow(_OE_)) {
            //debug
            int data = 0;

            //povoleny vystup
            for (int i = 0; i < 8; i++) {
                this.setPin(outputs[i], savedData[i] ? Pin.PinState.HIGH : Pin.PinState.LOW);
                data += this.savedData[i] ? 1 << (7 - i) : 0;
            }

//            if(!Bus.getBus().isIA_())
//            System.out.println("Nastavene data chipom: " + data + " \t\t\t\t\t\t\t\t\t\t" + Thread.currentThread().getName());
        }

    }

    @Override
    protected void fillPins() {
        registerPin(_OE_, new InputPin(this, "OE_"));
        registerPin(_1D, new InputPin(this, "1D"));
        registerPin(_2D, new InputPin(this, "2D"));
        registerPin(_3D, new InputPin(this, "3D"));
        registerPin(_4D, new InputPin(this, "4D"));
        registerPin(_5D, new InputPin(this, "5D"));
        registerPin(_6D, new InputPin(this, "6D"));
        registerPin(_7D, new InputPin(this, "7D"));
        registerPin(_8D, new InputPin(this, "8D"));
        registerPin(_GND, new InputPin(this, "GND"));
        registerPin(_LE, new InputPin(this, "LE"));
        registerPin(_8Q, new OutputPin(this, Pin.PinDriver.TRI_STATE, "8Q"));
        registerPin(_7Q, new OutputPin(this, Pin.PinDriver.TRI_STATE, "7Q"));
        registerPin(_6Q, new OutputPin(this, Pin.PinDriver.TRI_STATE, "6Q"));
        registerPin(_5Q, new OutputPin(this, Pin.PinDriver.TRI_STATE, "5Q"));
        registerPin(_4Q, new OutputPin(this, Pin.PinDriver.TRI_STATE, "4Q"));
        registerPin(_3Q, new OutputPin(this, Pin.PinDriver.TRI_STATE, "3Q"));
        registerPin(_2Q, new OutputPin(this, Pin.PinDriver.TRI_STATE, "2Q"));
        registerPin(_1Q, new OutputPin(this, Pin.PinDriver.TRI_STATE, "1Q"));
        registerPin(_VCC, new InputPin(this, "VCC"));
    }

    @Override
    public void simulate() {
        if (isPowered(_VCC, _GND)) {
            this.updateGate();
        } else {
            reset();
        }
    }

    @Override
    public void reset() {
        this.setPin(_1Q, Pin.PinState.NOT_CONNECTED);
        this.setPin(_2Q, Pin.PinState.NOT_CONNECTED);
        this.setPin(_3Q, Pin.PinState.NOT_CONNECTED);
        this.setPin(_4Q, Pin.PinState.NOT_CONNECTED);
        this.setPin(_5Q, Pin.PinState.NOT_CONNECTED);
        this.setPin(_6Q, Pin.PinState.NOT_CONNECTED);
        this.setPin(_7Q, Pin.PinState.NOT_CONNECTED);
        this.setPin(_8Q, Pin.PinState.NOT_CONNECTED);

        for (int i = 0; i < 8; i++) {
            this.savedData[i] = false;
        }
    }


    @Override
    public String getName() {
        return NAME;
    }

    public String getStringDescription() {
        return DESCRIPTION;
    }

    public Pane getImage() {
        return generateItemImage(NAME, PINS_COUNT);
    }
}
