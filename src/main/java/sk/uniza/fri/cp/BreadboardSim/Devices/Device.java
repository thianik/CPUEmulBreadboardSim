package sk.uniza.fri.cp.BreadboardSim.Devices;

import javafx.event.EventHandler;
import javafx.scene.input.MouseEvent;
import sk.uniza.fri.cp.BreadboardSim.*;
import sk.uniza.fri.cp.BreadboardSim.Board.Board;
import sk.uniza.fri.cp.BreadboardSim.Board.BoardChangeEvent;
import sk.uniza.fri.cp.BreadboardSim.Components.Component;
import sk.uniza.fri.cp.BreadboardSim.Devices.Pin.Pin;
import sk.uniza.fri.cp.BreadboardSim.Socket.Potential;
import sk.uniza.fri.cp.BreadboardSim.Socket.Socket;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author Moris
 * @version 1.0
 * @created 17-mar-2017 16:16:34
 */
public abstract class Device extends Item {

	private Component component;
    private ArrayList<Socket> socketsToConnectTo;
    //private boolean isConnected;

    private int lastGridPosX;
    private int lastGridPosY;

    private EventHandler<MouseEvent> onMousePressedEventHandler = new EventHandler<MouseEvent>() {
        @Override
        public void handle(MouseEvent event) {
            if(isConnected()) disconnectAllPins();
            searchForSockets();
            highlightConnectibleSockets();
        }
    };

    private EventHandler<MouseEvent> onMouseDraggedEventHandler = new EventHandler<MouseEvent>() {
        @Override
        public void handle(MouseEvent event) {
            //ak sa zmenila pozicia zariadenia na ploche
            if(getGridPosX() != lastGridPosX || getGridPosY() != lastGridPosY){
                if (isConnected()) disconnectAllPins();

                //hladaj nove sokety na ktore by sa mohlo pripojit
                unhighlightConnectibleSockets();
                searchForSockets();
                highlightConnectibleSockets();

                lastGridPosX = getGridPosX();
                lastGridPosY = getGridPosY();
            }
        }
    };

    private EventHandler<MouseEvent> onMouseReleasedEventHandler = new EventHandler<MouseEvent>() {
        @Override
        public void handle(MouseEvent event) {
            //po umiestneni zariadenia na ploche sa pripoj k moznym soketom
            unhighlightConnectibleSockets();
            if (isConnected()) disconnectAllPins();
            tryToConnectToFoundSockets();
        }
    };

    public Device(){}

	public Device(Board board){
		super(board);
		
        socketsToConnectTo = new ArrayList<>();

        this.addEventFilter(MouseEvent.MOUSE_PRESSED, onMousePressedEventHandler);
        this.addEventFilter(MouseEvent.MOUSE_DRAGGED, onMouseDraggedEventHandler);
        this.addEventFilter(MouseEvent.MOUSE_RELEASED, onMouseReleasedEventHandler);

	}

    @Override
    public void makeImmovable() {
        super.makeImmovable();

        if(getBoard() != null) {
            this.removeEventFilter(MouseEvent.MOUSE_PRESSED, onMousePressedEventHandler);
            this.removeEventFilter(MouseEvent.MOUSE_DRAGGED, onMouseDraggedEventHandler);
            this.removeEventFilter(MouseEvent.MOUSE_RELEASED, onMouseReleasedEventHandler);
        }
    }

    /**
     * Simulacia sa vzdy vykonava na simulacnom vlakne
     */
	public abstract void simulate();
	public abstract void reset();


	public void registerToComponent(Component component){
		component.addDevice(this);
	}

	public void unregisterFromComponent(Component component){
		component.removeDevice(this);
	}

	public boolean isConnected(Pin pin){
	    return pin.getSocket().getPotential().getValue() != Potential.Value.NC;
    }

    /**
     * Aktualizacia a vratenie aktualnej hodnoty na pine podla potencialu socketu ku ktoremu je pripojeny
     *
     * @param inputPin Vstupny pin, ktoreho stavova hodnota sa ma aktualizovat a vratit
     * @return Aktualna hodnota na vstupnom pine
     */
    public boolean isHigh(Pin inputPin){
        if(inputPin == null) return false; //TODO vynimka?
        if(!inputPin.isConnected()) return false;

        Potential.Value value = inputPin.getSocket().getPotential().getValue();

        if(value == Potential.Value.HIGH){
            inputPin.setState(Pin.PinState.HIGH);
            return true;
        } else if (value == Potential.Value.LOW){
            inputPin.setState(Pin.PinState.LOW);
        } else inputPin.setState(Pin.PinState.NOT_CONNECTED);

        return false;
    }

	public boolean isLow(Pin inputPin){
		return !isHigh(inputPin);
	}

    /**
     * Nastavenie hodnoty na vystupnom pine a zaznamenanie eventu pre simulaciu s aktualizaciou.
     * Volat iba pri simulacii
     *
     * @param pin
     * @param state
     */
	public void setPin(Pin pin, Pin.PinState state){
	    pin.setState(state);

	    //if(getBoard().isSimulationRunning()) //TODO ?? je potrebne?
	    switch (state){
            case HIGH: getBoard().addEvent(new BoardChangeEvent(pin.getSocket(), Potential.Value.HIGH));
                break;
            case LOW: getBoard().addEvent(new BoardChangeEvent(pin.getSocket(), Potential.Value.LOW));
                break;
            case HIGH_IMPEDANCE:
            case NOT_CONNECTED:
                getBoard().addEvent(new BoardChangeEvent(pin.getSocket(), Potential.Value.NC));
        }
    }

    /*
    DETAKCIA KOLIZII -> PRIPOJENIA PINOV NA SOKETY
     */

    /**
     * Vrati piny urcene na pripojenie do soketov.
     * @return List pinov zariadenia
     */
    public abstract List<Pin> getPins();

    /**
     * Vyhlada sokety umiestnene pod pinmi zariadenia a zapamata si ich vo vlastnom poli pre operacie s nimi. 
     * @return Vysledok hladania. True ak nasiel aspon jeden soket pre pin.
     */
    public boolean searchForSockets(){
        boolean found = false;
        List<Component> collisionComponents = getBoard().checkForCollisionWithComponent(this);

        //vycistenie pola pre sokety ku ktorym sa moze pripojit
        this.socketsToConnectTo.clear();


        //ak existuje komponent s ktorym sa toto zariadenie prekryva
        if(collisionComponents != null){
            List<Pin> pins = getPins();
            Socket[] sockets = new Socket[pins.size()];

            //pre kazdy z komponentov
            for (Component component : collisionComponents) {
                //hladaj ci sa nachadza pin nad nejakym volnym soketom
                for (int i = 0; i < pins.size(); i++) {
                    //ak bol uz najdeny soket pre tento pin, preskoc hladanie
                    if(sockets[i] != null) continue;

                    //TODO berie sokety aj cez prekryvajuci komponent
                    Socket socket = getBoard().checkForCollisionWithSocket(component, pins.get(i));
                    //ak ano pridaj ho do zoznamu soketov na pripojenie
                    if(socket != null){
                        found = true;
                        sockets[i] = socket;
                    }
                }
            }

            if(found) this.socketsToConnectTo.addAll(Arrays.asList(sockets));
        }

        return found;
    }

    /**
     * Pokúsi sa pripojiť zariadenie k nájdeným soketom. Pripojenie sa podarí, iba ak je možné zapojiť všetky piny
     * zariadenia naraz.
     *
     * @return True ak sa podarilo pripojiť všetky piny zariadenia k soketom, false inak.
     */
    public boolean tryToConnectToFoundSockets() {
        List<Pin> pins = getPins();

        //ak sa mozu pripojit vsetky piny zariadenia
        if (this.socketsToConnectTo.size() == pins.size()) {
            for (int i = 0; i < this.socketsToConnectTo.size(); i++) {
                Socket socket = this.socketsToConnectTo.get(i);
                if (socket != null && !socket.isOccupied()) {
                    socket.connect(pins.get(i));

                    //ak nie si priradeny ku komponentu, prirad sa ku komponentu prveho pripojeneho soketu
                    if(this.component == null) {
                        this.component = socket.getComponent();
                        this.component.addDevice(this);
                    }
                } else {
                    //odpoj pripojene piny
                    this.disconnectAllPins();
                    return false;
                }
            }
        } else return false;

        return true;
    }

    public void disconnectAllPins(){
        List<Pin> pins = getPins();
        pins.forEach(Pin::disconnect);

        if(this.component != null){
            this.component.removeDevice(this);
            this.component = null;
        }
    }

    /**
     * Je aspon jeden pin pripojeny k soketu?
     * @return
     */
    public boolean isConnected(){
        List<Pin> pins = getPins();
        for (Pin pin : pins) {
            if(pin.isConnected()) return true;
        }

        return false;
    }

    /**
     * Zvyrazni sokety ku ktorym sa moze pripojit
     */
    public void highlightConnectibleSockets(){
        socketsToConnectTo.forEach((socket) -> {
            if (socket != null) socket.highlight(!socket.isOccupied() ? Socket.OK : Socket.WARNING);
        });
    }

    /**
     * Zrusi zvyraznenie soketok ku ktorym sa moze pripojit
     */
    public void unhighlightConnectibleSockets(){
        socketsToConnectTo.forEach((socket) -> {
            if (socket != null) socket.unhighlight(!socket.isOccupied() ? Socket.OK : Socket.WARNING);
        });
    }

    /**
     * Aktualizacia grafickych zobrazeni.
     * Vykonava sa na vlakne FXApplicationThread, preto pozor na hodnoty zdielanych premennych
     */
	protected void updateGraphic(){
        //update grafiky sa musi robit na FXThread-e
        //if(!Platform.isFxApplicationThread())
        //    Platform.runLater(this::updateGraphic);
    }

    @Override
    public void delete() {
        super.delete();

        this.disconnectAllPins();
    }
}

//    public boolean isHigh(int inputPinIndex){
//        if(inputPinIndex >= this.pinsCount) return false;
//
//        Pin inputPin = pins[inputPinIndex];
//        if(inputPin != null && inputPin.getSocket().getPotential().getValue() == Potential.Value.HIGH){
//            inputPin.setState(Pin.PinState.HIGH);
//            return true;
//        } else {
//            inputPin.setState(Pin.PinState.LOW);
//            return false;
//        }
//    }
//
//    public boolean isLow(int inputPinIndex){
//        return !isHigh(inputPinIndex);
//    }



