package sk.uniza.fri.cp.BreadboardSim.Components;


import javafx.scene.Group;
import javafx.scene.control.Label;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import sk.uniza.fri.cp.BreadboardSim.Board;
import sk.uniza.fri.cp.BreadboardSim.GridSystem;
import sk.uniza.fri.cp.BreadboardSim.SocketsFactory;

/**
 * @author Moris
 * @version 1.0
 * @created 17-mar-2017 16:16:34
 */
public class Breadboard extends Component {

	private static int id = 1;

	//grafika
	private Rectangle background;
	private static final Color BACKGROUND_COLOR = Color.WHITESMOKE;

	public Breadboard(Board board){
		super(board, id++);

		//grafika
		GridSystem grid = getBoard().getGrid();
		this.gridWidth = grid.getSizeX() * 66;
		this.gridHeight = grid.getSizeY() * 23;

		background = new Rectangle(this.gridWidth, this.gridHeight, BACKGROUND_COLOR);

		Group topPowerLines = generatePowerLines(1);
		topPowerLines.setLayoutX(grid.getSizeX() * 4);
		topPowerLines.setLayoutY(grid.getSizeY() * 2);

		Group topSockets = generate5VerticalSocketsGroups(101);
		topSockets.setLayoutX(grid.getSizeX() * 2);
		topSockets.setLayoutY(grid.getSizeX() * 6);

		Group bottomSockets = generate5VerticalSocketsGroups(416);
		bottomSockets.setLayoutX(grid.getSizeX() * 2);
		bottomSockets.setLayoutY(grid.getSizeX() * 13);

		Group bottomPowerLines = generatePowerLines(731);
		bottomPowerLines.setLayoutX(grid.getSizeX() * 4);
		bottomPowerLines.setLayoutY(grid.getSizeY() * 20);

		//popisky
		Group charLabelsLeft = generateAtoJLabels();
		charLabelsLeft.setLayoutX(grid.getSizeX());
		charLabelsLeft.setLayoutY(grid.getSizeY() * 6);

		Group charLabelsRight = generateAtoJLabels();
		charLabelsRight.setLayoutX(grid.getSizeX() * 65);
		charLabelsRight.setLayoutY(grid.getSizeY() * 6);

		Group numberLabelsTop = generateNumberLabels();
		numberLabelsTop.setLayoutX(grid.getSizeX() * 2);
		numberLabelsTop.setLayoutY(grid.getSizeY() * 4.5);

		Group numberLabelsBottom = generateNumberLabels();
		numberLabelsBottom.setLayoutX(grid.getSizeX() * 2);
		numberLabelsBottom.setLayoutY(grid.getSizeY() * 17.5);

		this.getChildren().addAll(background, topPowerLines, topSockets, bottomSockets, bottomPowerLines, charLabelsLeft, charLabelsRight, numberLabelsTop, numberLabelsBottom);
	}

	private Group generatePowerLines(int startId){
		GridSystem grid = getBoard().getGrid();

		//sokety
		Group vccSockets = SocketsFactory.getHorizontal(this, startId, 50, 5, 1, super.socketsForDevices);

		Group gndSockets = SocketsFactory.getHorizontal(this, startId + 50, 50, 5, 1, super.socketsForDevices);
		gndSockets.setLayoutY(grid.getSizeY());

		//ciarove oznacenie
		Line redLine = new Line(-grid.getSizeX(), -grid.getSizeY(), grid.getSizeX() * 59, -grid.getSizeY());
		redLine.setStrokeWidth(2);
		redLine.setStroke(Color.RED);

		Line blueLine = new Line(-grid.getSizeX(), grid.getSizeY() * 2, grid.getSizeX() * 59, grid.getSizeY() * 2);
		blueLine.setStrokeWidth(2);
		blueLine.setStroke(Color.BLUE);

		return new Group(vccSockets, gndSockets, redLine, blueLine);
	}

	private Group generate5VerticalSocketsGroups(int startId){
		GridSystem grid = getBoard().getGrid();

		Group sockets = new Group();

		for (int i = 0; i < 63; i++) {
			Group vertiacalLine = SocketsFactory.getVertical(this, startId + i * 5, 5, super.socketsForDevices);
			vertiacalLine.setLayoutX(i * grid.getSizeX());
			sockets.getChildren().add(vertiacalLine);
		}
		
		return sockets;
	}

	private Group generateAtoJLabels(){
		GridSystem grid = getBoard().getGrid();

		Group AtoJLabels = new Group();
		char ch = 'A';
		int pos = 0;
		do {
			Label label = new Label(String.valueOf(ch));
			label.setFont(new Font("Lucida Console", grid.getSizeX()));
			label.setId("breadboardLabel");
			label.setPrefWidth(grid.getSizeX());
			label.setLayoutX(pos * grid.getSizeX() - grid.getSizeX() / 2.0);
			AtoJLabels.getChildren().add(label);

			if (pos != 4)
				pos++;
			else
				pos += 3;

			ch++;
		} while	(ch <= 'J');
		AtoJLabels.setRotate(-90);
		AtoJLabels.setLayoutX(grid.getSizeX() * -5.5 );
		AtoJLabels.setLayoutY(grid.getSizeY() * 5);

		return new Group(AtoJLabels);
	}

	private Group generateNumberLabels(){
		GridSystem grid = getBoard().getGrid();

		Group numberLabels = new Group();
		Label number = new Label("1");
		number.setFont(new Font("Lucida Console", grid.getSizeX()));
		number.setId("breadboardLabel");
		number.setPrefWidth(grid.getSizeX());
		number.setLayoutX(- grid.getSizeX() / 2.0);
		numberLabels.getChildren().add(number);

		for (int i = 5; i <= 60; i+=5) {
			number = new Label(String.valueOf(i));
			number.setFont(new Font("Lucida Console", grid.getSizeX()));
			number.setId("breadboardLabel");
			number.setPrefWidth(grid.getSizeX());
			number.setLayoutX((i-1) * grid.getSizeX() - grid.getSizeX() / 2.0);
			numberLabels.getChildren().add(number);
		}

		return numberLabels;
	}
}