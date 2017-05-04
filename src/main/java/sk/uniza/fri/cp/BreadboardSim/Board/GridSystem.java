package sk.uniza.fri.cp.BreadboardSim.Board;


import javafx.geometry.Point2D;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Paint;
import javafx.scene.shape.*;
import javafx.scene.shape.Line;

/**
 * @author Moris
 * @version 1.0
 * @created 17-mar-2017 16:16:34
 */
public class GridSystem {

	private int sizeX;
	private int sizeY;
    private double scale = 1;

	public GridSystem(int sizeX, int sizeY){
		this.sizeX = sizeX;
		this.sizeY = sizeY;
	}

	public GridSystem(int size){
		this.sizeX = size;
		this.sizeY = size;
	}

    public void changeScale(double scale) {
        this.scale = scale;
    }

    public int getSizeX() {
        return (int) (sizeX * this.scale);
    }

    public int getSizeY() {
        return (int) (sizeY * this.scale);
    }

	public int getSizeMin(){ return Math.min(sizeX, sizeY);}

	public int getSizeMax(){ return Math.max(sizeX, sizeY);}

	public Point2D screenToGrid(double screenX, double screenY){
		return new Point2D(0,0);
	}

	public Point2D getPosition(double x, double y){
		int gridX = 0;
		int gridY = 0;

		gridX = (int) (Math.round(x) / sizeX) * sizeX;
		gridY = (int) (Math.round(y) / sizeY) * sizeY;

        return new Point2D(gridX * this.scale, gridY * this.scale);
    }

	public Point2D getPosition(Point2D point){
		return getPosition(point.getX(), point.getY());
	}

	public Point2D getBox(double x, double y){
		Point2D position = getPosition(x, y);
		return new Point2D(position.getX() / sizeX, position.getY() / sizeY);
	}

	public Point2D gridToLocal(int gridX, int gridY){
        return new Point2D(gridX * sizeX * this.scale,
                gridY * sizeY * this.scale);
    }

	public Pane generateBackground(double width, double height, Paint bgColor, Paint linesColor){
		Pane bck = new Pane(new Rectangle(width, height, bgColor));

		for (int x = 0; x <= width; x += sizeX){
			bck.getChildren().add(generateGridLine(x, 0, x, height, linesColor));
		}

		for (int y = 0; y <= height; y += sizeY){
			bck.getChildren().add(generateGridLine(0, y, width, y, linesColor));
		}

		return bck;
	}

	private Line generateGridLine(double startX, double startY, double endX, double endY, Paint lineColor){
		Line line = new Line(startX, startY, endX, endY);
		line.setStroke(lineColor);
		line.setOpacity(0.5);
		line.setStrokeWidth(1);

		return line;
	}

}