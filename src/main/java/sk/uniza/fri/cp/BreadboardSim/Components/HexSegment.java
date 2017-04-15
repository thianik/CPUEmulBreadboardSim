package sk.uniza.fri.cp.BreadboardSim.Components;


import javafx.scene.Group;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;
import javafx.scene.text.Text;
import sk.uniza.fri.cp.BreadboardSim.Board.Board;
import sk.uniza.fri.cp.BreadboardSim.Devices.LED;
import sk.uniza.fri.cp.BreadboardSim.Board.GridSystem;
import sk.uniza.fri.cp.BreadboardSim.Socket.Potential;
import sk.uniza.fri.cp.BreadboardSim.Socket.Socket;


/**
 * @author Moris
 * @version 1.0
 * @created 17-mar-2017 16:16:34
 */
public class HexSegment extends Component{

	private static final Color SEGMENT_ON_COLOR = Color.RED;
	private static final Color SEGMENT_OFF_COLOR = Color.LIGHTGRAY;

    private static int segmentID = 1;

	private HexSegment instance;
	private OneSegment[] segments;
	private Socket[] inputSockets;
	private Socket commonGndSocket;
	private Group commonGndSocketGroup;

	private Rectangle background;

	private enum SegmentType {
		HORIZONTAL, VERTICAL, DOT;
	}

    public HexSegment() {
    }

	public HexSegment(Board board){
        super(board);
        this.instance = this;

		GridSystem grid = board.getGrid();

        this.gridWidth = grid.getSizeX() * 8;
        this.gridHeight = grid.getSizeY() * 16;

        this.background = new Rectangle(this.gridWidth, this.gridHeight, Color.rgb(51, 100, 68));

		this.inputSockets = new Socket[8];
		Group inputSocketsGroup = generateInputSockets();
		inputSocketsGroup.setLayoutX(grid.getSizeX());
        inputSocketsGroup.setLayoutY(grid.getSizeY());

        //vyvod na uzemnenie segmentov
        commonGndSocket = new Socket(this);
        Text gndSocketText = Board.getLabelText("A" + segmentID++, grid.getSizeMin());
        gndSocketText.setLayoutX(grid.getSizeX());
        gndSocketText.setLayoutY(0.25 * gndSocketText.getBoundsInParent().getHeight());

        this.commonGndSocketGroup = new Group(commonGndSocket, gndSocketText);
        this.commonGndSocketGroup.setLayoutX(grid.getSizeX() * 5);
        this.commonGndSocketGroup.setLayoutY(grid.getSizeY() * 8);

		this.segments = new OneSegment[8];
		Group segmentsGroup = generateSegments();
		segmentsGroup.setLayoutX(- segments[0].getThicknessCoef());
		segmentsGroup.setLayoutY(grid.getSizeY() * 9);

		//pripojenie segmentov ku katode, anodu maju spolocnu
		for (int i = 0; i < 8; i++) {
			this.segments[i].connect(this.inputSockets[i]);
		}

        this.getChildren().addAll(background, inputSocketsGroup, segmentsGroup, commonGndSocketGroup);

        //registracia vsetkych soketov
        this.addAllSockets(inputSockets);
        this.addSocket(commonGndSocket);
    }

    public void hideBackground(boolean hide) {
        if (hide) this.background.setOpacity(0);
        else this.background.setOpacity(1);
    }

	public Group getCommonGndSocketGroup(){
	    return commonGndSocketGroup;
    }

	private Group generateInputSockets(){
		GridSystem grid = getBoard().getGrid();

		Group sockets = new Group();

		for (int i = 0; i < 8; i++) {
            Socket socket = new Socket(this);
            socket.setLayoutY(grid.getSizeY() * i);

			Text text = Board.getLabelText(String.valueOf((char) (i+97)), grid.getSizeMin());
			text.setLayoutX(grid.getSizeX());
			text.setLayoutY(grid.getSizeY() * i + 0.25 * text.getBoundsInParent().getHeight());
			sockets.getChildren().add(text);

			inputSockets[i] = socket;
            //super.sockets.add(socket);
        }

		sockets.getChildren().addAll(inputSockets);
		return sockets;
	}

	private Group generateSegments(){
		GridSystem grid = getBoard().getGrid();

		Group segmentsGroup = new Group();

		this.segments[0] = new OneSegment(SegmentType.HORIZONTAL);
		this.segments[1] = new OneSegment(SegmentType.VERTICAL);
		this.segments[2] = new OneSegment(SegmentType.VERTICAL);
		this.segments[3] = new OneSegment(SegmentType.HORIZONTAL);
		this.segments[4] = new OneSegment(SegmentType.VERTICAL);
		this.segments[5] = new OneSegment(SegmentType.VERTICAL);
		this.segments[6] = new OneSegment(SegmentType.HORIZONTAL);
		this.segments[7] = new OneSegment(SegmentType.DOT);

		int segmentLength = this.segments[0].getGridLength();
		double thicknessCoef = this.segments[0].getThicknessCoef();

		this.segments[0].setLayoutX(grid.getSizeX());
		this.segments[0].setLayoutY(grid.getSizeY());
		this.segments[0].invert();
		this.segments[1].setLayoutX(grid.getSizeX() * (1 + segmentLength));
		this.segments[1].setLayoutY(grid.getSizeY());
		this.segments[2].setLayoutX(grid.getSizeX() * (1 + segmentLength));
		this.segments[2].setLayoutY(grid.getSizeY() * (2 + segmentLength) - grid.getSizeY() * thicknessCoef);
		this.segments[3].setLayoutX(grid.getSizeX());
		this.segments[3].setLayoutY(grid.getSizeY() * (2 + 2 * segmentLength) - grid.getSizeY() * thicknessCoef);
		this.segments[4].setLayoutX(grid.getSizeX());
		this.segments[4].setLayoutY(grid.getSizeY() * (2 + segmentLength) - grid.getSizeY() * thicknessCoef);
		this.segments[4].invert();
		this.segments[5].setLayoutX(grid.getSizeX());
		this.segments[5].setLayoutY(grid.getSizeY());
		this.segments[5].invert();
		this.segments[6].setLayoutX(grid.getSizeX());
		this.segments[6].setLayoutY(grid.getSizeY() * (1 + segmentLength));
		this.segments[7].setLayoutX(grid.getSizeX() * (2 + segmentLength));
		this.segments[7].setLayoutY(grid.getSizeY() * (2 + 2 * segmentLength));
		this.segments[7].invert();

		for (int i = 0; i < 8; i++) {
			segmentsGroup.getChildren().add(this.segments[i]);
		}

		return segmentsGroup;
	}

	private class OneSegment extends Region{

        private static final int GRID_LENGTH = 2;

	    private final SegmentType type;
        private LED LED;

		private double thicknessCoef;
		private Shape segmentShape;
		private boolean inverted;

		private Socket innerGndSocket;

		OneSegment(SegmentType type){
			this.type = type;
			this.thicknessCoef = 0.5;

			//vytvorenie fiktivneho vnutorneho soketu pre pripojenie gnd ledky a napojenie na potencial zo spolocneho gnd soketku
            this.innerGndSocket = new Socket(instance);
            new Potential(this.innerGndSocket, commonGndSocket);

			GridSystem grid = getBoard().getGrid();

			switch (type){
				case HORIZONTAL: segmentShape = new Rectangle(grid.getSizeX() * GRID_LENGTH, grid.getSizeY() * this.thicknessCoef, SEGMENT_OFF_COLOR);
					break;
				case VERTICAL: segmentShape = new Rectangle(grid.getSizeX() * this.thicknessCoef, grid.getSizeY() * GRID_LENGTH, SEGMENT_OFF_COLOR);
					break;
				case DOT: segmentShape = new Rectangle(grid.getSizeX() * this.thicknessCoef, grid.getSizeY() * this.thicknessCoef, SEGMENT_OFF_COLOR);
			}

            this.LED = new LED(getBoard(), segmentShape, SEGMENT_ON_COLOR);
            this.LED.makeImmovable();
            this.LED.setInverseAnodeLogic(true);

            this.getChildren().addAll(LED);
        }

		void connect(Socket input){
            input.connect(this.LED.getCathode());
            this.innerGndSocket.connect(this.LED.getAnode());
        }

		void invert(){
			GridSystem grid = getBoard().getGrid();

			switch (this.type){
				case HORIZONTAL: segmentShape.setLayoutY( inverted?0:-grid.getSizeY()*thicknessCoef);
					break;
				case VERTICAL: segmentShape.setLayoutX( inverted?0:-grid.getSizeX()*thicknessCoef);
					break;
				case DOT:
					segmentShape.setLayoutY( inverted?0:-grid.getSizeY()*thicknessCoef);
			}

			inverted = !inverted;
		}

		int getGridLength(){ return GRID_LENGTH; }

		double getThicknessCoef(){ return thicknessCoef; }


	}
}