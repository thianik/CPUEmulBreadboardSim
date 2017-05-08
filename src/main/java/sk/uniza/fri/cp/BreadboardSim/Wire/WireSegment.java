package sk.uniza.fri.cp.BreadboardSim.Wire;


import javafx.scene.paint.Paint;
import javafx.scene.shape.Line;

/**
 * @author Moris
 * @version 1.0
 * @created 17-mar-2017 16:16:36
 */
public class WireSegment extends Line {

	private Wire wire;
	private Joint startJoint;
	private Joint endJoint;

	public WireSegment(Wire wire, Joint start, Joint end){
		this.wire = wire;
		this.startJoint = start;
		this.endJoint = end;

		this.startJoint.connectWireSegment(this);
		this.endJoint.connectWireSegment(this);

		//nabindovanie k stredu jointov na koncoch kablika
        this.startXProperty().bind(this.startJoint.layoutXProperty());
        this.startYProperty().bind(this.startJoint.layoutYProperty());
        this.endXProperty().bind(this.endJoint.layoutXProperty());
        this.endYProperty().bind(this.endJoint.layoutYProperty());

        this.setStrokeWidth(start.getBoard().getGrid().getSizeMin() * 2.0 / 6.0);
        this.setFill(wire.getColor());
        this.setStroke(wire.getColor());
        this.setOpacity(1);
    }

	public Wire getWire(){return this.wire;}

	public Joint getEndJoint(){
		return endJoint;
	}

	public Joint getOtherJoint(Joint joint){
		return startJoint == joint ? endJoint : startJoint;
	}

	/**
	 * 
	 * @param newVal
	 */
	public void setEndJoint(Joint newVal){
		endJoint = newVal;
	}

	public Joint getStartJoint(){
		return startJoint;
	}

	/**
	 * 
	 * @param newVal
	 */
	public void setStartJoint(Joint newVal){
		startJoint = newVal;
	}

	/**
	 * 
	 * @param color
	 */
	public void setColor(Paint color){
        this.setFill(color);
        this.setStroke(color);
    }

}