package sk.uniza.fri.cp.BreadboardSim.Components;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import sk.uniza.fri.cp.BreadboardSim.Board.Board;
import sk.uniza.fri.cp.BreadboardSim.Board.GridSystem;
import sk.uniza.fri.cp.BreadboardSim.Devices.Device;
import sk.uniza.fri.cp.BreadboardSim.Devices.Pin.InputOutputPin;
import sk.uniza.fri.cp.BreadboardSim.Devices.Pin.InputPin;
import sk.uniza.fri.cp.BreadboardSim.Devices.Pin.OutputPin;
import sk.uniza.fri.cp.BreadboardSim.Devices.Pin.Pin;
import sk.uniza.fri.cp.BreadboardSim.Socket.Potential;
import sk.uniza.fri.cp.BreadboardSim.Socket.Socket;
import sk.uniza.fri.cp.BreadboardSim.Socket.SocketType;
import sk.uniza.fri.cp.BreadboardSim.Socket.SocketsFactory;
import sk.uniza.fri.cp.Bus.Bus;

import java.util.List;
//import javax.swing.event.ChangeListener;

/**
 * Pripojenie na zbernice - address, data, control
 * @author Moris
 * @version 1.0
 * @created 17-mar-2017 16:16:34
 */
public class BusInterface extends Component {

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

	public BusInterface(Board board, Bus bus){
        super(board);
        this.bus = bus;

		//grafika
		GridSystem grid = getBoard().getGrid();

        this.gridWidth= grid.getSizeX() * 66; //68
        this.gridHeight = grid.getSizeY() * 5;
		background = new Rectangle(this.gridWidth, this.gridHeight, Color.rgb(51,100,68));

		//GND
        Group leftGndSockets = SocketsFactory.getHorizontalPower(this, 2, Potential.Value.LOW, getPowerSockets());
        Text leftGndText = Board.getLabelText("GND", grid.getSizeMin());
        leftGndText.setLayoutX(-grid.getSizeX()/2.0);
        leftGndText.setLayoutY(-grid.getSizeY());
        leftGndSockets.getChildren().add(leftGndText);
        leftGndSockets.setLayoutX(grid.getSizeX() * 8);
        leftGndSockets.setLayoutY(grid.getSizeY() * 4);

        Group rightGndSockets = SocketsFactory.getHorizontalPower(this, 2, Potential.Value.LOW, getPowerSockets());
        Text rightGndText = Board.getLabelText("GND", grid.getSizeMin());
        rightGndText.setLayoutX(-grid.getSizeX()/2.0);
        rightGndText.setLayoutY(-grid.getSizeY());
        rightGndSockets.getChildren().add(rightGndText);
        rightGndSockets.setLayoutX(grid.getSizeX() * 57);
        rightGndSockets.setLayoutY(grid.getSizeY() * 4);



        this.bus.addressBusProperty().addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
                for (int i = 0; i < 16; i++) {
                    if((1<<i & newValue.intValue()) != 0)
                        addressBusCommunicators[i].setHigh();
                    else
                        addressBusCommunicators[i].setLow();
                }
            }
        });

        this.bus.dataBusProperty().addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
                //nastavenie aktualnych dat na datovej zbernici pre vsetky instancie komunikatora (ledky)
                DataBusCommunicator.data = newValue.intValue();
                for (int i = 0; i < 8; i++) {
                    //0x140 -> 1 0100 0000 => signal zapisu MW/IW je v nule
                    if((bus.controlBusProperty().getValue() & 0x140) != 0x140) {
                        //zapis
                        ((DataBusCommunicator) dataBusCommunicators[i]).setToWrite(true);
                    } else {
                        ((DataBusCommunicator) dataBusCommunicators[i]).setToWrite(false);
                    }

                    ((DataBusCommunicator) dataBusCommunicators[i]).update();
                }
            }
        });

        this.bus.controlBusProperty().addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
                for (int i = 4; i < 9; i++) { //iba pre vystupne signaly
                    if((1<<i & newValue.intValue()) != 0)
                        controlBusCommunicators[i].setHigh();
                    else
                        controlBusCommunicators[i].setLow();
                }

                //MR/IR
                if((newValue.intValue() & 0xA0) != 0xA0) {
                    //citanie
                    for (int i = 0; i < 8; i++)
                        ((DataBusCommunicator) dataBusCommunicators[i]).setToRead(true);
                } else {
                    for (int i = 0; i < 8; i++)
                        ((DataBusCommunicator) dataBusCommunicators[i]).setToRead(false);
                }
            }
        });

        //update zobrazenia lediek pri zmene na zbernici
        /*this.bus.getAddressBusEventStream().subscribe((number) -> {
            for (int i = 0; i < 16; i++) {
                if((1<<i & number.getNewValue().intValue()) != 0)
                    addressBusCommunicators[i].setHigh();
                else
					addressBusCommunicators[i].setLow();
            }
        });*/

       /* this.bus.getDataBusEventStream().subscribe((number) -> {
        	//nastavenie aktualnych dat na datovej zbernici pre vsetky instancie komunikatora (ledky)
        	DataBusCommunicator.data = number.getNewValue().intValue();
			for (int i = 0; i < 8; i++) {
                //0x140 -> 1 0100 0000 => signal zapisu MW/IW je v nule
                if((this.bus.controlBusProperty().getValue() & 0x140) != 0x140) {
                    //zapis
                    ((DataBusCommunicator) dataBusCommunicators[i]).setToWrite(true);
                } else {
                    ((DataBusCommunicator) dataBusCommunicators[i]).setToWrite(false);
                }

                ((DataBusCommunicator) dataBusCommunicators[i]).update();
            }
        });*/

        /*this.bus.getControlBusEventStream().subscribe((number) ->{
            for (int i = 4; i < 9; i++) { //iba pre vystupne signaly
                if((1<<i & number.getNewValue().intValue()) != 0)
                    controlBusCommunicators[i].setHigh();
                else
					controlBusCommunicators[i].setLow();
            }

            //MR/IR
            if((number.getNewValue().intValue() & 0xA0) != 0xA0) {
				//citanie
                for (int i = 0; i < 8; i++)
                    ((DataBusCommunicator) dataBusCommunicators[i]).setToRead(true);
            } else {
                for (int i = 0; i < 8; i++)
                    ((DataBusCommunicator) dataBusCommunicators[i]).setToRead(false);
            }
        });*/



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
	}

	public void setBackgroundColor(Color newColor){
	    background.setFill(newColor);
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
			AddressBusCommunicator communicator = new AddressBusCommunicator(getBoard(), this, addressSockets[i], i);
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
			DataBusCommunicator communicator = new DataBusCommunicator(getBoard(), this, dataSockets[i], i);
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
			ControlBusCommunicator communicator = new ControlBusCommunicator(getBoard(), this, controlSockets[i], i);
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

		protected Pin pin;
		protected int byteNr; //cislo bitu na zbernici od najnizsieho radu
        protected Socket interfaceSocket;

		private static final double RADIUS_COEF = 0.4;

		private Circle indicator;
		private Paint onColor;
		private Paint offColor;
		private volatile boolean indicatorOn;

		public BusCommunicator(Board board, Component component, Socket interfaceSocket, int byteNr, Paint onColor, Paint offColor) {
			super(board);

			this.interfaceSocket = interfaceSocket;
			this.byteNr = byteNr;
			this.onColor = onColor;
			this.offColor = offColor;

			GridSystem grid = board.getGrid();
			this.indicator = new Circle(grid.getSizeMin() * RADIUS_COEF, offColor);

			initPin();
			this.interfaceSocket.connect(this.pin);
            this.interfaceSocket.lockPin();

			this.getChildren().addAll(indicator);
			this.makeImmovable();
		}

		public void setHigh(){
			setPin(this.pin, Pin.PinState.HIGH);
            this.indicatorOn = true;
            updateGraphic();
		}

		public void setLow(){
            setPin(this.pin, Pin.PinState.LOW);
            this.indicatorOn = false;
            updateGraphic();
		}

        @Override
        public List<Pin> getPins() {
            return null;
        }

        @Override
		public void simulate() {
            this.indicatorOn = isHigh(this.pin);
			updateGraphic();
		}

        @Override
        protected void updateGraphic() {
            super.updateGraphic();

            if(this.indicatorOn)
                this.indicator.setFill(onColor);
            else
                this.indicator.setFill(offColor);
        }

        @Override
        public void reset() {

        }

        abstract void initPin();
	}

	private static class AddressBusCommunicator extends BusCommunicator{

		private static final Paint ON_COLOR = Color.RED;
		private static final Paint OFF_COLOR = Color.DARKRED;

		public AddressBusCommunicator(Board board, Component component, Socket interfaceSocket, int byteNr) {
			super(board, component, interfaceSocket, byteNr, ON_COLOR, OFF_COLOR);
		}

		@Override
		void initPin() {
			this.pin = new OutputPin(this);
		}
	}

	private static class DataBusCommunicator extends BusCommunicator{

		private static final Paint ON_COLOR = Color.LIME;
		private static final Paint OFF_COLOR = Color.SEAGREEN;


        private boolean read; //nastavenie ako vstupu
        private boolean write; //nastavenie ako vystupu

		public DataBusCommunicator(Board board, Component component, Socket interfaceSocket, int byteNr) {
			super(board, component, interfaceSocket, byteNr, ON_COLOR, OFF_COLOR);
			this.read = false;
		}

        public void setToRead(boolean read){
            this.read = read;
            if(read) {
                this.pin.setState(Pin.PinState.HIGH_IMPEDANCE);
                writeToBus();
            }
        }

        public void setToWrite(boolean write){
            this.write = write;
            update();
        }

        private static int data; //aktualne data na zbernici

		public void writeToBus(){
			int dataToWrite;

			if(this.pin.getState() == Pin.PinState.HIGH)
				dataToWrite = data | (1<<this.byteNr);
			else
				dataToWrite = data & ~(1<<this.byteNr);

			Bus.getBus().setDataBus((byte)dataToWrite);
		}

		/**
		 * Aktualizacia po zmene dat na zbernici
		 */
		public void update(){

            //TODO WTF... cele je to divne... zatial to funguje ale treba to premysliet
            if(this.write //ak je pozardovany zapis zo zbernice do pamate -> vsetky vystupy sa nastavuju podla zbernice
                    || this.interfaceSocket.getThisPotential().getChild() == null  //nije pripojeny kablik
                    || ( this.interfaceSocket.getThisPotential().getChild() != null //alebo je ale nie je to vystup
                            && this.interfaceSocket.getPotential().getType() != SocketType.OUT))
            {
                if((data & (1<<this.byteNr)) != 0) {
                    setHigh();
                } else {
                    setLow();
                }
            }
		}

		@Override
		public void simulate() {
			super.simulate();

            update();

			//ak prebieha citanie, zapis novu hodnotu na zbernicu
			if(read){
                writeToBus();
            }
		}

		@Override
		void initPin() {
			this.pin = new InputOutputPin(this);
		}
	}

	private static class ControlBusCommunicator extends BusCommunicator{

		private static final Paint ON_COLOR = Color.YELLOW;
		private static final Paint OFF_COLOR = Color.OLIVE;

		public ControlBusCommunicator(Board board, Component component, Socket interfaceSocket, int byteNr) {
			super(board, component, interfaceSocket, byteNr, ON_COLOR, OFF_COLOR);
		}

		@Override
		public void simulate() {
			super.simulate();

			//simulujeme zapis na zbernicu ak sa jedna o vstupny signal
			switch (this.byteNr){
				case 2: Bus.getBus().setRY(this.pin.getState() == Pin.PinState.HIGH);
					//pri vstupnom signaly je indikator zavisly na aktualnom vstupe
					//updateIndicator(this.pin.getState() == Pin.PinState.HIGH);
					break;
				case 3: Bus.getBus().setIT(this.pin.getState() == Pin.PinState.HIGH);
					//updateIndicator(this.pin.getState() == Pin.PinState.HIGH);
					break;
			}
		}

		@Override
		void initPin() {
			if(this.byteNr == 2 || this.byteNr == 3)
				this.pin = new InputPin(this);
			else
				this.pin = new OutputPin(this);
		}
	}
}