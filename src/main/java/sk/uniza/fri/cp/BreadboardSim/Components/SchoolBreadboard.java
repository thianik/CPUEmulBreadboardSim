package sk.uniza.fri.cp.BreadboardSim.Components;


import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import sk.uniza.fri.cp.BreadboardSim.Board;
import sk.uniza.fri.cp.BreadboardSim.GridSystem;
import sk.uniza.fri.cp.Bus.Bus;

/**
 * @author Moris
 * @version 1.0
 * @created 17-mar-2017 16:16:35
 */
public class SchoolBreadboard extends Component {

	private static final Color BACKGROUND_COLOR = Color.rgb(51,100,68);

	private static SchoolBreadboard instance;

	private Rectangle background;

	private BusInterface busInterface;
	private Breadboard breadboard;
	private HexSegmentsPanel hexSegmentsPanel;
	private NumKeys numKeys;

	private SchoolBreadboard(Board board){
		super(board, 1);

		GridSystem grid =  board.getGrid();

        this.gridWidth = grid.getSizeX() * 70;
        this.gridHeight = grid.getSizeY() * 46;
		background = new Rectangle(this.gridWidth, this.gridHeight, BACKGROUND_COLOR);
		background.setArcWidth(grid.getSizeX());
		background.setArcHeight(grid.getSizeY());

		this.busInterface = new BusInterface(board, Bus.getBus());
		this.busInterface.makeImmovable();
        this.busInterface.setSelectable(false);
		this.busInterface.setLayoutX(grid.getSizeX() * 2);

		this.breadboard = new Breadboard(board);
		this.breadboard.makeImmovable();
        this.breadboard.setSelectable(false);
		this.breadboard.setLayoutX(grid.getSizeX() * 2);
		this.breadboard.setLayoutY(busInterface.getGridHeight());

		this.hexSegmentsPanel = new HexSegmentsPanel(board);
		this.hexSegmentsPanel.makeImmovable();
        this.hexSegmentsPanel.setSelectable(false);
		this.hexSegmentsPanel.setLayoutX(grid.getSizeX() * 2);
		this.hexSegmentsPanel.setLayoutY(busInterface.getGridHeight() + breadboard.getGridHeight() + grid.getSizeY() );

		this.numKeys = new NumKeys(board);
		this.numKeys.makeImmovable();
        this.numKeys.setSelectable(false);
		this.numKeys.setLayoutX(grid.getSizeX() * 2 + breadboard.getGridWidth() - numKeys.getGridWidth());
		this.numKeys.setLayoutY(busInterface.getGridHeight() + breadboard.getGridHeight());

		addAllPowerSockets(busInterface.getPowerSockets());
		addAllPowerSockets(breadboard.getPowerSockets());
		addAllPowerSockets(hexSegmentsPanel.getPowerSockets());
		addAllPowerSockets(numKeys.getPowerSockets());

		this.socketsForDevices.addAll(breadboard.getSocketsForDevices());

		this.getChildren().addAll(background, busInterface, breadboard, numKeys, hexSegmentsPanel);
	}

	public static SchoolBreadboard getSchoolBreadboard(Board board){
		if(instance == null)
			instance = new SchoolBreadboard(board);

		return instance;
	}

	@Override
	public void moveTo(int gridPosX, int gridPosY) {
		super.moveTo(gridPosX, gridPosY);

		this.busInterface.setGridPos(getGridPosX(), getGridPosY());
		this.breadboard.setGridPos(getGridPosX(), getGridPosY());
		this.hexSegmentsPanel.setGridPos(getGridPosX(), getGridPosY());
		this.numKeys.setGridPos(getGridPosX(), getGridPosY());

		this.busInterface.updateConnectedDevicesPosition();
		this.breadboard.updateConnectedDevicesPosition();
		this.hexSegmentsPanel.updateConnectedDevicesPosition();
		this.numKeys.updateConnectedDevicesPosition();
	}

	@Override
	public void delete() {}
}