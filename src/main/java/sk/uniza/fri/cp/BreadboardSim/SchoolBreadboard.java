package sk.uniza.fri.cp.BreadboardSim;


import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import sk.uniza.fri.cp.BreadboardSim.Board.Board;
import sk.uniza.fri.cp.BreadboardSim.Board.GridSystem;
import sk.uniza.fri.cp.BreadboardSim.Components.*;
import sk.uniza.fri.cp.Bus.Bus;

import java.util.LinkedList;
import java.util.List;

/**
 * @author Moris
 * @version 1.0
 * @created 17-mar-2017 16:16:35
 */
public class SchoolBreadboard extends Item {

	private static final Color BACKGROUND_COLOR = Color.rgb(51,100,68);

	private static SchoolBreadboard instance;

    private Component busInterface;
    private Component breadboard;
    private Component hexSegmentsPanel;
    private Component numKeys;

	private SchoolBreadboard(Board board){
        super(board);

		GridSystem grid =  board.getGrid();

        int gridWidth = grid.getSizeX() * 70;
        int gridHeight = grid.getSizeY() * 46;
        Rectangle background = new Rectangle(gridWidth, gridHeight, BACKGROUND_COLOR);
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

		this.getChildren().addAll(background, busInterface, breadboard, numKeys, hexSegmentsPanel);
	}

	public static SchoolBreadboard getSchoolBreadboard(Board board){
		if(instance == null)
			instance = new SchoolBreadboard(board);

		return instance;
	}

    public List<? extends Component> getComponents() {
        List<Component> list = new LinkedList<>();
        list.add(this.busInterface);
        list.add(this.breadboard);
        list.addAll(((HexSegmentsPanel) this.hexSegmentsPanel).getComponents());
        list.add(this.numKeys);
        return list;
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