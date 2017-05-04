package sk.uniza.fri.cp.BreadboardSim.Socket;

import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.Parent;
import javafx.scene.effect.BoxBlur;
import javafx.scene.input.MouseDragEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.shape.*;
import sk.uniza.fri.cp.BreadboardSim.Board.BoardChangeEvent;
import sk.uniza.fri.cp.BreadboardSim.Board.BoardEvent;
import sk.uniza.fri.cp.BreadboardSim.Board.GridSystem;
import sk.uniza.fri.cp.BreadboardSim.Components.Component;
import sk.uniza.fri.cp.BreadboardSim.Devices.Device;
import sk.uniza.fri.cp.BreadboardSim.Devices.Pin.InputOutputPin;
import sk.uniza.fri.cp.BreadboardSim.Devices.Pin.InputPin;
import sk.uniza.fri.cp.BreadboardSim.Devices.Pin.OutputPin;
import sk.uniza.fri.cp.BreadboardSim.Devices.Pin.Pin;
import sk.uniza.fri.cp.BreadboardSim.Item;
import sk.uniza.fri.cp.BreadboardSim.SchoolBreadboard;
import sk.uniza.fri.cp.BreadboardSim.Wire.Wire;
import sk.uniza.fri.cp.BreadboardSim.Wire.WireEnd;

import java.util.LinkedList;

/**
 * pri vytvarani kablika pozor na vyjdenie mimo plochy
 * @author Moris
 * @version 1.0
 * @created 17-mar-2017 16:16:35
 */
public class Socket extends Group {

	//HIGHLIGHT KONSTANTY
    public static final int WARNING = 3;
    public static final int COMMON_POTENTIAL = 2;
    public static final int OK = 1;
	public static final int INFO = 0;

	private boolean[] activeHighlights;

	private Component component;
	private Potential potential;
	private Pin pin;

	private boolean pinLocked = false;

	private WireEnd connectedWireEnd;

	//grafika
	private final double coreRadius;
	private final double borderRadius;
	private Rectangle boundingBox;
	private Circle core;
	private Circle colorizer;

	private Arc topBorder;
    private static final Color TOP_BORDER_DEF_COLOR = Color.rgb(190, 190, 190);
    private static final Color TOP_BORDER_HIGHLIGHT_COLOR = Color.DARKBLUE;

	private Arc bottomBorder;
    private static final Color BOTTOM_BORDER_DEF_COLOR = Color.rgb(230, 230, 230);
    private static final Color BOTTOM_BORDER_HIGHLIGHT_COLOR = Color.LIGHTBLUE;

	//Eventy
    private static Wire creatingWire;
	//aktualizacia konca kablika, ak sa vytvara

	// zvyraznenie
	private EventHandler<MouseEvent> onMouseEntered = new EventHandler<MouseEvent>() {
		@Override
		public void handle(MouseEvent event) {
			highlight(INFO);
		}
	};

	private EventHandler<MouseEvent> onMouseExited = new EventHandler<MouseEvent>() {
		@Override
		public void handle(MouseEvent event) {
			unhighlight(INFO);
		}
	};

	/**
	 * zoznam socketov, ktore boli zvyraznene (aby ich bolo mozne odvyraznit)
	 */
    private LinkedList<Socket> highlighted;

	/**
	 * zaciatok vytvarania kablika, zvyraznienie spojenych socketov
	 */
	private EventHandler<MouseEvent> onMousePressed = new EventHandler<MouseEvent>() {
        @Override
        public void handle(MouseEvent event) {

            //event.consume();
        }
    };

    private EventHandler<MouseEvent> onMouseDragged = new EventHandler<MouseEvent>() {
        @Override
        public void handle(MouseEvent event) {
            if (!event.isPrimaryButtonDown()) return;

            Socket socket = (Socket)event.getSource();

            if (creatingWire != null) {
                Point2D boardXY = socket.component.getBoard().sceneToBoard(event.getSceneX(), event.getSceneY());
                creatingWire.catchFreeEnd().moveTo(boardXY.getX(), boardXY.getY());
            }

            event.consume();
        }
    };

    private EventHandler<MouseEvent> onMouseReleased = new EventHandler<MouseEvent>() {
        @Override
        public void handle(MouseEvent event) {

            if(creatingWire != null){
                creatingWire.setMouseTransparent(false);
                creatingWire.setOpacity(1);
                if (!creatingWire.areBotheEndsConnected()) creatingWire.delete();
                creatingWire = null;
            }

            event.consume();
        }
    };

	/**
	 * startFullDrag()
	 */
	private EventHandler<MouseEvent> onMouseDragDetected = new EventHandler<MouseEvent>() {
        @Override
        public void handle(MouseEvent event) {
            if (!event.isPrimaryButtonDown()) return;

            startFullDrag();

            Socket socket = (Socket) event.getSource();

            creatingWire = new Wire(socket);
            creatingWire.setMouseTransparent(true);
            creatingWire.setOpacity(0.5);

            socket.getComponent().getBoard().addItem(creatingWire);

            event.consume();
        }
    };


	/**
     * zvyraznenie pripojenych socketov pripojenych k somuto soketu
     */
	private EventHandler<MouseDragEvent> onMouseDragEntered = new EventHandler<MouseDragEvent>() {
        @Override
        public void handle(MouseDragEvent event) {
            getPotential().getConnectedSockets(highlighted);

            highlighted.forEach(socket -> socket.highlight(COMMON_POTENTIAL));

            //colorizer.setOpacity(0.9);
            //System.out.println("Socket - drag entered , source: " + event.getSource() + " / gesture source: " + event.getGestureSource());

            event.consume();
        }
    };
	/**
	 * zruzenie zvyraznenia pripojenych socketov pripojenych k somuto socketu
	 */
	private EventHandler<MouseDragEvent> onMouseDragExited = new EventHandler<MouseDragEvent>() {
        @Override
        public void handle(MouseDragEvent event) {
            highlighted.forEach(socket -> socket.unhighlight(COMMON_POTENTIAL));
            highlighted.clear();

            //colorizer.setOpacity(0);

            event.consume();
        }
    };

    /**
     * ked je na mne (socket) pustene tlacidlo
     */
    private EventHandler<MouseDragEvent> onMouseDragReleased = new EventHandler<MouseDragEvent>() {
        @Override
        public void handle(MouseDragEvent event) {
            if (highlighted.size() > 0) {
                highlighted.forEach(socket -> socket.unhighlight(COMMON_POTENTIAL));
                highlighted.clear();
            }

            //ak sa vytvara kablik
            if(creatingWire != null){
                Socket socket = (Socket) event.getSource();

                if(socket != event.getGestureSource()) {
                    //pripoj novy koniec na seba
                    creatingWire.catchFreeEnd().connectSocket(socket);

                }
                else
                    creatingWire.delete();

            } else if (event.getGestureSource() instanceof WireEnd){ //ak sa presuva koniec kablika
                WireEnd end = (WireEnd) event.getGestureSource();
                Socket socket = (Socket) event.getSource();
                end.connectSocket(socket);
            }

            event.consume();
        }
    };

	/**
	 * 
	 * @param component
	 */
    public Socket(Component component) {
        this.activeHighlights = new boolean[4];
		this.component = component;
        this.highlighted = new LinkedList<>();


		GridSystem grid = this.component.getBoard().getGrid();
		boundingBox = new Rectangle(grid.getSizeX(), grid.getSizeY());
		boundingBox.setOpacity(0);
		boundingBox.setLayoutX(-grid.getSizeX()/2.0);
        boundingBox.setLayoutY(-grid.getSizeY()/2.0);
        coreRadius = grid.getSizeX() * 3.0 / 16.1;
        borderRadius = grid.getSizeX() * 5.3 / 15.0;

		//grafika
		core = new Circle(borderRadius, borderRadius, coreRadius);
		core.setFill(Color.BLACK);

		topBorder = new Arc(borderRadius, borderRadius, borderRadius, borderRadius, 0.0, 180.0);
        topBorder.setFill(TOP_BORDER_DEF_COLOR);
        topBorder.setType(ArcType.ROUND);

		bottomBorder = new Arc(borderRadius, borderRadius, borderRadius, borderRadius, 180.0, 180.0);
        bottomBorder.setFill(BOTTOM_BORDER_DEF_COLOR);
        bottomBorder.setType(ArcType.ROUND);

        this.colorizer = new Circle(borderRadius, borderRadius, borderRadius+borderRadius*0.3);
        this.colorizer.setFill(Color.ORANGE);
        this.colorizer.setMouseTransparent(true);
        this.colorizer.setOpacity(0);
        BoxBlur blur = new BoxBlur(borderRadius*0.7,borderRadius*0.7,3);
        this.colorizer.setEffect(blur);

        this.potential = new Potential(this, null);

		//pre relativny offset k stredu socketu
		Group graphics = new Group(topBorder, bottomBorder, core, colorizer);
		graphics.setTranslateX(-borderRadius);
		graphics.setTranslateY(-borderRadius);

		this.getChildren().addAll(boundingBox, graphics);

		this.addEventFilter(MouseEvent.MOUSE_ENTERED, onMouseEntered);
		this.addEventFilter(MouseEvent.MOUSE_EXITED, onMouseExited);
		this.addEventFilter(MouseEvent.MOUSE_PRESSED, onMousePressed);
		this.addEventFilter(MouseEvent.MOUSE_DRAGGED, onMouseDragged);
        this.addEventFilter(MouseEvent.MOUSE_RELEASED, onMouseReleased);
        this.addEventFilter(MouseEvent.DRAG_DETECTED, onMouseDragDetected);
        this.addEventFilter(MouseDragEvent.MOUSE_DRAG_ENTERED, onMouseDragEntered);
        this.addEventFilter(MouseDragEvent.MOUSE_DRAG_EXITED, onMouseDragExited);
        this.addEventFilter(MouseDragEvent.MOUSE_DRAG_RELEASED, onMouseDragReleased);

	}

	/**
	 * 
	 * @param component
	 * @param potentialValue
	 */
    public Socket(Component component, Potential.Value potentialValue) {
        this(component);
        this.potential.setValue(potentialValue);
	}

	public double getRadius(){
		return borderRadius;
	}

    public Point2D getBoardLayoutX() {
        return component.getBoard().sceneToBoard(this.getLocalToSceneTransform().getTx(), this.getLocalToSceneTransform().getTy());
    }

	public double getBoardX(){
        return this.getLocalToSceneTransform().getTx() - component.getBoard().getOriginSceneOffsetX();
    }

    public double getBoardY(){
        return this.getLocalToSceneTransform().getTy() - component.getBoard().getOriginSceneOffsetY();
    }

    /**
     * Zvýraznenie soketu. Každý typ zvýraznenia má svoju prioritu. Ak je použíté zvýraznenie s vyššou prioritou,
     * nezobrazí sa zvýraznenie s nižšou prioritou.
     *
     * Typy podľa priority:
     * WARNING - Červené zvýraznenie pri závažnej chybe na sokete (napr. skrat na výstup). Mení farbu aj pripojeného
     *           konca káblika, ak nejaký je
     * COMMON_POTENTIAL - Žltá farba. Soket je pripojený na spoločný potenciál s inými soketmi.
     * OK - Zelená farba. Akcia nad soketom je povolená.
     * INFO - Oranžová farba. Iné
     *
     * @param highlightType Typ zvýraznenia.
     */
    public void highlight(int highlightType){
        this.activeHighlights[highlightType] = true;
        this.updateHighlight();
	}

	/**
	 * Zrusenie zvyraznenia soketu
	 */
	public void unhighlight(int highlightType){
        this.activeHighlights[highlightType] = false;
        this.updateHighlight();
	}

	private void updateHighlight(){
        if(this.activeHighlights[WARNING]){
            changeColor(this.colorizer, Color.RED, 0.8);
            if (this.connectedWireEnd != null) {
                this.connectedWireEnd.setColor(Color.RED);
            }
            return;
        } else if(this.activeHighlights[COMMON_POTENTIAL]){
            changeColor(this.colorizer, Color.YELLOW, 0.5);
        } else if(this.activeHighlights[OK]){
            changeColor(this.colorizer, Color.GREEN, 0.5);
        } else if(this.activeHighlights[INFO]){
            changeColor(this.colorizer, Color.ORANGE, 0.5);
        } else {
            //ak nie je nic na zvyrazenie
            changeColor(this.colorizer, Color.WHITE, 0);
        }

        if(this.connectedWireEnd != null)
            this.connectedWireEnd.setDefaultColor();
    }

    private void changeColor(Shape shape, Color color, double opacity) {
        if (Platform.isFxApplicationThread()) {
            shape.setFill(color);
            shape.setOpacity(opacity);
        } else {
            Platform.runLater(() -> {
                shape.setFill(color);
                shape.setOpacity(opacity);
            });
        }
    }

	/**
	 * Uzamkne pin v sokete. Znemožní tak pripojenie iného pinu.
	 * @return Pin uzamknutý
	 */
	public void lockPin(){
		pinLocked = true;
	}

	public void unlockPin(){
		pinLocked = false;
	}

	public boolean hasConnectedPin() { return this.pin != null; }

    public boolean hasConnectedWire() {
        return this.connectedWireEnd != null;
    }

    public boolean isOccupied() {
        return this.hasConnectedPin() || this.hasConnectedWire();
    }

	/**
	 * Pripojenie pinu k soketu. Pri pripojení sa aktualizuje typ soketu na základe pripojeného pinu.
	 * Pin sa nepripojí, ak je v sokete zamknutý iný pin alebo je k nemu pripojený káblik.
	 *
	 * @param pin Pin a pripojenie. Soket na základe neho mení svoj typ.
	 * @return True ak sa podarilo pripojiť pin, false inak.
	 */
	public boolean connect(Pin pin){
		if(this.pinLocked || (this.connectedWireEnd != null) ) return false;

		this.pin = pin;
		this.pin.setSocket(this);
		if(pin instanceof InputPin){
			this.setType(SocketType.IN);
		} else if (pin instanceof InputOutputPin){
			this.setType(SocketType.IO);
		} else if (pin instanceof OutputPin){
			switch (((OutputPin) pin).getPinDriver()){
			    //ak je output typu TRI_STATE alebo OPEN_COLLECTOR, ma rovnaku prioritu, TRI_STATE moze byt ale aj v HiZ
                //case OPEN_COLLECTOR:
                case TRI_STATE:
                    this.setType(SocketType.TRI_OUT);
                    break;
				case PUSH_PULL:
					this.setType(SocketType.OUT);
					break;
                default:
            }
		} else this.setType(SocketType.NC);

        //ak simulacia bezi, pridaj zmenu na sokete
        if(this.component.getBoard().isSimulationRunning()){
			this.component.getBoard().addEvent(new BoardEvent(this));
		}

		return true;
	}

	public void connect(WireEnd wireEnd){

        this.connectedWireEnd = wireEnd;
        this.component.addWire(wireEnd.getWire());
    }

	public boolean disconnect(){
	    //ak je pripojeny kablik, odpoj ho
	    if(this.connectedWireEnd != null) {
	        this.component.removeWire(this.connectedWireEnd.getWire());
	        this.connectedWireEnd = null;
	        return true;}

        //ak nie je pripojeny kablik ale pin
        //ak je zamknuty, return
		if(pinLocked) return false;

	    //ak nie je zamknuty, odpoj ho
		if(this.pin != null) {
			this.pin.setSocket(null);
			this.pin = null;
		}

		//a nastav typ soketu na nepripojeny
		this.setType(SocketType.NC);

		//ak simulacia bezi, pridaj zmenu na sokete
		if(this.component.getBoard().isSimulationRunning()){
			this.component.getBoard().addEvent(new BoardChangeEvent(this, Potential.Value.NC));
		}

		return true;
	}

	public Potential getPotential(){
		if(this.potential != null)
			return this.potential.getPotential();

		return null;
	}

	public Potential getThisPotential(){
		return this.potential;
	}

	public WireEnd getWireEnd(){
	    return this.connectedWireEnd;
    }

	public Pin getPin(){
		return this.pin;
	}

	public Device getDevice(){
		if(this.pin != null){
			return this.pin.getDevice();
		}

		return null;
	}

	public Component getComponent(){
	    return component;
    }

    /**
     * Pohyblivy item . ak je komponent na ktorom je soket scholbreadboar vrat ten inak komponent samotny
     *
     * @return
     */
    public Item getItem() {

        Parent parent = this.component;
        while (parent.getParent() instanceof Item) {
            parent = parent.getParent();
        }
        return (Item) parent;

    }

	public void setType(SocketType socketType){
		this.potential.setType(socketType);
	}

	public SocketType getType(){
		return this.getPotential().getType();
	}

	/**
	 * Nastavenie hodnoty potencialu priradeneho tomuto soketu
	 * @param potentialValue
     */
    public boolean setPotential(Potential.Value potentialValue){
        return this.potential.setValue(potentialValue);
	}



}