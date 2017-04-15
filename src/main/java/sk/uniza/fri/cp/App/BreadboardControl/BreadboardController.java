package sk.uniza.fri.cp.App.BreadboardControl;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Point2D;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import org.controlsfx.control.ToggleSwitch;
import sk.uniza.fri.cp.BreadboardSim.*;
import sk.uniza.fri.cp.BreadboardSim.Board.Board;
import sk.uniza.fri.cp.BreadboardSim.Components.Breadboard;
import sk.uniza.fri.cp.BreadboardSim.Components.HexSegment;
import sk.uniza.fri.cp.BreadboardSim.Components.NumKeys;
import sk.uniza.fri.cp.BreadboardSim.Devices.Chips.*;
import sk.uniza.fri.cp.BreadboardSim.Devices.Chips.Gates.*;
import sk.uniza.fri.cp.BreadboardSim.Wire.Wire;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * Created by Moris on 4.3.2017.
 */

public class BreadboardController implements Initializable {

    //@FXML private BorderPane sceneRoot;
    //@FXML private HBox hbPicker;

    @FXML private VBox root;
    @FXML private AnchorPane boardPane;

    @FXML private VBox toolsBox;
    @FXML private ColorPicker wireColorPicker;
    @FXML private SplitPane toolsSplitPane;
    private ItemPicker newItemPicker;
    private ScrollPane descriptionPane;

    @FXML private Label lbCoordinates;

    @FXML
    private ToggleSwitch tsPower;

    private Board board;

    @Override
    public void initialize(URL location, ResourceBundle resources) {

        //tool box -> napravo - colorPicker, itemPicker, description
        this.wireColorPicker.setValue(Wire.getDefaultColor());
        this.wireColorPicker.setOnAction(event -> Wire.setDefaultColor(wireColorPicker.getValue()));

        this.descriptionPane = new ScrollPane();
        this.descriptionPane.setFitToHeight(true);
        this.descriptionPane.setFitToWidth(true);

        this.newItemPicker = new ItemPicker();
        this.newItemPicker.setPanelForDescription(this.descriptionPane);
        registerItems();

        toolsSplitPane.getItems().addAll(this.newItemPicker, this.descriptionPane);

        board = new Board(2000,2000);
        board.setDescriptionPane(this.descriptionPane);
        board.setOnMouseMoved(event -> {
            double offsetX = board.getViewportBounds().getMinX();
            double offsetY = board.getViewportBounds().getMinY();
            Point2D cursorPoint = board.sceneToLocal(event.getSceneX(), event.getSceneY());
            Point2D gridPoint = board.getGrid().getBox(cursorPoint.getX() - offsetX, cursorPoint.getY() - offsetY);
            lbCoordinates.setText(((int) gridPoint.getX()) + "x" + ((int) gridPoint.getY()));
        });

        //ak sa zmeni stav simulacie, zmen aj tlacitko spustania simulacie
        board.simRunningProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != this.tsPower.isSelected()) {
                this.tsPower.setSelected(newValue);
            }
        });

        this.tsPower.selectedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue)
                board.powerOn();
            else
                board.powerOff();
        });

        this.boardPane.getChildren().add(board);
    }

    public void callDelete(){
        this.board.deleteSelect();
    }

    public void powerOn() {
        this.board.powerOn();
    }

    private void registerItems(){
        this.newItemPicker.registerItem(new Gen7400());
        this.newItemPicker.registerItem(new Gen7402());
        this.newItemPicker.registerItem(new Gen7404());
        this.newItemPicker.registerItem(new Gen7408());
        this.newItemPicker.registerItem(new Gen7410());
        this.newItemPicker.registerItem(new Gen7430());
        this.newItemPicker.registerItem(new Gen7432());
        this.newItemPicker.registerItem(new Gen7486());
        this.newItemPicker.registerItem(new SN74125());
        this.newItemPicker.registerItem(new SN74138());
        this.newItemPicker.registerItem(new SN74148());
        this.newItemPicker.registerItem(new SN74151());
        this.newItemPicker.registerItem(new SN74573());
        this.newItemPicker.registerItem(new U6264B());

        this.newItemPicker.registerItem(new Breadboard());
        this.newItemPicker.registerItem(new HexSegment());
        this.newItemPicker.registerItem(new NumKeys());
    }

    @FXML
    private void handlePowerAction(){
        System.out.println("power");
        if (board.isSimulationRunning())
            board.powerOff();
        else
            board.powerOn();
    }

    @FXML
    private void handleSaveAction() {
        board.save();
    }

    @FXML
    private void handleLoadAction() {
        board.load();
    }

    @FXML
    private void handleClearBoardAction() {
        board.clearBoard();
    }
}
