package sk.uniza.fri.cp.BreadboardSim.Devices.Chips;

import javafx.geometry.Insets;
import javafx.scene.Group;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.*;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import sk.uniza.fri.cp.BreadboardSim.*;
import sk.uniza.fri.cp.BreadboardSim.Devices.Device;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

/**
 * @author Moris
 * @version 1.0
 * @created 17-mar-2017 16:16:34
 */
public abstract class Chip extends Device {

	private Pin[] pins;

	public Chip(){}

	public Chip(Board board, int pinsCount){
		super(board);
		this.pins = new Pin[pinsCount];

		fillPins();


		this.getChildren().add(generateGraphic());

	}

	public Pin getPin(int index){
		if(index < pins.length)
			return pins[index];
		return null;
	}

	@Override
	public List<Pin> getPins() {
		return Arrays.asList(this.pins);
	}

	public boolean isPinNumberValid(int pinNumber){
		return pinNumber > 0 && pinNumber <= this.pins.length;
	}

	public boolean isPowered(int vccPinNumber, int gndPinNumber){
		if(this.isPinNumberValid(vccPinNumber) && this.isPinNumberValid(gndPinNumber)) {
			Socket vccSocket = this.pins[vccPinNumber - 1].getSocket();
			Socket gndSocket = this.pins[gndPinNumber - 1].getSocket();
			if(vccSocket != null && gndSocket != null)
				return vccSocket.getPotential().getValue() == Potential.Value.HIGH
					&& gndSocket.getPotential().getValue() == Potential.Value.LOW;
		}
		return false; //TODO vynimka
	}

	public boolean isHigh(int inputPinNumber) {
		return inputPinNumber > 0 && inputPinNumber <= this.pins.length && isHigh(this.pins[inputPinNumber - 1]);
	}

	public boolean isLow(int inputPinNumber) {
		return inputPinNumber > 0 && inputPinNumber <= this.pins.length && isLow(this.pins[inputPinNumber - 1]);
	}

	/**
	 * Pred vytvorenim grafiky je nutne naplnit pole pinov
	 */
	protected abstract void fillPins();


	/**
	 * Zaregistruje cipu pin s cislom
	 * @param pinNumber Cislo pinu na cipe
	 * @param pin Pin, ktory sa ma registrovat na cip
	 */
	protected void registerPin(int pinNumber, Pin pin){
		//TODO vynimka? aj v setAllPins?
		if(pinNumber > 0 && pinNumber <= this.pins.length) {
			this.pins[pinNumber-1] = pin;
		}
	}

	protected void setPin(int pinNumber, Pin.PinState state){
		if(pinNumber > 0 && pinNumber <= this.pins.length)
			setPin(this.pins[pinNumber-1], state);
	}

	/**
	 * Zaregistruje postupne vsetky piny od daneho cisla
	 * @param fromNumber Cislo prveho pinu
	 * @param pins Piny na registraciu
	 */
	boolean setAllPins(int fromNumber, Pin... pins){
		if(fromNumber > 0 && fromNumber + pins.length <= this.pins.length) {
			System.arraycopy(pins, 0, this.pins, fromNumber-1, pins.length);
			return true;
		}
		return false;
	}

	public abstract String getName();

	private Group generateGraphic(){
		Group graphic = new Group();
		GridSystem grid = getBoard().getGrid();

		double gridHeight = 3; //vyska cipu v jednotkach policok
		double heightReduction = grid.getSizeY() * 0.3 ; //zmensenie na jednej strane

		double width = grid.getSizeX() * (this.pins.length / 2) + grid.getSizeX();
		double height = grid.getSizeY() * gridHeight - heightReduction*2;

		double topBevel = grid.getSizeY()*0.3;
		double leftBevel = grid.getSizeX()*0.2;
		Rectangle bodyLayer0 = new Rectangle(width, height, Color.BLACK);
		Rectangle bodyLayer1 = new Rectangle(width-leftBevel, height-topBevel, Color.rgb(64,64,64));
		Rectangle bodyLayer2 = new Rectangle(width-2*leftBevel, height-2*topBevel, Color.rgb(48,48,48));
		bodyLayer2.setLayoutX(leftBevel);
		bodyLayer2.setLayoutY(topBevel);

		Arc bodyMarkTop = new Arc(0, height/2, grid.getSizeY()*0.5, grid.getSizeY()*0.5, 0, 90);
		bodyMarkTop.setFill(Color.rgb(28,28,28));
		bodyMarkTop.setType(ArcType.ROUND);
		Arc bodyMarkBottom = new Arc(0, height/2, grid.getSizeY()*0.5, grid.getSizeY()*0.5, -90, 90);
		bodyMarkBottom.setFill(Color.rgb(56,56,56));
		bodyMarkBottom.setType(ArcType.ROUND);
		Arc bodyMarkMiddle = new Arc(0, height/2, grid.getSizeY()*0.25, grid.getSizeY()*0.25, -90, 180);
		bodyMarkMiddle.setFill(Color.rgb(38,38,38));
		bodyMarkMiddle.setType(ArcType.ROUND);
		bodyMarkMiddle.setStrokeWidth(0);

		Group body = new Group(bodyLayer0, bodyLayer1, bodyLayer2, bodyMarkTop, bodyMarkBottom, bodyMarkMiddle);
		body.setLayoutX(-grid.getSizeX());
		body.setLayoutY(heightReduction);

		Group legs = new Group();
		final double legWidth = grid.getSizeX()*2.0/3.0;
		final double legHeight = grid.getSizeY()*0.7;
		for (int i = 0; i < this.pins.length; i++){
			Polygon legBase = new Polygon(0, 0,
					legWidth, 0,
					legWidth, legHeight/4.0,
					legWidth/2.0, legHeight/2.0,
					0, legHeight/4.0);
			legBase.setFill(Color.rgb(140,140,140));
			Rectangle legPin = new Rectangle(legWidth*0.5, legHeight);
			legPin.setFill(Color.rgb(140,140,140));
			legPin.setLayoutX(legWidth/2 - legPin.getWidth()/2.0);

			//nastavenie pozicie odpovedajuceho pinu pre pripojenie k soketu
			this.pins[i].setCenterX(legWidth/2.0);
			this.pins[i].setCenterY(legHeight*0.7);
			Group leg = new Group(legBase, legPin, this.pins[i]);

			if(i < this.pins.length/2){
				//spodne nozicky
				leg.setLayoutX(grid.getSizeX() * i - legWidth/2);
				leg.setLayoutY(grid.getSizeY() * gridHeight - legHeight*0.7);
			} else {
				//horne nozicky
				leg.setRotate(180);
				leg.setLayoutX(grid.getSizeX() * ( this.pins.length - i - 1 ) - legWidth/2);
				leg.setLayoutY(-legHeight+legHeight*0.7);
			}
			legs.getChildren().add(leg);
		}

		Text label = new Text(getName());
		label.setFont(Font.font(grid.getSizeY()*1.5));
		label.setFill(Color.WHITE);
		label.setLayoutY(grid.getSizeY()*2);

		graphic.getChildren().addAll(body, legs, label);

		this.getChildren().add(new Pane(graphic));

		return graphic;
	}

	protected static Pane generateItemImage(String chipName, int pinsCount){

		Group graphic = new Group();

		double width = 10 * (pinsCount / 2);
		double height = 30;

		double topBevel = 10*0.3;
		double leftBevel = 10*0.2;
		Rectangle bodyLayer0 = new Rectangle(width, height, Color.BLACK);
		Rectangle bodyLayer1 = new Rectangle(width-leftBevel, height-topBevel, Color.rgb(64,64,64));
		Rectangle bodyLayer2 = new Rectangle(width-2*leftBevel, height-2*topBevel, Color.rgb(48,48,48));
		bodyLayer2.setLayoutX(leftBevel);
		bodyLayer2.setLayoutY(topBevel);

		Arc bodyMarkTop = new Arc(0, height/2, 10*0.5, 10*0.5, 0, 90);
		bodyMarkTop.setFill(Color.rgb(28,28,28));
		bodyMarkTop.setType(ArcType.ROUND);
		Arc bodyMarkBottom = new Arc(0, height/2, 10*0.5, 10*0.5, -90, 90);
		bodyMarkBottom.setFill(Color.rgb(56,56,56));
		bodyMarkBottom.setType(ArcType.ROUND);
		Arc bodyMarkMiddle = new Arc(0, height/2, 10*0.25, 10*0.25, -90, 180);
		bodyMarkMiddle.setFill(Color.rgb(38,38,38));
		bodyMarkMiddle.setType(ArcType.ROUND);
		bodyMarkMiddle.setStrokeWidth(0);

		Group body = new Group(bodyLayer0, bodyLayer1, bodyLayer2, bodyMarkTop, bodyMarkBottom, bodyMarkMiddle);
		body.setLayoutX(-10/2);

		Group legs = new Group();
		final double legWidth = 5*2/3;
		final double legHeight = 5*0.7;
		for (int i = 0; i < pinsCount; i++){
			Polygon legBase = new Polygon(0, 0,
					legWidth, 0,
					legWidth, legHeight/4.0,
					legWidth/2.0, legHeight/2.0,
					0, legHeight/4.0);
			legBase.setFill(Color.rgb(140,140,140));
			Rectangle legPin = new Rectangle(legWidth*0.5, legHeight);
			legPin.setFill(Color.rgb(140,140,140));
			legPin.setLayoutX(legWidth/2 - legPin.getWidth()/2.0);

			Group leg = new Group(legBase, legPin);

			if(i < pinsCount/2){
				//spodne nozicky
				leg.setLayoutX(10 * i - legWidth/2);
				leg.setLayoutY(10 * legHeight*0.7);
			} else {
				//horne nozicky
				leg.setRotate(180);
				leg.setLayoutX(10 * ( pinsCount - i - 1 ) - legWidth/2);
				leg.setLayoutY(-legHeight+legHeight*0.7);
			}
			legs.getChildren().add(leg);
		}

		Text label = new Text(chipName);
		label.setFill(Color.WHITE);
		label.setFont(Font.font(10*1.5));
		label.setLayoutY(10*2);
		label.setLayoutX(10);

		graphic.getChildren().addAll(body, legs, label);
		graphic.setLayoutX(10/2);

		return new Pane(graphic);
	}
}