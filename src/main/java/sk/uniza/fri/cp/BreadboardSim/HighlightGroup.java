package sk.uniza.fri.cp.BreadboardSim;


import javafx.event.EventHandler;
import javafx.scene.Group;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import sk.uniza.fri.cp.BreadboardSim.Board.Board;

/**
 * @author Moris
 * @version 1.0
 * @created 17-mar-2017 16:16:34
 */
public abstract class HighlightGroup extends Group implements Selectable {


	private boolean isSelectable;

	public HighlightGroup(){
	    this.isSelectable = true;

		EventHandler<MouseEvent> onMouseClickEventHandler = (event) -> {
            if (!event.isPrimaryButtonDown()) return;
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

        this.addEventHandler(MouseEvent.MOUSE_PRESSED, onMouseClickEventHandler);

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


	public Pane getDescription(){
		return new Pane(new Label("No description \n" + this.getClass().getSimpleName()));
	}

    @Override
	public void select(){

	}

	@Override
	public void deselect(){

	}

	@Override
	public void delete(){

	}

    @Override
    public boolean isSelectable() {
        return this.isSelectable;
    }

    @Override
    public void setSelectable(boolean newValue) {
        this.isSelectable = newValue;
    }
}