package sk.uniza.fri.cp.BreadboardSim;


import javafx.event.EventHandler;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;

/**
 * interface???
 * @author Moris
 * @version 1.0
 * @created 17-mar-2017 16:16:35
 */
public abstract class Item extends Movable {


	public Item(Board board){
		super(board);


	}

	public Item(Board board, int gridPosX, int gridPosY){
		this(board);
		moveTo(gridPosX, gridPosY);
	}

	/**
	 * Bezparametrický konštruktor slúži na vytvorenie inštancie objektu pre ItemPicker.
	 */
	public Item(){
		this(null);
		makeImmovable();

		this.getChildren().add(getImage());
	}

	/**
	 * Obrázok, ktorý sa zobrazí v ItemPicker
	 * @return Panel s grafikou
	 */
	public Pane getImage(){
		return new Pane(new Rectangle(30,30, Color.GREEN));
	}

	/**
	 * Panel s popisom objektu. Vkladá sa do panela ScrollPanel.
	 * @return Panel s popisom
	 */
	public Pane getDescription(){
		Label text = new Label("Description ... " + this.getClass().getSimpleName());
		text.setFont(Font.font(10));
		return new Pane(text);
	}
}