package sk.uniza.fri.cp.BreadboardSim.Components;

import com.sun.org.apache.xpath.internal.operations.Bool;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import org.reactfx.EventSource;
import org.reactfx.EventStream;
import org.reactfx.EventStreams;
import org.reactfx.util.FxTimer;
import sk.uniza.fri.cp.BreadboardSim.Board.Board;
import sk.uniza.fri.cp.BreadboardSim.Board.BoardEvent;
import sk.uniza.fri.cp.BreadboardSim.Board.GridSystem;
import sk.uniza.fri.cp.BreadboardSim.Devices.Device;
import sk.uniza.fri.cp.BreadboardSim.Devices.Pin.InputOutputPin;
import sk.uniza.fri.cp.BreadboardSim.Devices.Pin.InputPin;
import sk.uniza.fri.cp.BreadboardSim.Devices.Pin.OutputPin;
import sk.uniza.fri.cp.BreadboardSim.Devices.Pin.Pin;
import sk.uniza.fri.cp.BreadboardSim.LightEmitter;
import sk.uniza.fri.cp.BreadboardSim.SchoolBreadboard;
import sk.uniza.fri.cp.BreadboardSim.Socket.Potential;
import sk.uniza.fri.cp.BreadboardSim.Socket.Socket;
import sk.uniza.fri.cp.BreadboardSim.Socket.SocketType;
import sk.uniza.fri.cp.BreadboardSim.Socket.SocketsFactory;
import sk.uniza.fri.cp.Bus.Bus;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
//import javax.swing.event.ChangeListener;

/**
 * Pripojenie na zbernice - address, data, control
 * @author Moris
 * @version 1.0
 * @created 17-mar-2017 16:16:34
 */
public class BusInterface extends Component {

    public static int DEBUG_DATA = 0;

    private static final Color ADDRESS_LED_ON = Color.RED;
    private static final Color ADDRESS_LED_OFF = Color.DARKRED;
    private static final Color DATA_LED_ON = Color.LIME;
    private static final Color DATA_LED_OFF = Color.SEAGREEN;
    private static final Color CONTROL_LED_ON = Color.YELLOW;
    private static final Color CONTROL_LED_OFF = Color.OLIVE;

	private Bus bus;

	private Rectangle background;

	private Socket[] addressSockets;
	private Socket[] dataSockets;
	private Socket[] controlSockets;
	private BusCommunicator[] addressBusCommunicators;
	private BusCommunicator[] dataBusCommunicators;
	private BusCommunicator[] controlBusCommunicators;

    private final ChangeListener<Boolean> onSimulationStateChange = new ChangeListener<Boolean>() {
        @Override
        public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
            if (newValue.booleanValue()) {
                //spustenie az po zapnuti PowerSoketov, inak sa skratuju hned po spusteni
                Thread runLater = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        while (!getBoard().getSimulator().inSteadyState()) {
                        }
                        ;

                        AddressBusCommunicator.setAddress(bus.getAddressBus());
                        DataBusCommunicator.setData(bus.getDataBus());
                        ControlBusCommunicator.setControls(bus.getControlBus());

                        for (int i = 0; i < 16; i++)
                            addressBusCommunicators[i].update();

                        for (int i = 4; i < 9; i++)  //iba pre vystupne signaly
                            controlBusCommunicators[i].update();

                        for (int i = 0; i < 8; i++)
                            dataBusCommunicators[i].update();
                    }
                });

                runLater.setDaemon(true);
                runLater.start();
            } else {
                for (int i = 0; i < 16; i++)
                    addressBusCommunicators[i].reset();

                for (int i = 4; i < 9; i++)  //iba pre vystupne signaly
                    controlBusCommunicators[i].reset();

                for (int i = 0; i < 8; i++)
                    dataBusCommunicators[i].reset();
            }
        }
    };
    private final ChangeListener<Number> onAddressBusChange = new ChangeListener<Number>() {
        @Override
        public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
            AddressBusCommunicator.setAddress(newValue.intValue());
            for (int i = 0; i < 16; i++) {
                addressBusCommunicators[i].update();
            }
        }
    };
    private final ChangeListener<Number> onDataBusChange = new ChangeListener<Number>() {
        @Override
        public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
            //nastavenie aktualnych dat na datovej zbernici pre vsetky instancie komunikatora (ledky)

            if ((bus.getControlBus() & 0xB0) == 0xB0) {
                //ak nie je zapnute citanie -> ak je, tak sa riadime iba podla simulacie,
                //nie podla dat ulozenych na zbernici
                DataBusCommunicator.setData(newValue.intValue());

                for (int i = 0; i < 8; i++) {
                    dataBusCommunicators[i].update();
                }
            }

        }
    };

    private volatile boolean lastReadStateOn = false;
    private volatile boolean lastWriteStateOn = false;
    private final ChangeListener<Number> onControlBusChange = new ChangeListener<Number>() {
        @Override
        public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
            ControlBusCommunicator.setControls(newValue.intValue());

            //0x140 -> 1 0100 0000 => signal zapisu MW/IW je v nule
            if ((newValue.intValue() & 0x140) != 0x140 && !lastWriteStateOn) {
                //zapis
                DataBusCommunicator.setToWrite(true);
                for (int i = 0; i < 8; i++)
                    dataBusCommunicators[i].update();

                lastWriteStateOn = true;
            }

            //MR/IR/IA
            if ((newValue.intValue() & 0xB0) != 0xB0 && !lastReadStateOn) {
                //citanie
                for (int i = 0; i < 8; i++) {
                    ((DataBusCommunicator) dataBusCommunicators[i]).setToRead(true);
                    dataBusCommunicators[i].update(); //ABY SI PREDISIEL DALSIM INFAKRTOM, TOTO TU MUSI BYT
                }
                lastReadStateOn = true;
            } else if (lastReadStateOn) {
                for (int i = 0; i < 8; i++) {
                    ((DataBusCommunicator) dataBusCommunicators[i]).setToRead(false);
//                    dataBusCommunicators[i].update(); //TODO musi tu byt< kontrola
                }
                lastReadStateOn = false;
            }
//
//            for (int i = 0; i < 8; i++)
//                dataBusCommunicators[i].update();
//
            for (int i = 4; i < 9; i++) { //iba pre vystupne signaly
                controlBusCommunicators[i].update();
            }
//
//            for (int i = 0; i < 8; i++)
//                dataBusCommunicators[i].update();

            if ((newValue.intValue() & 0x140) == 0x140 && lastWriteStateOn) {
                DataBusCommunicator.setToWrite(false);
                for (int i = 0; i < 8; i++) {
                    dataBusCommunicators[i].update();
                    //dataBusCommunicators[i].update();
                }
                lastWriteStateOn = false;
            }

        }
    };

    public BusInterface() {
    }

    public BusInterface(Board board) {
        super(board);
        this.bus = Bus.getBus();

        //grafika
        GridSystem grid = getBoard().getGrid();

        this.gridWidth = grid.getSizeX() * 66; //68
        this.gridHeight = grid.getSizeY() * 5;
        background = new Rectangle(this.gridWidth, this.gridHeight, SchoolBreadboard.BACKGROUND_COLOR);

        //GND
        Group leftGndSockets = SocketsFactory.getHorizontalPower(this, 2, Potential.Value.LOW, getPowerSockets());
        Text leftGndText = Board.getLabelText("GND", grid.getSizeMin());
        leftGndText.setLayoutX(-grid.getSizeX() / 2.0);
        leftGndText.setLayoutY(-grid.getSizeY());
        leftGndSockets.getChildren().add(leftGndText);
        leftGndSockets.setLayoutX(grid.getSizeX() * 8);
        leftGndSockets.setLayoutY(grid.getSizeY() * 4);

        Group rightGndSockets = SocketsFactory.getHorizontalPower(this, 2, Potential.Value.LOW, getPowerSockets());
        Text rightGndText = Board.getLabelText("GND", grid.getSizeMin());
        rightGndText.setLayoutX(-grid.getSizeX() / 2.0);
        rightGndText.setLayoutY(-grid.getSizeY());
        rightGndSockets.getChildren().add(rightGndText);
        rightGndSockets.setLayoutX(grid.getSizeX() * 57);
        rightGndSockets.setLayoutY(grid.getSizeY() * 4);

        this.getChildren().addAll(background,    leftGndSockets, rightGndSockets);
        generateInterface();

        //registracia vsetkych soketov
        this.addAllSockets(getPowerSockets());
        //zvysna sa pridavaju pocas vytvarania v getnerateInterface

        //prvotna inicializacia
        this.bus.setRandomAddress();
        this.bus.setRandomData();
        int control = this.bus.getControlBus();
        for (int i = 4; i < 9; i++) { //iba pre vystupne signaly
            if((1<<i & control) != 0)
                controlBusCommunicators[i].setHigh();
            else
                controlBusCommunicators[i].setLow();
        }

        registerListeners();
    }

	public void setBackgroundColor(Color newColor){
	    background.setFill(newColor);
    }

    @Override
    public void delete() {
        super.delete();
        unregisterListeners();
    }

    private void registerListeners() {
        this.getBoard().simRunningProperty().addListener(onSimulationStateChange);
        this.bus.addressBusProperty().addListener(onAddressBusChange);
        this.bus.dataBusProperty().addListener(onDataBusChange);
        this.bus.controlBusProperty().addListener(onControlBusChange);
    }

    private void unregisterListeners() {
        this.getBoard().simRunningProperty().removeListener(onSimulationStateChange);
        this.bus.addressBusProperty().removeListener(onAddressBusChange);
        this.bus.dataBusProperty().removeListener(onDataBusChange);
        this.bus.controlBusProperty().removeListener(onControlBusChange);
    }

	private void generateInterface(){
		GridSystem grid = getBoard().getGrid();
		Text name;

		//ADRESNA ZBERNICA
		addressSockets = new Socket[16];
		addressBusCommunicators = new BusCommunicator[16];
		Group addressInterface = new Group();
		addressInterface.setLayoutX(grid.getSizeX() * 15);
		addressInterface.setLayoutY(grid.getSizeY() * 4);

		//generovanie soketov, komunikatorov a oznaceni soketov
		for (int i = 0; i < 16; i++) {
			double centerX = grid.getSizeX() * (16-i);

			//soket
            Socket socket = new Socket(this);
            socket.setLayoutX(centerX);
			addressSockets[i] = socket;

			//komunikator so zbernicou
            AddressBusCommunicator communicator = new AddressBusCommunicator(getBoard(), addressSockets[i], i);
            communicator.setLayoutX(centerX);
			communicator.setLayoutY(-grid.getSizeY());
			addressBusCommunicators[i] = communicator;

			//oznacenie
			Text label = Board.getLabelText(String.valueOf(i), grid.getSizeMin());
			label.setLayoutX(centerX - label.getBoundsInLocal().getWidth()/2.0);
			label.setLayoutY(-grid.getSizeY()*2);

			addressInterface.getChildren().addAll(socket, communicator, label);
		}

		//ozancenie
		name = Board.getLabelText("AB", grid.getSizeMin());
		name.setLayoutX(-grid.getSizeX() * 3);
		addressInterface.getChildren().add(name);


		//DATOVA ZBERNICA
        dataSockets = new Socket[8];
        dataBusCommunicators = new BusCommunicator[8];
        Group dataInterface = new Group();
		dataInterface.setLayoutX(grid.getSizeX() * 36);
		dataInterface.setLayoutY(grid.getSizeY() * 4);

		//generovanie soketov, komunikatorov a oznaceni soketov
		for (int i = 0; i < 8; i++) {
			double centerX = grid.getSizeX() * (8-i);

			//soket
            Socket socket = new Socket(this);
            socket.setLayoutX(centerX);
			dataSockets[i] = socket;

			//komunikator so zbernicou
            DataBusCommunicator communicator = new DataBusCommunicator(getBoard(), dataSockets[i], i);
            communicator.setLayoutX(centerX);
			communicator.setLayoutY(-grid.getSizeY());
			dataBusCommunicators[i] = communicator;

			//oznacenie
			Text label = Board.getLabelText(String.valueOf(i), grid.getSizeMin());
			label.setLayoutX(centerX - label.getBoundsInLocal().getWidth()/2.0);
			label.setLayoutY(-grid.getSizeY()*2);

			dataInterface.getChildren().addAll(socket, communicator, label);
		}

		//ozancenie
		name = Board.getLabelText("DB", grid.getSizeMin());
		name.setLayoutX(-grid.getSizeX() * 3);
		dataInterface.getChildren().add(name);

		//RIADIACA ZBERNICA
		controlSockets = new Socket[9];
		controlBusCommunicators = new BusCommunicator[9];
		Group controlInterface = new Group();
		controlInterface.setLayoutX(grid.getSizeX() * 45);
		controlInterface.setLayoutY(grid.getSizeY() * 4);

		//generovanie soketov, komunikatorov a oznaceni soketov
		for (int i = 0; i < 9; i++) {
			double centerX = grid.getSizeX() * (9-i);

			//soket
            Socket socket = new Socket(this);
            socket.setLayoutX(centerX);
			controlSockets[i] = socket;

			//komunikator so zbernicou
            ControlBusCommunicator communicator = new ControlBusCommunicator(getBoard(), controlSockets[i], i);
            communicator.setLayoutX(centerX);
			communicator.setLayoutY(-grid.getSizeY());
			controlBusCommunicators[i] = communicator;

			//oznacenie
			String lb = "";
			switch (i){
				case 0:	lb = "BA";
					break;
				case 1:	lb = "BQ";
					break;
				case 2:	lb = "RY";
					break;
				case 3:	lb = "IT";
					break;
				case 4:	lb = "IA";
					break;
				case 5:	lb = "IR";
					break;
				case 6:	lb = "IW";
					break;
				case 7:	lb = "MR";
					break;
				case 8:	lb = "MW";
					break;
			}
			Text label = Board.getLabelText(lb, grid.getSizeMin());
			label.setLayoutX(centerX - label.getBoundsInLocal().getWidth() / 2.0);
			label.setLayoutY(-2*grid.getSizeY());

			if(i % 2 != 0) label.setLayoutY(-3*grid.getSizeY());	//posunutie oznacenia hore

			//negovacia ciarka nad oznacenim
			if(i >= 4) {
				Line negLogic = new Line(label.getLayoutX(),
                        label.getLayoutY() - grid.getSizeX() + 2,
                        label.getLayoutX() + label.getBoundsInLocal().getWidth(),
                        label.getLayoutY() - grid.getSizeX() + 2);
				negLogic.setId("breadboardLabel");
				controlInterface.getChildren().add(negLogic);
			}

			controlInterface.getChildren().addAll(socket, communicator, label);
		}

		this.getChildren().addAll(addressInterface, dataInterface, controlInterface);
        //registracia soketov
        this.addAllSockets(addressSockets);
        this.addAllSockets(dataSockets);
        this.addAllSockets(controlSockets);
    }


	private abstract static class BusCommunicator extends Device{

		private static final double RADIUS_COEF = 0.4;

		private Circle indicator;
        private LightEmitter lightEmitter;
        private Color onColor;
        private Color offColor;
        private volatile boolean indicatorOn;

        protected Pin pin; //pin pripojeny k rozhraniu na doske
        private int byteNr; //cislo bitu na zbernici od najnizsieho radu

        public BusCommunicator(Board board, Socket interfaceSocket, int byteNr, Color onColor, Color offColor) {
            super(board);

			this.byteNr = byteNr;
			this.onColor = onColor;
			this.offColor = offColor;

			GridSystem grid = board.getGrid();
			this.indicator = new Circle(grid.getSizeMin() * RADIUS_COEF, offColor);

            this.lightEmitter = new LightEmitter(board, this.indicator, this.onColor, this.offColor);

			initPin();

            //vytvorenie vnuterneho soketu, prepojenie potencialom s vonkajsim a pripojenie pinu
            //TODO TO TU JE ZRADA DAJAKA... TEN POTENCIAL SA STALE MENI... ZMENA V TRIEDE DB -> pri !W && !R
//			Socket hiddenSocket = new Socket(interfaceSocket.getComponent());
//			Potential hiddenPotential = new Potential(hiddenSocket, interfaceSocket);
//			hiddenSocket.connect(this.pin);
            interfaceSocket.connect(this.pin);
            interfaceSocket.lockPin();

			this.getChildren().addAll(indicator);
			this.makeImmovable();
		}

        /**
         * Aktualizácia hodnoty komunikátora.
         */
        abstract public void update();

        /**
         * Inicializácia pinu volaná v konštruktore.
         */
        abstract protected void initPin();

        protected final int getByteNr() {
            return this.byteNr;
        }

        protected final Pin getPin() {
            return this.pin;
        }

        @Override
        public boolean isHigh(Pin inputPin) {
            if (super.isHigh(inputPin)) {
                this.turnIndicatorOn();
                return true;
            } else {
                //this.turnIndicatorOff();
                return false;
            }
        }

        protected final void setHigh() {
            //if(this.getBoard().isSimulationRunning())
            this.setDataPin(this.pin, Pin.PinState.HIGH);
            turnIndicatorOn();
        }

        @Override
        public boolean isLow(Pin inputPin) {
            if (super.isLow(inputPin)) {
                this.turnIndicatorOff();
                return true;
            } else {
                //this.turnIndicatorOn();
                return false;
            }
        }

        protected final void setLow() {
            //if(this.getBoard().isSimulationRunning())
            this.setDataPin(this.pin, Pin.PinState.LOW);
            turnIndicatorOff();
        }

        protected final void setHighImpedance(boolean turnIndicatorOn) {
            this.setDataPin(this.pin, Pin.PinState.HIGH_IMPEDANCE);
            if (turnIndicatorOn)
                turnIndicatorOn();
            else
                turnIndicatorOff();
        }


        protected final void turnIndicatorOn() {
            this.lightEmitter.turnOn();
        }

        protected final void turnIndicatorOff() {
            this.lightEmitter.turnOff();
        }

        @Override
        public List<Pin> getPins() {
            return null;
        }

        @Override
        public void delete() {
            super.delete();
            this.lightEmitter.delete();
        }

        @Override
        public void reset() {
            this.pin.getSocket().setPotential(Potential.Value.NC);
        }
	}

	private static class AddressBusCommunicator extends BusCommunicator{

        private static final Color ON_COLOR = Color.RED;
        private static final Color OFF_COLOR = Color.DARKRED;

        private volatile static int address;

        /**
         * Nastavenie akutálnej adresy na zbernici
         *
         * @param newAddress Adresa na zbernici
         */
        public static void setAddress(int newAddress) {
            address = newAddress;
        }

        public AddressBusCommunicator(Board board, Socket interfaceSocket, int byteNr) {
            super(board, interfaceSocket, byteNr, ON_COLOR, OFF_COLOR);
        }

        @Override
        public void update() {
            if ((address & 1 << this.getByteNr()) != 0) {
                this.setHigh();
            } else {
                this.setLow();
            }
        }

        @Override
        public void simulate() {
        } //output, neprebieha simulacia

        @Override
        protected void initPin() {
            this.pin = new OutputPin(this, "Address Output Pin " + this.getByteNr());
        }

    }

	private static class DataBusCommunicator extends BusCommunicator{

        private static final Color ON_COLOR = Color.LIME;
        private static final Color OFF_COLOR = Color.SEAGREEN;

        private volatile static int data; //aktualne data na zbernici
        private volatile static boolean read; //nastavenie ako vstupu
        private volatile static boolean write; //nastavenie ako vystupu
        //private volatile static Potential[] connectedPotentials = new Potential[8]; dalo by sa pouzit pre zistovenie, ci su dva vystupy pripojene na rovnaky potencial pre zistenie skratu, mozno

        public static void setData(int newData) {
            data = newData;
        }

        public void setToRead(boolean newRead) {
            read = newRead;
            Bus.getBus().dataIsChanging();
            if (read) {
                this.setDataPin(this.getPin(), Pin.PinState.HIGH_IMPEDANCE);
            } else {
                this.update();
            }
        }

        public static void setToWrite(boolean newWrite) {
            write = newWrite;
        }

        public DataBusCommunicator(Board board, Socket interfaceSocket, int byteNr) {
            super(board, interfaceSocket, byteNr, ON_COLOR, OFF_COLOR);
            this.read = false;
            this.write = false;
        }

		/**
		 * Aktualizacia po zmene dat na zbernici
         * //TODO REFAKTORING
         */
		public void update(){
            if (write) {
                Bus.getBus().dataIsChanging();
                //zapis zo zbernice do externej pamate na zaklade
                if ((data & 1 << this.getByteNr()) != 0) {
                    this.setHigh();
//                    System.out.println("BUSINTERFACE WRITE Nastavovane data na interface: " + data + " \t\t" + Thread.currentThread().getName() + " \t bit: " + getByteNr() + " HIGH");
                } else {
                    this.setLow();
//                    System.out.println("BUSINTERFACE WRITE Nastavovane data na interface: " + data + " \t\t" + Thread.currentThread().getName() + " \t bit: " + getByteNr() + " LOW");
                }
                DEBUG_DATA = data;

            } else if (read) {
                Bus.getBus().dataIsChanging();
                //citanie z externeho zariadenia a zapis na zbernicu
                if (this.isHigh(this.getPin())) {
                    //ak je na pine jednotka
                    Bus.getBus().setDataBus((byte) (data |= (1 << this.getByteNr())));
//                    System.out.println("BUSINTERFACE READ Zapisane data na zbernicu: " + data + " \t\t" + Thread.currentThread().getName() + " \t bit: " + getByteNr() + " HIGH");
                } else if (this.isLow(this.getPin())) {
                    //ak nie tak sa predpoklada ze je tam nula (aj ked je nepripojeny, lebo CPU je odpojene)
                    Bus.getBus().setDataBus((byte) (data &= ~(1 << this.getByteNr())));
//                    System.out.println("BUSINTERFACE READ Zapisane data na zbernicu: " + data + " \t\t" + Thread.currentThread().getName() + " \t bit: " + getByteNr() + " LOW");
                } else {
//                    System.out.println("BUSINTERFACE READ Zapisane rovnake data na zbernicu lebo je NC pri citani: " + data + " \t\t" + Thread.currentThread().getName() + " \t bit: " + getByteNr());
                }

            } else {
                //ANI ZAPIS ANI CITANIE -> TO DIVNE SPRAVANIE Z POVODNEHO SIMULATORA

                //zachovanie povodnej hodnoty
                Potential.Value oldValue = this.getPin().getSocket().getThisPotential().getValue();

                //odpojenie aby neovplyvnoval vyslednu hodnotu na potencialy pri zistovani kto ho riadi (potencial)
                if (this.getBoard().isSimulationRunning())
                    this.getPin().getSocket().setPotential(Potential.Value.NC);

                //zistenie, ci je potencial, na ktory je pripojeny riadeny nim (zbernicou) alebo nie
                if (this.getPin().getSocket().getPotential().getValue() != Potential.Value.NC) {
                    //VYSLEDNY POTENCIAL RIADI NIEKTO INY (lebo nie je not connected -> je tam nejaky vystup)

                    //ak hodnota potencialu nie je riadena zo zbernice, je riadena inym vystupom, ktory sa sem pripaja
                    //a teda podla neho iba nastavim ledku kontrolou ci je HIGH alebo LOW
                    if (!this.isHigh(this.getPin()))
                        this.isLow(this.getPin());
                } else {
                    //VYSLEDNY POTENCIAL JE RIADENY ZO ZBERNICE

                    //vratenie povodneho potencialu na sokete zbernice
                    this.getPin().getSocket().setPotential(oldValue);

                    //ak ja riadim hodnotu potencialu, tak ju riadim na zaklade dat zo zbernice
                    if ((data & 1 << this.getByteNr()) != 0) {
                        //mal by vysielat output HIGH
                        this.turnIndicatorOn();

                        //ak sa nova hodnota lisi od starej, vytvorim event pre simulaciu, inak sa zacyklim ako nikto
                        if (oldValue != Potential.Value.HIGH) {
                            this.setHigh();
                        }
                    } else {
                        this.turnIndicatorOff();

                        if (oldValue != Potential.Value.LOW) {
                            this.setLow();
                        }
                    }
                }
            }

		}

		@Override
		public void simulate() {
            update();
		}

		@Override
        protected void initPin() {
            this.pin = new InputOutputPin(this, "Data IO Pin " + this.getByteNr());
        }

    }

	private static class ControlBusCommunicator extends BusCommunicator{

        private static final Color ON_COLOR = Color.YELLOW;
        private static final Color OFF_COLOR = Color.OLIVE;

        private volatile static int controls; //priznaky na zbernici

        public static void setControls(int newControls) {
            controls = newControls;
        }

        public ControlBusCommunicator(Board board, Socket interfaceSocket, int byteNr) {
            super(board, interfaceSocket, byteNr, ON_COLOR, OFF_COLOR);
        }

        @Override
        public void update() {
            Bus.getBus().drainPermits();
            if ((controls & 1 << this.getByteNr()) != 0) {
                //if(this.getByteNr() == 8) System.out.println("CONTROL 8 HIGH");
//                System.out.println("CONTOL BUS nastavenie priznaku" + getByteNr() + " HIGH");
                this.setHigh();
            } else {
                // if(this.getByteNr() == 8) System.out.println("CONTROL 8 LOW");
//                System.out.println("CONTOL BUS nastavenie priznaku" + getByteNr() + " LOW");
                this.setLow();
            }
        }

		@Override
		public void simulate() {

			//simulujeme zapis na zbernicu ak sa jedna o vstupny signal
            switch (this.getByteNr()) {
                case 2:
                    if (this.isHigh(this.getPin()))//kontrola isHigh zapina aj indikator
                        Bus.getBus().setRY(true);
                    else {
                        Bus.getBus().setRY(true);
                        this.turnIndicatorOff();
                    }
                    break;
                case 3:
                    if (this.isHigh(this.getPin()))
                        Bus.getBus().setIT(true);
                    else {
                        Bus.getBus().setIT(false);
                        this.turnIndicatorOff();
                    }
                    break;
            }
        }

        @Override
        protected void initPin() {
            if (this.getByteNr() == 2 || this.getByteNr() == 3)
                this.pin = new InputPin(this, "Control Input Pin " + this.getByteNr());
            else
                this.pin = new OutputPin(this, "Control Output Pin " + this.getByteNr());
        }
	}
}