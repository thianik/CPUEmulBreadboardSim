package sk.uniza.fri.cp.BreadboardSim.Wire;


import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import sk.uniza.fri.cp.BreadboardSim.Board.Board;
import sk.uniza.fri.cp.BreadboardSim.Board.GridSystem;
import sk.uniza.fri.cp.BreadboardSim.Movable;

/**
 * spajac casti kablikov
 * layoutX a layoutY su stredom
 *
 * Na indexe 0 wireSegments je vždy segment. Aj pri odstránení
 *
 * @author Moris
 * @version 1.0
 * @created 17-mar-2017 16:16:35
 */
public class Joint extends Movable {

    private static final Color DEFAULT_COLOR = Color.DARKGRAY;

	protected WireSegment[] wireSegments; //segmenty ktore spaja

	private Wire wire;
	private Group graphic;
	private Circle colorizer;
	private Rectangle boundingBox;
	private double radius;

	/**
	 * Spojovník častí káblikov.
	 *
	 *
	 */
	public Joint(Board board, Wire wire){
		super(board);
		this.wire = wire;

		this.wireSegments = new WireSegment[2];

		GridSystem grid = getBoard().getGrid();

		boundingBox = new Rectangle(grid.getSizeX(), grid.getSizeY());
		boundingBox.setOpacity(0);
		boundingBox.setLayoutX(-grid.getSizeX()/2.0);
		boundingBox.setLayoutY(-grid.getSizeY()/2.0);

		this.radius = grid.getSizeMin() / 3.5;
		this.graphic = generateJointGraphic(radius);

		this.getChildren().addAll(boundingBox, graphic);
	}

	/**
	 * Pripojenie jointu na segment. Ak nie je pripojeny ani jeden segment, hodi ho ako primarny,
	 * inak ako sekundarny segment.
	 */
	public void connectWireSegment(WireSegment segment){
		if(this.wireSegments[0] == null)
			this.wireSegments[0] = segment;
		else
			this.wireSegments[1] = segment;
	}

	public boolean removeWireSegment(WireSegment segment){
		if(wireSegments[0] == segment) {
			wireSegments[0] = wireSegments[1];
			wireSegments[1] = null;
		}
		else if(wireSegments[1] == segment)
			wireSegments[1] = null;
		else return false;

		return true;
	}

	public Wire getWire(){
		return this.wire;
	}

	public WireSegment getPrimaryWireSegment(){
		return this.wireSegments[0];
	}

	public WireSegment getSecondaryWireSegment(){
		return this.wireSegments[1];
	}

	public void setColor(Color color){
        this.colorizer.setFill(color);
        this.colorizer.setOpacity(1);
    }

    public void setDefaultColor(){
	    this.colorizer.setOpacity(0);
    }

	protected Group generateJointGraphic(double radius){

		Group graphics = new Group();

		Circle joint = new Circle(radius, radius, radius, DEFAULT_COLOR);
        joint.setTranslateX(-radius);
        joint.setLayoutY(-radius);


        this.colorizer = new Circle(0,0,radius, Color.RED);
        this.colorizer.setOpacity(0);

        graphics.getChildren().addAll(joint,colorizer);


		return graphics;
	}

}