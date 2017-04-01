package sk.uniza.fri.cp.App.BreadboardControl;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Point2D;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import sk.uniza.fri.cp.BreadboardSim.*;
import sk.uniza.fri.cp.BreadboardSim.Devices.Chips.Gates.Gen7400;
import sk.uniza.fri.cp.BreadboardSim.Devices.Chips.Gates.Gen7402;

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

        this.boardPane.getChildren().add(board);
    }

    public void callDelete(){
        this.board.deleteSelect();
    }

    private void registerItems(){
        this.newItemPicker.registerItem(new Gen7400());
        this.newItemPicker.registerItem(new Gen7402());
    }

    @FXML
    private void handlePowerAction(){
        System.out.println("power");
        if(board.simRunningProperty().getValue())
            board.powerOff();
        else
            board.powerOn();
    }

}
