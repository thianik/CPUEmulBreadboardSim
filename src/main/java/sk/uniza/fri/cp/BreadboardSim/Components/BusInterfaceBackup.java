//package sk.uniza.fri.cp.BreadboardSim.Components;
//
//import javafx.application.Platform;
//import javafx.scene.Group;
//import javafx.scene.paint.Color;
//import javafx.scene.paint.Paint;
//import javafx.scene.shape.Circle;
//import javafx.scene.shape.Line;
//import javafx.scene.shape.Rectangle;
//import javafx.scene.text.Font;
//import javafx.scene.text.Text;
//import sk.uniza.fri.cp.BreadboardSim.*;
//import sk.uniza.fri.cp.BreadboardSim.Devices.Device;
//import sk.uniza.fri.cp.Bus.Bus;
////import javax.swing.event.ChangeListener;
//
///**
// * Pripojenie na zbernice - address, data, control
// * @author Moris
// * @version 1.0
// * @created 17-mar-2017 16:16:34
// */
//public class BusInterfaceBackup extends Component {
//
//    private static final Color ADDRESS_LED_ON = Color.RED;
//    private static final Color ADDRESS_LED_OFF = Color.DARKRED;
//    private static final Color DATA_LED_ON = Color.LIME;
//    private static final Color DATA_LED_OFF = Color.SEAGREEN;
//    private static final Color CONTROL_LED_ON = Color.YELLOW;
//    private static final Color CONTROL_LED_OFF = Color.OLIVE;
//
//	private static int id = 1;
//	private Bus bus;
//
//	private Rectangle background;
//
//	private Group addressSockets;
//	private Group dataSockets;
//	private Group controlSockets;
//	private Group addressLeds;
//	private Group dataLeds;
//	private Group controlLeds;
//
//	private Circle[] addressLedsArray;
//	private Circle[] dataLedsArray;
//	private Circle[] controlLedsArray;
//
//	public BusInterfaceBackup(Board board, Bus bus){
//		super(board, id++);
//		this.bus = bus;
//
//		//grafika
//		GridSystem grid = getBoard().getGrid();
//
//        this.gridWidth= grid.getSizeX() * 66; //68
//        this.gridHeight = grid.getSizeY() * 5;
//		background = new Rectangle(this.gridWidth, this.gridHeight, Color.rgb(51,100,68));
//
//		//Adresna zbernica
//		addressSockets = new Group();
//		addressLeds = new Group();
//		addressLedsArray = new Circle[16];
//		Group addressInterface = generateInterface(16, addressSockets, SocketType.OUT, addressLeds, addressLedsArray, ADDRESS_LED_OFF, "AB");
//		addressInterface.setLayoutX(grid.getSizeX() * 15);
//		addressInterface.setLayoutY(grid.getSizeY() * 4);
//
//		//Datova zbernica
//		dataSockets = new Group();
//		dataLeds = new Group();
//		dataLedsArray = new Circle[8];
//		Group dataInterface = generateInterface(8, dataSockets, SocketType.OCO, dataLeds, dataLedsArray, DATA_LED_OFF, "DB");	//TODO datova zbernica OCO?
//		dataInterface.setLayoutX(grid.getSizeX() * 36);
//		dataInterface.setLayoutY(grid.getSizeY() * 4);
//
//		//Riadiaca zbernica
//		Group controlInterface = generateControlInterface();
//		controlInterface.setLayoutX(grid.getSizeX() * 45);
//		controlInterface.setLayoutY(grid.getSizeY() * 4);
//
//		//GND
//        Group leftGndSockets = SocketsFactory.getHorizontalPower(this, 1, 2, Potential.Value.LOW, getPowerSockets());
//        Text leftGndText = Board.getLabelText("GND", grid.getSizeMin());
//        leftGndText.setLayoutX(-grid.getSizeX()/2.0);
//        leftGndText.setLayoutY(-grid.getSizeY());
//        leftGndSockets.getChildren().add(leftGndText);
//        leftGndSockets.setLayoutX(grid.getSizeX() * 8);
//        leftGndSockets.setLayoutY(grid.getSizeY() * 4);
//
//        Group rightGndSockets = SocketsFactory.getHorizontalPower(this, 1, 2, Potential.Value.LOW, getPowerSockets());
//        Text rightGndText = Board.getLabelText("GND", grid.getSizeMin());
//        rightGndText.setLayoutX(-grid.getSizeX()/2.0);
//        rightGndText.setLayoutY(-grid.getSizeY());
//        rightGndSockets.getChildren().add(rightGndText);
//        rightGndSockets.setLayoutX(grid.getSizeX() * 57);
//        rightGndSockets.setLayoutY(grid.getSizeY() * 4);
//
//        //super.sockets.addAll(getPowerSockets());
//
//        //update zobrazenia lediek pri zmene na zbernici
//        this.bus.getAddressBusEventStream().subscribe((number) -> {
//            for (int i = 0; i < 16; i++) {
//                if((1<<i & number.getNewValue().intValue()) != 0)
//                    changeLed("address", i, true);
//                else
//                    changeLed("address", i, false);
//            }
//
//        });
//
//        this.bus.getDataBusEventStream().subscribe((number) -> {
//            for (int i = 0; i < 8; i++) {
//                if((1<<i & number.getNewValue().intValue()) != 0)
//                    changeLed("data", i, true);
//                else
//                    changeLed("data", i, false);
//            }
//        });
//
//        this.bus.getControlBusEventStream().subscribe((number) ->{
//            for (int i = 0; i < 9; i++) {
//                if((1<<i & number.getNewValue().intValue()) != 0)
//                    changeLed("control", i, true);
//                else
//                    changeLed("control", i, false);
//            }
//        });
//
//		this.getChildren().addAll(background, addressInterface, dataInterface, controlInterface, leftGndSockets, rightGndSockets);
//	}
//
//	public void setBackgroundColor(Color newColor){
//	    background.setFill(newColor);
//    }
//
//
//    private void changeLed(String busType, int index, boolean value){
//        Color color;
//        Circle[] array;
//
//        switch (busType){
//            case "address": color = value?ADDRESS_LED_ON:ADDRESS_LED_OFF;
//                array = addressLedsArray;
//				((Socket) addressSockets.getChildren().get(index)).setPotential(value? Potential.Value.HIGH: Potential.Value.LOW);
//				break;
//            case "data": color = value?DATA_LED_ON:DATA_LED_OFF;
//                array = dataLedsArray;
//				((Socket) dataSockets.getChildren().get(index)).setPotential(value? Potential.Value.HIGH: Potential.Value.LOW);
//                break;
//            case "control": color = value?CONTROL_LED_ON:CONTROL_LED_OFF;
//                array = controlLedsArray;
//				((Socket) controlSockets.getChildren().get(index)).setPotential(value? Potential.Value.HIGH: Potential.Value.LOW);
//                break;
//            default: color = Color.BLACK;
//                array = addressLedsArray;
//        }
//
//        array[index].setFill(color);
//    }
//
//	private Group generateInterface(int socketsNumber, Group socketsGroup, SocketType socketType, Group ledsGroup, Circle[] ledsArray, Color ledDefColor, String nameLabel){
//		GridSystem grid = getBoard().getGrid();
//
//		Group labels = new Group();
//
//		//generovanie soketov, lediek a oznaceni
//		for (int i = 0; i < socketsNumber; i++) {
//			double centerX = grid.getSizeX() * (socketsNumber-i);
//
//			//soket
//			Socket socket = new Socket(this, i);
//			socket.setType(socketType);
//			socket.setLayoutX(centerX);
//			socketsGroup.getChildren().add(socket);
//			//super.sockets.add(socket);
//
//			//led
//			Circle led = new Circle(grid.getSizeX() * 0.4, ledDefColor);
//			led.setCenterX(centerX);
//			led.setCenterY(-grid.getSizeY());
//			ledsGroup.getChildren().add(led);
//			ledsArray[i] = led;
//
//			//oznacenie
//			Text label = Board.getLabelText(String.valueOf(i), grid.getSizeMin());
//			label.setLayoutX(centerX - label.getBoundsInLocal().getWidth()/2.0);
//			labels.getChildren().add(label);
//		}
//
//		labels.setLayoutY(-grid.getSizeY()*2);
//
//		Text name = Board.getLabelText(nameLabel, grid.getSizeMin());
//		name.setLayoutX(-grid.getSizeX() * 3);
//
//		return new Group(socketsGroup, ledsGroup, labels, name);
//	}
//
//	private Group generateControlInterface(){
//		GridSystem grid = getBoard().getGrid();
//
//		Group labels = new Group();
//
//		controlSockets = new Group();
//		controlLeds = new Group();
//		controlLedsArray = new Circle[9];
//
//		//generovanie soketov, lediek a oznaceni
//		for (int i = 0; i < 9; i++) {
//			double centerX = grid.getSizeX() * (9-i);
//
//			//soket
//			Socket socket = new Socket(this, i);
//			socket.setType(SocketType.OCO);
//			socket.setLayoutX(centerX);
//			controlSockets.getChildren().add(socket);
//
//			//led
//			Circle led = new Circle(grid.getSizeX() * 0.4, Color.OLIVE);
//			led.setCenterX(centerX);
//			led.setCenterY(-grid.getSizeY());
//			controlLeds.getChildren().add(led);
//			controlLedsArray[i] = led;
//
//			//oznacenie
//			String lb = "";
//			switch (i){
//				case 0:	lb = "BA";
//					break;
//				case 1:	lb = "BQ";
//					break;
//				case 2:	lb = "RY";
//					break;
//				case 3:	lb = "IT";
//					break;
//				case 4:	lb = "IA";
//					break;
//				case 5:	lb = "IR";
//					break;
//				case 6:	lb = "IW";
//					break;
//				case 7:	lb = "MR";
//					break;
//				case 8:	lb = "MW";
//					break;
//			}
//			Text text = new Text(lb) ;
//			//label.setPrefWidth(grid.getSizeX());
//			text.setId("breadboardLabel");
//			text.setFont(new Font(grid.getSizeX()));
//            text.setStrokeWidth(0);
//			text.setLayoutX(centerX - text.getBoundsInLocal().getWidth() / 2.0);
//			labels.getChildren().add(text);
//
//			if(i % 2 != 0) text.setLayoutY(-grid.getSizeY());	//posunutie oznacenia hore
//
//			//negovacia ciarka nad oznacenim
//			if(i >= 3) {
//				Line negLogic = new Line(text.getLayoutX(),
//                        text.getLayoutY() - grid.getSizeX() + 2,
//                        text.getLayoutX() + text.getBoundsInLocal().getWidth(),
//                        text.getLayoutY() - grid.getSizeX() + 2);
//				negLogic.setId("breadboardLabel");
//				labels.getChildren().add(negLogic);
//			}
//
//
//		}
//
//		labels.setLayoutY(-grid.getSizeY() * 2);
//
//		return new Group(controlSockets, controlLeds, labels);
//	}
//
//	private abstract static class BusCommunicator extends Device{
//
//		protected Pin pin;
//
//		private static final double RADIUS_COEF = 0.4;
//
//		private Socket internalSocket;
//		private Circle indicator;
//		private Paint onColor;
//		private Paint offColor;
//
//		public BusCommunicator(Board board, Component component, Socket interfaceSocket, Paint onColor, Paint offColor) {
//			super(board);
//
//			this.onColor = onColor;
//			this.offColor = offColor;
//
//			//vytvorenie potencialu medzi vnutornym a vonkajsim soketom
//			this.internalSocket = new Socket(component, 0);
//			new Potential(this.internalSocket, interfaceSocket);
//
//			GridSystem grid = board.getGrid();
//			this.indicator = new Circle(grid.getSizeMin() * RADIUS_COEF, offColor);
//
//			initPin();
//			this.internalSocket.connect(this.pin);
//
//			this.getChildren().addAll(indicator);
//		}
//
//		public void setHigh(){
//			if(this.pin.getState() != Pin.PinState.HIGH) {
//				this.pin.setState(Pin.PinState.HIGH);
//				updateIndicator(true);
//			}
//		}
//
//		public void setLow(){
//			if(this.pin.getState() != Pin.PinState.LOW) {
//				this.pin.setState(Pin.PinState.LOW);
//				updateIndicator(false);
//			}
//		}
//
//		public void setHighImpedance(boolean indicatorOn){
//			if(this.pin.getState() != Pin.PinState.HIGH_IMPEDANCE) {
//				this.pin.setState(Pin.PinState.HIGH_IMPEDANCE);
//				updateIndicator(indicatorOn);
//			}
//		}
//
//		@Override
//		public void simulate() {
//			super.simulate();
//		}
//
//		void updateIndicator(boolean turnOn){
//			if(Platform.isFxApplicationThread())
//				updateColor(turnOn);
//			else
//				Platform.runLater(()->updateColor(turnOn));
//
//		}
//
//		private void updateColor(boolean turnOn){
//			if(turnOn)
//				this.indicator.setFill(onColor);
//			else
//				this.indicator.setFill(offColor);
//		}
//
//		abstract void initPin();
//	}
//
//	private static class AddressBusCommunicator extends BusCommunicator{
//
//		private static final Paint ON_COLOR = Color.RED;
//		private static final Paint OFF_COLOR = Color.DARKRED;
//
//		public AddressBusCommunicator(Board board, Component component, Socket interfaceSocket) {
//			super(board, component, interfaceSocket, ON_COLOR, OFF_COLOR);
//		}
//
//		@Override
//		void initPin() {
//			this.pin = new OutputPin(this);
//		}
//	}
//
//	private static class DataBusCommunicator extends BusCommunicator{
//
//		private static final Paint ON_COLOR = Color.LIME;
//		private static final Paint OFF_COLOR = Color.SEAGREEN;
//
//		private static int data;
//
//		public DataBusCommunicator(Board board, Component component, Socket interfaceSocket) {
//			super(board, component, interfaceSocket, ON_COLOR, OFF_COLOR);
//		}
//
//		@Override
//		public void simulate() {
//			super.simulate();
//		}
//
//		@Override
//		void initPin() {
//			this.pin = new InputOutputPin(this);
//		}
//	}
//
//	private static class ControlBusCommunicator extends BusCommunicator{
//
//		private static final Paint ON_COLOR = Color.YELLOW;
//		private static final Paint OFF_COLOR = Color.OLIVE;
//
//		private static int control;
//
//		private String inputSignal;
//
//		public ControlBusCommunicator(Board board, Component component, Socket interfaceSocket) {
//			super(board, component, interfaceSocket, ON_COLOR, OFF_COLOR);
//		}
//
//		public ControlBusCommunicator(Board board, Component component, Socket interfaceSocket, String inputSignal) {
//			super(board, component, interfaceSocket, ON_COLOR, OFF_COLOR);
//			this.inputSignal = inputSignal;
//		}
//
//		@Override
//		public void simulate() {
//			super.simulate();
//
//			//simulujeme zapis na zbernicu ak sa jedna o vstupny signal
//			if(inputSignal != null){
//				switch (inputSignal.toUpperCase()){
//					case "IT": Bus.getBus().setIT(this.pin.getState() == Pin.PinState.HIGH);
//					break;
//					case "RY": Bus.getBus().setRY(this.pin.getState() == Pin.PinState.HIGH);
//					break;
//				}
//
//				//pri vstupnom signaly je indikator zavisly na aktualnom vstupe
//				updateIndicator(this.pin.getState() == Pin.PinState.HIGH);
//			}
//		}
//
//		@Override
//		void initPin() {
//			if(inputSignal != null)
//				this.pin = new InputPin(this);
//			else
//				this.pin = new OutputPin(this);
//		}
//	}
//}