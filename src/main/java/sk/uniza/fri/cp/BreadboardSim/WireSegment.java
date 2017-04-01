package sk.uniza.fri.cp.BreadboardSim;


import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Line;
import javafx.scene.transform.Transform;

/**
 * @author Moris
 * @version 1.0
 * @created 17-mar-2017 16:16:36
 */
public class WireSegment extends Line {

	private Wire wire;
	private Joint startJoint;
	private Joint endJoint;

	//private Line line;

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

		this.setStrokeWidth(start.getBoard().getGrid().getSizeMin() * 2/5);
		this.setFill(Wire.getDefaultColor());
		this.setStroke(Wire.getDefaultColor());
		this.setOpacity(0.8);
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

	}

}