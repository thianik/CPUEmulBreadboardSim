package sk.uniza.fri.cp.BreadboardSim.Devices.Chips;

import javafx.scene.layout.Pane;
import sk.uniza.fri.cp.BreadboardSim.Board.Board;
import sk.uniza.fri.cp.BreadboardSim.Devices.Pin.InputPin;
import sk.uniza.fri.cp.BreadboardSim.Devices.Pin.OutputPin;
import sk.uniza.fri.cp.BreadboardSim.Devices.Pin.Pin;

/**
 * Created by Moris on 6.5.2017.
 */
public class SN74164 extends Chip {

    private static final String NAME = "SN74164";
    private static final String DESCRIPTION = "8-BIT PARRALEL-OUT SERIAL SHIFT REGISTERS";
    private static final int PINS_COUNT = 14;
    private static final int _A = 1;
    private static final int _B = 2;
    private static final int _QA = 3;
    private static final int _QB = 4;
    private static final int _QC = 5;
    private static final int _QD = 6;
    private static final int _GND = 7;
    private static final int _CLK = 8;
    private static final int _CLR_ = 9;
    private static final int _QE = 10;
    private static final int _QF = 11;
    private static final int _QG = 12;
    private static final int _QH = 13;
    private static final int _VCC = 14;

    private int[] outputs = {_QA, _QB, _QC, _QD, _QE, _QF, _QG, _QH};
    private boolean[] Q = new boolean[8];
    private boolean wasClockLow = true;

    public SN74164() {
        super(PINS_COUNT);
    }

    public SN74164(Board board) {
        super(board, PINS_COUNT);
    }

    private void updateGate() {
        if (this.isLow(_CLR_)) {
            for (int i = 0; i < Q.length; i++) {
                Q[i] = false;
            }
        } else if (this.isHigh(_CLR_)) {
            if (wasClockLow && this.isHigh(_CLK)) {
                //shift
                System.arraycopy(Q, 0, Q, 1, Q.length - 1);

                Q[0] = this.isHigh(_A) && this.isHigh(_B);
            }
        }

        for (int i = 0; i < outputs.length; i++) {
            this.setPin(outputs[i], Q[i] ? Pin.PinState.HIGH : Pin.PinState.LOW);
        }
    }

    @Override
    protected void fillPins() {
        registerPin(_A, new InputPin(this, "A"));
        registerPin(_B, new InputPin(this, "B"));
        registerPin(_QA, new OutputPin(this, "QA"));
        registerPin(_QB, new OutputPin(this, "QB"));
        registerPin(_QC, new OutputPin(this, "QC"));
        registerPin(_QD, new OutputPin(this, "QD"));
        registerPin(_GND, new OutputPin(this, "GND"));
        registerPin(_CLK, new InputPin(this, "CLK"));
        registerPin(_CLR_, new InputPin(this, "CLR_"));
        registerPin(_QE, new OutputPin(this, "QE"));
        registerPin(_QF, new OutputPin(this, "QF"));
        registerPin(_QG, new OutputPin(this, "QG"));
        registerPin(_QH, new OutputPin(this, "QH"));
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
        this.setPin(_QA, Pin.PinState.NOT_CONNECTED);
        this.setPin(_QB, Pin.PinState.NOT_CONNECTED);
        this.setPin(_QC, Pin.PinState.NOT_CONNECTED);
        this.setPin(_QD, Pin.PinState.NOT_CONNECTED);
        this.setPin(_QE, Pin.PinState.NOT_CONNECTED);
        this.setPin(_QF, Pin.PinState.NOT_CONNECTED);
        this.setPin(_QG, Pin.PinState.NOT_CONNECTED);
        this.setPin(_QH, Pin.PinState.NOT_CONNECTED);
        wasClockLow = true;
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
