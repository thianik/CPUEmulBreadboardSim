package sk.uniza.fri.cp.BreadboardSim;


import javafx.geometry.Bounds;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import sk.uniza.fri.cp.BreadboardSim.Board.Board;
import sk.uniza.fri.cp.BreadboardSim.Devices.Chips.Chip;

/**
 * interface???
 * @author Moris
 * @version 1.0
 * @created 17-mar-2017 16:16:35
 */
public abstract class Item extends Movable {

    AnchorPane cachedDescription;
    private Shape selectionShape; //prekrytie pri selecte

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
        Text itemName = new Text(this.getClass().getSimpleName());
        itemName.setLayoutX(5);
        itemName.setLayoutY(itemName.getBoundsInParent().getHeight());
        return new Pane(new Rectangle(itemName.getBoundsInParent().getWidth() + 10, 30, Color.GREEN), itemName);
    }

	/**
	 * Panel s popisom objektu. Vkladá sa do panela ScrollPanel.
	 * @return Panel s popisom
	 */
    public AnchorPane getDescription() {
        if (this.cachedDescription != null) return cachedDescription;
        return null;
    }

    protected void cacheDescription(AnchorPane descriptionPane) {
        this.cachedDescription = descriptionPane;
    }

    @Override
    public void select() {
        super.select();

        if (!this.isSelectable()) return;
        if (this instanceof Chip) return;

        double offset = 1;

        Bounds bounds = this.getBoundsInLocal();
        this.selectionShape = new Rectangle(bounds.getWidth() + 2 * offset, bounds.getHeight() + 2 * offset);

        this.selectionShape.setFill(null);
        for (double value : STROKE_DASH_ARRAY)
            this.selectionShape.getStrokeDashArray().add(value);

        //this.selectionShape.getStrokeDashArray().add(Collections.(STOKE_DASH_ARRAY));
        this.selectionShape.setStrokeWidth(2);
        this.selectionShape.setStroke(Color.BLACK);
        this.selectionShape.setStrokeLineCap(StrokeLineCap.ROUND);
        this.selectionShape.setOpacity(0.8);

        this.selectionShape.setLayoutX(-offset);
        this.selectionShape.setLayoutY(-offset);

        this.getChildren().add(this.selectionShape);
    }

    @Override
    public void deselect() {
        super.deselect();

        if (this instanceof Chip) return;

        this.getChildren().remove(this.selectionShape);
    }
}