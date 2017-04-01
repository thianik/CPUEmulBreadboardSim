package sk.uniza.fri.cp.BreadboardSim;


import javafx.event.EventHandler;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;

import java.util.LinkedList;

/**
 * @author Moris
 * @version 1.0
 * @created 17-mar-2017 16:16:35
 */
public interface Selectable {

	double[] STROKE_DASH_ARRAY = {10d, 5d};

	Board getBoard();
	Pane getDescription();
	void select();
	void deselect();
	void delete();
}