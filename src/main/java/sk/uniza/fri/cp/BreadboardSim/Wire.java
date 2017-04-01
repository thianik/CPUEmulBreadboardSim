package sk.uniza.fri.cp.BreadboardSim;


import javafx.event.Event;
import javafx.scene.Group;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * @author Moris
 * @version 1.0
 * @created 17-mar-2017 16:16:36
 */
public class Wire extends HighlightGroup {

	private static Color defaultColor = Color.BLACK;

	private Paint color;
	private Potential potential;

	private WireEnd[] ends;	//konce kablika
	private List<Joint> joints;
	private List<WireSegment> segments;

	//grupy pre vrstvovanie -> segmenty dole, jointy hore
	private Group jointsGroup;
	private Group segmentsGroup;

	private Joint createdJoint;

	/**
	 * 
	 * @param startSocket
	 */
	public Wire(Socket startSocket){
		this.joints = new LinkedList<>();
		this.segments = new LinkedList<>();
		this.segmentsGroup = new Group();
		this.jointsGroup = new Group();

		//konce kablika
		this.ends = new WireEnd[2];

		//zaciatok kablika, pripojeny k soketu
		this.ends[0] = new WireEnd(startSocket.getComponent().getBoard(), this);
		this.ends[0].connectSocket(startSocket);

		//koniec kablika vo vzduchu
		this.ends[1] = new WireEnd(startSocket.getComponent().getBoard(), this);
		this.ends[1].moveTo(this.ends[0].getLayoutX(), this.ends[0].getLayoutY());

		//prvotny segment
		WireSegment segment = new WireSegment(this, this.ends[0], this.ends[1]);

		this.segments.add(segment);

		this.segmentsGroup.getChildren().addAll(segment);
		this.jointsGroup.getChildren().addAll(this.ends[0], this.ends[1]);
		this.getChildren().addAll(segmentsGroup, jointsGroup);

		//pri zacati tahania vytvori novy
		this.addEventHandler(MouseEvent.DRAG_DETECTED, event -> {
			if(event.getTarget() instanceof WireSegment){
				WireSegment segmentToSplit = ((WireSegment) event.getTarget());
				createdJoint = splitSegment(segmentToSplit);
			}
		});

		this.addEventHandler(MouseEvent.MOUSE_DRAGGED, event -> {
			if(createdJoint != null){
				Event.fireEvent(createdJoint, new MouseEvent(MouseEvent.MOUSE_DRAGGED, event.getSceneX(), event.getSceneY(),
						event.getScreenX(), event.getScreenY(), MouseButton.PRIMARY, 1, true,
						true, true, true, true, true,
						true, true, true, true, null));
			}
		});

		this.addEventHandler(MouseEvent.MOUSE_RELEASED, event -> createdJoint = null);
	}

	/**
	 * 
	 * @param defColor
	 */
	public static void setDefaultColor(Color defColor){
		defaultColor = defColor;
	}

	public static Color getDefaultColor(){
		return defaultColor;
	}

	/**
	 * Vráti voľný koniec káblika pri jeho prvotnom vytváraní.
	 * Pri opätovnom volaní vráti ten koniec káblika, ktorý bol vytvorený ako druhý.
	 *
	 * @return Voľný koniec káblika pri vytváraní.
	 */
	public WireEnd catchFreeEnd(){
		return this.ends[1];
	}


	/**
	 * Odstránenie spojovača na kábliku. Je možné odstraňovať iba vnútorné jointy, nie konce.
	 * @param joint
	 */
	public void removeJoint(Joint joint){
		if(joint instanceof WireEnd) return;

		//segmenty spojene jointom
		WireSegment firstSegment = joint.getPrimaryWireSegment();
		WireSegment secondSegment = joint.getSecondaryWireSegment();

		//jointy z odlahlych koncov spojenych segmentov
		Joint firstJoint = firstSegment.getOtherJoint(joint);
		Joint secondJoint = secondSegment.getOtherJoint(joint);

		//odstranenie segmentov z joinov
		firstJoint.removeWireSegment(firstSegment);
		secondJoint.removeWireSegment(secondSegment);

		WireSegment newWireSegment = new WireSegment(this, firstJoint, secondJoint);
		this.segments.add(newWireSegment);
		this.segmentsGroup.getChildren().add(newWireSegment);

		this.segments.remove(firstSegment);
		this.segments.remove(secondSegment);
		this.joints.remove(joint);

		this.segmentsGroup.getChildren().removeAll(firstSegment, secondSegment);
		this.jointsGroup.getChildren().remove(joint);
	}

	public Joint splitSegment(WireSegment segment){
		Joint newJoint = new Joint(getBoard(), this);

		//konce rozdelujuceho segmentu
		Joint firstJoint = segment.getStartJoint();
		Joint secondJoint = segment.getEndJoint();

		//presun noveho jointu na miesto prveho aby neskakal mimo plochu
		newJoint.moveTo(firstJoint.getLayoutX(), firstJoint.getLayoutY());

		//odpojenie segmentu od koncov
		firstJoint.removeWireSegment(segment);
		secondJoint.removeWireSegment(segment);

		//vytvorenie novych dvoch segmentov
		WireSegment firstSegment = new WireSegment(this, firstJoint, newJoint);
		WireSegment secondSegment = new WireSegment(this, newJoint, secondJoint);

		this.segments.add(firstSegment);
		this.segments.add(secondSegment);
		this.joints.add(newJoint);
		this.segmentsGroup.getChildren().addAll(firstSegment, secondSegment);
		this.jointsGroup.getChildren().add(newJoint);

		this.segments.remove(segment);
		this.segmentsGroup.getChildren().remove(segment);

		return newJoint;
	}

	//ak su oba konce pripojene, oba vygeneruju moveBy
	//preto rychly fix v podobe allowToMove -> prvy pohne, druhy nie
	private boolean allowToMove = true;

	public void moveBy(double deltaX, double deltaY){
		//ak su pripojene oba konce
		if(this.ends[0].getSocket() != null && this.ends[1].getSocket() != null)
			//ak su oba konce pripojene k rovnakemu komponentu
			if(this.ends[0].getSocket().getComponent().equals(this.ends[1].getSocket().getComponent())) {
				//ak je povolene posuvanie (prvy z WireEnd-ov)
				if (allowToMove)
					//posun vsetky jointy o deltu
					this.joints.forEach(joint -> joint.moveBy(deltaX, deltaY));
				allowToMove = !allowToMove;
			}
	}

	public Board getBoard(){
		return this.ends[0].getBoard();
	}

	public void updatePotential(){
		if(this.potential != null){
			this.potential.delete();
			this.potential = null;
		}

		Socket toUpdate;
		if(this.ends[0] != null && this.ends[1] != null){
			Socket start = this.ends[0].getSocket();
			Socket end = this.ends[1].getSocket();
			if(start != null && end != null) {
				//ak su oba konce pripojene, updateujeme jednu vetvu potencialov
				this.potential = new Potential(start, end);
				toUpdate = start;
			} else {
				//ak je iba jeden koniec pripojeny, updateujeme jeho vetvu potencialov (druha sa updatuje pri odpojeni WireEnd)
				toUpdate = start!=null?start:end;
			}

			if(getBoard().simRunningProperty().getValue())
				getBoard().addEvent(new BoardEvent(toUpdate));
		}
	}

	@Override
	public void delete() {
		super.delete();

		this.ends[0].getBoard().removeItem(this);
	}
}