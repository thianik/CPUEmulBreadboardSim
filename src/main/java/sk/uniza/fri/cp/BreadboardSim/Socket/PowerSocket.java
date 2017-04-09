package sk.uniza.fri.cp.BreadboardSim.Socket;

import sk.uniza.fri.cp.BreadboardSim.Board.BoardChangeEvent;
import sk.uniza.fri.cp.BreadboardSim.Components.Component;

/**
 * Created by Moris on 24.3.2017.
 */
public class PowerSocket extends Socket {

    private Potential.Value value;

    public PowerSocket(Component component, Potential.Value connectedValue) {
        super(component);
        this.setType(SocketType.OUT);
        this.value = connectedValue;
    }

    public void powerUp(){
        //this.setPotential(value);
        getComponent().getBoard().addEvent(new BoardChangeEvent(this, value));
    }

    public void powerDown(){
        //this.setPotential(Potential.Value.NC);
        getComponent().getBoard().addEvent(new BoardChangeEvent(this, Potential.Value.NC));
    }
}
