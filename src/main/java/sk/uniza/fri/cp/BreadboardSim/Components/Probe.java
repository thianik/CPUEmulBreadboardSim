package sk.uniza.fri.cp.BreadboardSim.Components;

import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import sk.uniza.fri.cp.BreadboardSim.Board.Board;
import sk.uniza.fri.cp.BreadboardSim.Board.GridSystem;
import sk.uniza.fri.cp.BreadboardSim.Devices.LED;
import sk.uniza.fri.cp.BreadboardSim.SchoolBreadboard;
import sk.uniza.fri.cp.BreadboardSim.Socket.Potential;
import sk.uniza.fri.cp.BreadboardSim.Socket.Socket;

/**
 * Created by Moris on 22.3.2017.
 */
public class Probe extends Component {

    private static final Color PROBE_ON_COLOR = Color.RED;
    private static final Color PROBE_OFF_COLOR = Color.YELLOW;
    private static final double RADIUS_COEF = 0.4;

    private Rectangle background;
    private Socket inputSocket;
    private Socket gndSocket;
    private Circle emitter;
    private LED led;

    public Probe() {
    }

    public Probe(Board board) {
        super(board);

        GridSystem grid = board.getGrid();

        this.background = new Rectangle(grid.getSizeX() * 5, grid.getSizeY() * 4, SchoolBreadboard.BACKGROUND_COLOR);

        //sokety
        this.inputSocket = new Socket(this);
        this.inputSocket.setLayoutX(grid.getSizeX());
        this.inputSocket.setLayoutY(grid.getSizeY());

        //vnutorny soket na uzemnenie LEDky
        this.gndSocket = new Socket(this, Potential.Value.LOW);

        this.emitter = new Circle(grid.getSizeMin() * RADIUS_COEF, PROBE_OFF_COLOR);
        this.led = new LED(board, this.emitter, PROBE_ON_COLOR);
        this.led.setLayoutX(grid.getSizeX() * 3);
        this.led.setLayoutY(grid.getSizeY());
        this.led.makeImmovable();

        Text probeText = Board.getLabelText("PROBE", grid.getSizeMin());
        probeText.setLayoutX(grid.getSizeX() * 3 - probeText.getBoundsInParent().getWidth() / 2); //centrovanie pod LEDku
        probeText.setLayoutY(grid.getSizeY() + probeText.getBoundsInParent().getHeight());

        this.inputSocket.connect(this.led.getAnode());
        this.inputSocket.lockPin();
        this.gndSocket.connect(this.led.getCathode());

        this.getChildren().addAll(this.background, this.inputSocket, this.led, probeText);
        this.addSocket(this.inputSocket);
    }

    public void removeBackground() {
        if (this.background != null) {
            this.getChildren().removeAll(this.background);
            this.background = null;
        }
    }
}
