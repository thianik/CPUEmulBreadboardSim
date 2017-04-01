package sk.uniza.fri.cp.BreadboardSim.Components;


import javafx.scene.Group;
import javafx.scene.text.Text;
import sk.uniza.fri.cp.BreadboardSim.Board;
import sk.uniza.fri.cp.BreadboardSim.GridSystem;
import sk.uniza.fri.cp.BreadboardSim.Potential;
import sk.uniza.fri.cp.BreadboardSim.SocketsFactory;

/**
 * @author Moris
 * @version 1.0
 * @created 17-mar-2017 16:16:34
 */
public class HexSegmentsPanel extends Component {

	private static int id = 1;

	public HexSegment[] hexSegments;

	public HexSegmentsPanel(Board board){
		super(board, id++);

        GridSystem grid = board.getGrid();

		this.hexSegments = new HexSegment[4];

        for (int i = 0; i < 4; i++) {
            this.hexSegments[i] = new HexSegment(board);
            this.hexSegments[i].makeImmovable();
            this.hexSegments[i].setLayoutX(grid.getSizeX() * 5 * i);

            Group segmentGndGroup = this.hexSegments[i].getCommonGndSocketGroup();
            segmentGndGroup.setLayoutX(grid.getSizeX() * 5 * (4-i) + grid.getSizeX() * 2);
            segmentGndGroup.setLayoutY(segmentGndGroup.getLayoutY() -  grid.getSizeY() * (3-i));
        }

        this.gridWidth = hexSegments[0].getGridWidth() * 4;
        this.gridHeight = hexSegments[0].getGridHeight();

        //VCC
        Group vccSockets = SocketsFactory.getHorizontalPower(this, 1, 2, Potential.Value.HIGH , getPowerSockets());
        vccSockets.setLayoutX(grid.getSizeX() * 5 * 4 + grid.getSizeX());
        vccSockets.setLayoutY(grid.getSizeY());

        Text vccText = Board.getLabelText("+5V", grid.getSizeMin());
        vccText.setLayoutX(-vccText.getBoundsInParent().getWidth()/4.0);
        vccText.setLayoutY(grid.getSizeY() + vccText.getBoundsInParent().getWidth()/4.0);
        vccSockets.getChildren().add(vccText);

        //GND
        Group gndSockets = SocketsFactory.getVerticalPower(this, 3, 4,  Potential.Value.LOW, getPowerSockets());
        gndSockets.setLayoutX(grid.getSizeX() * 5 * 4 + grid.getSizeX());
        gndSockets.setLayoutY(grid.getSizeY() * 5);

        Text gndText = Board.getLabelText("GND", grid.getSizeMin());
        gndText.setLayoutX(-gndText.getBoundsInParent().getWidth()/2.0);
        gndText.setLayoutY(grid.getSizeY() * 5);
        gndSockets.getChildren().add(gndText);

        this.getChildren().addAll(this.hexSegments);
        this.getChildren().addAll(gndSockets, vccSockets);
	}


}