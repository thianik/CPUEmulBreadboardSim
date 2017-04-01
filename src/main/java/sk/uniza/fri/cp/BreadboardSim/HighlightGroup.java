package sk.uniza.fri.cp.BreadboardSim;


import javafx.event.EventHandler;
import javafx.scene.Group;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;

import java.util.List;

/**
 * @author Moris
 * @version 1.0
 * @created 17-mar-2017 16:16:34
 */
public abstract class HighlightGroup extends Group implements Selectable {

	private List<Warning> warnings;
	private Warning sendedWarning;
	public Warning m_Warning;

	public HighlightGroup(){
		EventHandler<MouseEvent> onMouseClickEventHandler = (event) -> {
			Board board = getBoard();

			//ak je objekt na ploche
			if(board != null) {
				//ak je stlaceny shift -> pridanie do vyberu
				if (event.isShiftDown()) {
					board.addSelect(this);
				} else {
					//ak nie je shift -> vyber iba tento jeden item
					board.clearSelect();
					board.addSelect(this);
				}

			}
		};

		this.addEventFilter(MouseEvent.MOUSE_PRESSED, onMouseClickEventHandler);

	}

	/**
	 * 
	 * @param turnOn
	 */
	public void highlightSelect(boolean turnOn){

	}

	/**
	 * 
	 * @param sender
	 * @param description
	 */
	public void highlightAddWarning(HighlightGroup sender, String description){

	}

	/**
	 * 
	 * @param warning
	 */
	public void setMyWarning(Warning warning){

	}

	public Pane getDescription(){
		return new Pane(new Label("No description \n" + this.getClass().getSimpleName()));
	}

	public void select(){

	}

	public void deselect(){

	}

	public void delete(){

	}

}