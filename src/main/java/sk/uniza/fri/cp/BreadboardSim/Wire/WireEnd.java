package sk.uniza.fri.cp.BreadboardSim.Wire;


import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.EventDispatcher;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.transform.Transform;
import sk.uniza.fri.cp.BreadboardSim.Board.Board;
import sk.uniza.fri.cp.BreadboardSim.Board.BoardEvent;
import sk.uniza.fri.cp.BreadboardSim.Socket.Socket;

/**
 * pozna socket aby sa vedel podla neho premiestnovat - nabindovat pozicie
 * @author Moris
 * @version 1.0
 * @created 17-mar-2017 16:16:36
 */
public class WireEnd extends Joint {

    private static final Color CONNECTED_COLOR = Color.DARKGREEN;

	private Socket socket;

	private double lastPosX = -1;
	private double lastPosY = -1;

    private boolean moved;

    private ChangeListener<Transform> socketPositionChangeListener = (observable, oldValue, newValue) -> {
        if(lastPosX == -1){
            lastPosX = getLayoutX();
            lastPosY = getLayoutY();
        }

        setLayoutX(socket.getBoardX() / getBoard().getAppliedScale());
        setLayoutY(socket.getBoardY() / getBoard().getAppliedScale());

        getWire().moveJointsWithEnd(this, getLayoutX() - lastPosX, getLayoutY() - lastPosY);

        lastPosX = getLayoutX();
        lastPosY = getLayoutY();
    };

	public WireEnd(Board board, Wire wire){
		super(board, wire);

		this.addEventFilter(MouseEvent.MOUSE_PRESSED, event -> {
			getWire().setMouseTransparent(true);
			getWire().setOpacity(0.5);

            //board.addSelect(this);

//			event.consume();
        });

        this.addEventFilter(MouseEvent.DRAG_DETECTED, event -> {
            startFullDrag();

            this.moved = true;

            if (this.getSocket() != null)
                this.disconnectSocket();

            //event.consume();
        });

        this.addEventFilter(MouseEvent.MOUSE_RELEASED, event -> {
            getWire().setMouseTransparent(false);
            getWire().setOpacity(1);

            if (!this.moved)
                getBoard().addSelect(getWire());

            this.moved = false;

            event.consume();
        });

        this.incRadius();
    }

	@Override
	public void connectWireSegment(WireSegment segment) {
		this.wireSegments[0] = segment;
	}

    public boolean isConnected() {
        return this.socket != null;
    }

	public void connectSocket(Socket socket){
		if(this.socket == null) {
			this.socket = socket;
			this.socket.connect(this);
//            this.socket.getItem().localToParentTransformProperty().addListener(socketPositionChangeListener);
// TODO localToScene ma udajne vacsie naroky, no vola sa az po localToParent. lisener potrebuje akt. hodnotu lTS
            this.socket.getItem().localToSceneTransformProperty().addListener(socketPositionChangeListener);
            setLayoutX(socket.getBoardX() / getBoard().getAppliedScale());
            setLayoutY(socket.getBoardY() / getBoard().getAppliedScale());

            lastPosX = getLayoutX();
            lastPosY = getLayoutY();

            this.setColor(this.getWire().getColor().brighter());

            this.getWire().updatePotential();

		} else {
			disconnectSocket();
			connectSocket(socket);
		}
	}

    @Override
    public void setDefaultColor() {
        if (this.isConnected()) this.setColor(this.getWire().getColor().brighter());
        else super.setDefaultColor();
    }

    public void disconnectSocket() {
        if(this.socket == null) return;
		Socket socketToUpdate = this.socket;

//        this.socket.getItem().localToParentTransformProperty().removeListener(socketPositionChangeListener);
        this.socket.getItem().localToSceneTransformProperty().removeListener(socketPositionChangeListener);
        this.socket.disconnect();
		this.socket = null;
		this.getWire().updatePotential();

		this.setDefaultColor();

		//ak simulacia bezi, pridaj novy event pre update potencialu odpojeneho soketu
		if(getBoard().simRunningProperty().getValue())
			getBoard().addEvent(new BoardEvent(socketToUpdate));
	}

	public Socket getSocket(){
		return socket;
	}

    @Override
    public void delete() {
        super.delete();
        this.getWire().delete();
    }
}