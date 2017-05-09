package sk.uniza.fri.cp.BreadboardSim.Devices.Chips;

import javafx.scene.layout.Pane;
import sk.uniza.fri.cp.BreadboardSim.Board.Board;
import sk.uniza.fri.cp.BreadboardSim.Devices.Pin.*;

import java.util.Arrays;

/**
 * Obvod U6264B
 * 8kb pamäť
 *
 * @author Tomáš Hianik
 * @created 14.4.2017.
 */
public class U6264B extends Chip {

    private static final String NAME = "U6264B";
    private static final String SHORT_DESCRIPTION = "8kb pamäť";
    private static final int PINS_COUNT = 28;
    private static final int _NC = 1;
    private static final int _A12 = 2;
    private static final int _A7 = 3;
    private static final int _A6 = 4;
    private static final int _A5 = 5;
    private static final int _A4 = 6;
    private static final int _A3 = 7;
    private static final int _A2 = 8;
    private static final int _A1 = 9;
    private static final int _A0 = 10;
    private static final int _DQ0 = 11;
    private static final int _DQ1 = 12;
    private static final int _DQ2 = 13;
    private static final int _GND = 14;
    private static final int _DQ3 = 15;
    private static final int _DQ4 = 16;
    private static final int _DQ5 = 17;
    private static final int _DQ6 = 18;
    private static final int _DQ7 = 19;
    private static final int _E1_ = 20;
    private static final int _A10 = 21;
    private static final int _G_ = 22;
    private static final int _A11 = 23;
    private static final int _A9 = 24;
    private static final int _A8 = 25;
    private static final int _E2 = 26;
    private static final int _W_ = 27;
    private static final int _VCC = 28;

    private static final int[] addressPins = {_A0, _A1, _A2, _A3, _A4, _A5, _A6, _A7, _A8, _A9, _A10, _A11, _A12};
    private static final int[] dataPins = {_DQ0, _DQ1, _DQ2, _DQ3, _DQ4, _DQ5, _DQ6, _DQ7};

    private final byte[] savedData = new byte[8192];

    public U6264B() {
        super(PINS_COUNT);
    }

    public U6264B(Board board) {
        super(board, PINS_COUNT, 6);
    }

    /**
     * Konstruktor pri kotrom sa da nastavit sirka IC.
     * Z dovodu kompatibility so startym simulatorom.
     *
     * @param board
     */
    public U6264B(Board board, int chipGridHeight) {
        super(board, PINS_COUNT, chipGridHeight);
    }

    private void updateGate() {

        if (this.isLow(_E2) || this.isHigh(_E1_)) {
            Arrays.stream(dataPins).forEach(dataPin -> this.setPin(dataPin, Pin.PinState.HIGH_IMPEDANCE));
            return;
        }

        if (this.isHigh(_W_) && this.isHigh(_G_)) {
            Arrays.stream(dataPins).forEach(dataPin -> this.setPin(dataPin, Pin.PinState.HIGH_IMPEDANCE));
            return;
        }

        if (this.isHigh(_W_) && this.isLow(_G_)) {
            //citanie z pamate
            int data = savedData[decodeAddress()];
            for (int i = 0; i < dataPins.length; i++) {
                if ((data & 1 << i) != 0) { //je tam jednotka
                    this.setPin(dataPins[i], Pin.PinState.HIGH);
                } else { //je tam nula
                    this.setPin(dataPins[i], Pin.PinState.LOW);
                }
            }
            return;
        }

        if (this.isLow(_W_)) {
            //zapis do pamate
            byte data = decodeData();
            this.savedData[decodeAddress()] = data;

            return;
        }

    }

    private int decodeAddress() {
        int address = 0;

        for (int i = 0; i < addressPins.length; i++) {
            if (this.isHigh(addressPins[i]))
                address |= 1 << i;
        }

        return address;
    }

    private byte decodeData() {
        int data = 0;

        for (int i = 0; i < dataPins.length; i++) {
            if (this.isHigh(dataPins[i]))
                data |= 1 << i;
        }

        return (byte) data;
    }

    @Override
    protected void fillPins() {
        registerPin(_NC, new NotConnectedPin(this));
        registerPin(_A12, new InputPin(this, "A12"));
        registerPin(_A7, new InputPin(this, "A7"));
        registerPin(_A6, new InputPin(this, "A6"));
        registerPin(_A5, new InputPin(this, "A5"));
        registerPin(_A4, new InputPin(this, "A4"));
        registerPin(_A3, new InputPin(this, "A3"));
        registerPin(_A2, new InputPin(this, "A2"));
        registerPin(_A1, new InputPin(this, "A1"));
        registerPin(_A0, new InputPin(this, "A0"));
        registerPin(_DQ0, new InputOutputPin(this, "DQ0"));
        registerPin(_DQ1, new InputOutputPin(this, "DQ1"));
        registerPin(_DQ2, new InputOutputPin(this, "DQ2"));
        registerPin(_GND, new OutputPin(this, "VSS"));
        registerPin(_DQ3, new InputOutputPin(this, "DQ3"));
        registerPin(_DQ4, new InputOutputPin(this, "DQ4"));
        registerPin(_DQ5, new InputOutputPin(this, "DQ5"));
        registerPin(_DQ6, new InputOutputPin(this, "DQ6"));
        registerPin(_DQ7, new InputOutputPin(this, "DQ7"));
        registerPin(_E1_, new InputPin(this, "E1_")); // (CE1_)
        registerPin(_A10, new InputPin(this, "A10"));
        registerPin(_G_, new InputPin(this, "G_")); //(OE_)
        registerPin(_A11, new InputPin(this, "A11"));
        registerPin(_A9, new InputPin(this, "A9"));
        registerPin(_A8, new InputPin(this, "A8"));
        registerPin(_E2, new InputPin(this, "E2")); // (CE2)
        registerPin(_W_, new InputPin(this, "W_")); // (WE_)
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
        this.setPin(_DQ0, Pin.PinState.NOT_CONNECTED);
        this.setPin(_DQ1, Pin.PinState.NOT_CONNECTED);
        this.setPin(_DQ2, Pin.PinState.NOT_CONNECTED);
        this.setPin(_DQ3, Pin.PinState.NOT_CONNECTED);
        this.setPin(_DQ4, Pin.PinState.NOT_CONNECTED);
        this.setPin(_DQ5, Pin.PinState.NOT_CONNECTED);
        this.setPin(_DQ6, Pin.PinState.NOT_CONNECTED);
        this.setPin(_DQ7, Pin.PinState.NOT_CONNECTED);

        Arrays.fill(this.savedData, (byte) 0);
    }


    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String getShortStringDescription() {
        return SHORT_DESCRIPTION;
    }

    public Pane getImage() {
        return generateItemImage(NAME, PINS_COUNT, 5);
    }
}
