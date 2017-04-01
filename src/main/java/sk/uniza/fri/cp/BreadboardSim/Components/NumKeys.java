package sk.uniza.fri.cp.BreadboardSim.Components;

import com.sun.org.apache.bcel.internal.generic.PUSH;
import com.sun.org.apache.xpath.internal.operations.Bool;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.scene.Group;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;

import javafx.scene.text.Font;
import javafx.scene.text.Text;
import sk.uniza.fri.cp.BreadboardSim.*;

/**
 * Komponent s klavesnicou
 * @author Moris
 * @version 1.0
 * @created 17-mar-2017 16:16:35
 */
public class NumKeys extends Component {

	private static int id = 1;

	private Rectangle background;
	private Group buttonsGroup;

	private Socket[] columnSockets;
	private Socket[] rowSockets;

	//konetxtove menu
	private ContextMenu contextMenu;
	private MenuItem miChangeButtonBehaviour;
	private boolean twoClick;
	boolean leftMouseButtonDown;

	public NumKeys(Board board){
		super(board, id++);

		columnSockets = new Socket[4];
		rowSockets = new Socket[4];

		//grafika
		GridSystem grid = getBoard().getGrid();

		this.gridWidth = grid.getSizeX() * 22;
		this.gridHeight = grid.getSizeY() * 17;
		background = new Rectangle(this.gridWidth, this.gridHeight, Color.rgb(51,100,68));

		//sokety
		Group columnGroup = generateColumnSockets();
		columnGroup.setLayoutX(grid.getSizeX() * 6);
		columnGroup.setLayoutY(grid.getSizeY() * 2);

        Group rowGroup = generateRowSockets();
        rowGroup.setLayoutX(grid.getSizeX() * 15);
        rowGroup.setLayoutY(grid.getSizeY() * 2);

        //GND
		Group gndGroup = SocketsFactory.getHorizontalPower(this, 1, 4, Potential.Value.LOW, getPowerSockets());
		Text rowText = Board.getLabelText("GND", grid.getSizeMin());
		rowText.setLayoutX(grid.getSizeX() * 4);
		rowText.setLayoutY(rowText.getBoundsInParent().getHeight()/2.0);
		gndGroup.getChildren().add(rowText);

        gndGroup.setLayoutX(grid.getSizeX() * 15);
        gndGroup.setLayoutY(grid.getSizeY());

        //+5V
        Group vccGroup = SocketsFactory.getHorizontalPower(this, 1, 2, Potential.Value.HIGH, getPowerSockets());
        Text vccText = Board.getLabelText("+5V", grid.getSizeMin());
        vccText.setLayoutX(grid.getSizeX()/2.0 - vccText.getBoundsInLocal().getWidth()/2.0);
        vccText.setLayoutY( grid.getSizeY() * 1.5);
        vccGroup.getChildren().add(vccText);

        vccGroup.setLayoutX(grid.getSizeX() * 11);
        vccGroup.setLayoutY(grid.getSizeY() * 2);

		//tlacitka
		buttonsGroup = new Group();

		for (int y = 0; y < 4; y++) {
			for (int x = 0; x < 4; x++) {
				Button button = new Button(rowSockets[x], columnSockets[3-y]);
				button.setLayoutX(button.getWidth() * x);
				button.setLayoutY(button.getHeight() * (3-y));

				buttonsGroup.getChildren().add(button);
			}
		}
        buttonsGroup.setLayoutX(grid.getSizeX() * 5);
		buttonsGroup.setLayoutY(grid.getSizeY() * 4);

		this.getChildren().addAll(background, buttonsGroup, columnGroup, rowGroup, gndGroup, vccGroup);

		//kontextove menu pre zmenu spravania tlacitok
		this.miChangeButtonBehaviour = new MenuItem("Dva kliky");
		this.miChangeButtonBehaviour.setOnAction(event -> {
			twoClick = !twoClick;
			miChangeButtonBehaviour.setText(twoClick?"Jeden klik":"Dva kliky");
		});

		this.contextMenu = new ContextMenu();
		this.contextMenu.getItems().add(this.miChangeButtonBehaviour);

		this.addEventHandler(MouseEvent.MOUSE_PRESSED, event -> {
			leftMouseButtonDown = event.isSecondaryButtonDown();
			if(!leftMouseButtonDown && contextMenu.isShowing())
				contextMenu.hide();
			});

		this.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> {
			if(leftMouseButtonDown) {
				this.contextMenu.show(this, event.getScreenX(), event.getScreenY());
				leftMouseButtonDown = false;
			}
		});
	}

	private Group generateColumnSockets(){

		GridSystem grid = getBoard().getGrid();

		Group columnGroup = new Group();

		for (int i = 0; i < 4; i++) {
			Socket socket = new Socket(this, i);
			socket.setType(SocketType.OUT); //TODO column ma byt OUT? zmenit na ten WEAK_OUT
			socket.setPotential(Potential.Value.HIGH);

			socket.setLayoutX(grid.getSizeX() * i);

			Text numberText = new Text(String.valueOf(i+1));
			numberText.setFont(Font.font(grid.getSizeX()));
			numberText.setId("breadboardLabel");
			numberText.setStrokeWidth(0);
			numberText.setLayoutX(grid.getSizeX() * i - numberText.getBoundsInLocal().getWidth() / 2.0);
            numberText.setLayoutY(grid.getSizeY() * 1.5);

			columnSockets[i] = socket;
			columnGroup.getChildren().addAll(socket, numberText);
		}

		Text columnText = new Text("COLUMN");
		columnText.setFont(Font.font(grid.getSizeY()));
		columnText.setId("breadboardLabel");
		columnText.setStrokeWidth(0);
		columnText.setLayoutX(-columnText.getBoundsInParent().getWidth() - grid.getSizeX());
		columnText.setLayoutY(columnText.getBoundsInParent().getHeight()/2.0);
		columnGroup.getChildren().add(columnText);

		return columnGroup;
	}

    private Group generateRowSockets(){

        GridSystem grid = getBoard().getGrid();

        Group rowGroup = new Group();

        for (int i = 0; i < 4; i++) {
            Socket socket = new Socket(this, i);
            //socket.setType(SocketType.IN);
            socket.setLayoutX(grid.getSizeX() * (3-i) );

            Text numberText = new Text(String.valueOf(i+1));
            numberText.setFont(Font.font(grid.getSizeX()));
            numberText.setId("breadboardLabel");
            numberText.setStrokeWidth(0);
            numberText.setLayoutX(grid.getSizeX() * (3-i) - numberText.getBoundsInLocal().getWidth() / 2.0);
            numberText.setLayoutY(grid.getSizeY() * 1.5);

            rowSockets[3-i] = socket;
            rowGroup.getChildren().addAll(socket, numberText);
        }

        Text rowText = new Text("ROW");
        rowText.setFont(Font.font(grid.getSizeY()));
        rowText.setId("breadboardLabel");
        rowText.setStrokeWidth(0);
        rowText.setLayoutX(grid.getSizeX() * 4);
        rowText.setLayoutY(rowText.getBoundsInParent().getHeight()/2.0);
        rowGroup.getChildren().add(rowText);

        return rowGroup;
    }

	private class Button extends Region {
		private final Color DEFAULT_BUTTON_COLOR = Color.BLACK;
		private final Color DEFAULT_HOVER_BUTTON_COLOR = Color.DARKGRAY;
		private final Color PUSHED_BUTTON_COLOR = Color.RED;
		private final Color PUSHED_HOVER_BUTTON_COLOR = Color.DARKRED;

		private Socket row;
		private Socket column;

		//eventy
		private boolean mouseHover;
		private boolean pressed;

		//grafika
		private Circle button;
		private Group buttonBase;

		private double width;
		private double height;

		private EventHandler<MouseEvent> onMouseEnter = new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
            	if(pressed)
					button.setFill(PUSHED_HOVER_BUTTON_COLOR);
            	else
                	button.setFill(DEFAULT_HOVER_BUTTON_COLOR);

                mouseHover = true;

                event.consume();
            }
        };

        private EventHandler<MouseEvent> onMouseExit = new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
            	if(event.isPrimaryButtonDown())
                	button.setFill(PUSHED_BUTTON_COLOR);
            	else if(pressed)
            		button.setFill(PUSHED_HOVER_BUTTON_COLOR);
            	else
            		button.setFill(DEFAULT_BUTTON_COLOR);

            	mouseHover = false;

                event.consume();
            }
        };

        private EventHandler<MouseEvent> onMousePress = new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
            	if(event.isPrimaryButtonDown() && !contextMenu.isShowing()) {
					button.setFill(PUSHED_BUTTON_COLOR);

					pressed = !twoClick || !pressed;

					if (pressed)
						fireEvent(true);

					event.consume();
				}
            }
        };

		private EventHandler<MouseEvent> onMouseRelease = new EventHandler<MouseEvent>() {
			@Override
			public void handle(MouseEvent event) {

				if(!twoClick) pressed = false;

				if(!pressed) {
					fireEvent(false);

					if(mouseHover)
						button.setFill(DEFAULT_HOVER_BUTTON_COLOR);
					else
						button.setFill(DEFAULT_BUTTON_COLOR);
				} else {
					if(mouseHover)
						button.setFill(PUSHED_HOVER_BUTTON_COLOR);
					else
						button.setFill(PUSHED_BUTTON_COLOR);
				}

				event.consume();
			}
		};

		Button(Socket row, Socket column){
			this.row = row;
			this.column = column;

			GridSystem grid = getBoard().getGrid();

			width = grid.getSizeX()*3;
			height = grid.getSizeY()*3;

			this.setWidth(width);
			this.setHeight(height);

			//button core
			button = new Circle(grid.getSizeX(), Color.BLACK);
			button.setCenterX(width/2.0);
			button.setCenterY(height/2.0);

			//button base
			double shrinkX = width * 1/10;
			double shrinkY = height * 1/10;

			Rectangle baseBck = new Rectangle(width - 2*shrinkX, height - 2*shrinkY, Color.LIGHTGRAY);
			baseBck.setLayoutX(shrinkX);
			baseBck.setLayoutY(shrinkY);
			baseBck.setArcWidth(grid.getSizeX()/2);
			baseBck.setArcHeight(grid.getSizeY()/2);

			double boltRadius = grid.getSizeX() / 5.0;
			double boltOffset = grid.getSizeX() / 2.5;

			Circle topLeftBolt = new Circle(boltRadius, Color.BLACK);
			topLeftBolt.setCenterX(boltOffset);
			topLeftBolt.setCenterY(boltOffset);

			Circle topRightBolt = new Circle(boltRadius, Color.BLACK);
			topRightBolt.setCenterX(width - boltOffset);
			topRightBolt.setCenterY(boltOffset);

			Circle bottomLeftBolt = new Circle(boltRadius, Color.BLACK);
			bottomLeftBolt.setCenterX(boltOffset);
			bottomLeftBolt.setCenterY(height - boltOffset);

			Circle bottomRightBolt = new Circle(boltRadius, Color.BLACK);
			bottomRightBolt.setCenterX(width - boltOffset);
			bottomRightBolt.setCenterY(height - boltOffset);

			buttonBase = new Group(baseBck, topLeftBolt, topRightBolt, bottomLeftBolt, bottomRightBolt);

			button.addEventFilter(MouseEvent.MOUSE_ENTERED, onMouseEnter);
            button.addEventFilter(MouseEvent.MOUSE_EXITED, onMouseExit);
            button.addEventFilter(MouseEvent.MOUSE_PRESSED, onMousePress);
			button.addEventFilter(MouseEvent.MOUSE_RELEASED, onMouseRelease);
			button.addEventFilter(MouseEvent.MOUSE_DRAGGED, Event::consume);

			this.getChildren().addAll(buttonBase, button);
		}

		//simulacia stlacenia tlacidla
		private void fireEvent(boolean pushed){
			//ak je riadok napojeny na zem
			if(this.row.getPotential().getValue() == Potential.Value.LOW){
				getBoard().addEvent(new BoardChangeEvent(this.column, pushed? Potential.Value.LOW : Potential.Value.HIGH));
			}
		}

	}
}