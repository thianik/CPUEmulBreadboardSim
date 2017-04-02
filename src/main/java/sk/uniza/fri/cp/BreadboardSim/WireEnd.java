package sk.uniza.fri.cp.BreadboardSim;


import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.EventHandler;
import javafx.geometry.Point2D;
import javafx.scene.input.MouseEvent;
import javafx.scene.transform.Transform;
import javafx.scene.transform.TransformChangedEvent;

/**
 * pozna socket aby sa vedel podla neho premiestnovat - nabindovat pozicie
 * @author Moris
 * @version 1.0
 * @created 17-mar-2017 16:16:36
 */
public class WireEnd extends Joint {

	private Socket socket;

	private double lastPosX = -1;
	private double lastPosY = -1;

	private ChangeListener<Transform> socketPositionChangeListener = new ChangeListener<Transform>() {
		@Override
		public void changed(ObservableValue<? extends Transform> observable, Transform oldValue, Transform newValue) {
			if(lastPosX == -1){
				lastPosX = getLayoutX();
				lastPosY = getLayoutY();
			}

			setLayoutX(socket.getBoardX());
			setLayoutY(socket.getBoardY());

			getWire().moveBy(getLayoutX() - lastPosX, getLayoutY() - lastPosY );
			lastPosX = getLayoutX();
			lastPosY = getLayoutY();
		}
	};

	public WireEnd(Board board, Wire wire){
		super(board, wire);

		this.addEventFilter(MouseEvent.MOUSE_PRESSED, event -> {
			getWire().setMouseTransparent(true);
			getWire().setOpacity(0.5);

			board.addSelect(this);

			event.consume();
		});

		this.addEventFilter(MouseEvent.MOUSE_RELEASED, event -> {
			getWire().setMouseTransparent(false);
			getWire().setOpacity(1);

			event.consume();
		});

		this.addEventFilter(MouseEvent.DRAG_DETECTED, event -> {
			startFullDrag();

			if(this.getSocket() != null)
				this.disconnectSocket();

			event.consume();
		});

	}

	@Override
	public void connectWireSegment(WireSegment segment) {
		this.wireSegments[0] = segment;
	}

	public void connectSocket(Socket socket){
		if(this.socket == null) {
			this.socket = socket;
			this.socket.connect(this);
			this.socket.localToSceneTransformProperty().addListener(socketPositionChangeListener);
			setLayoutX(socket.getBoardX());
			setLayoutY(socket.getBoardY());

			this.getWire().updatePotential();
		} else {
			disconnectSocket();
			connectSocket(socket);
		}
	}

	public void disconnectSocket(){
	    if(this.socket == null) return;
		Socket socketToUpdate = this.socket;

		this.socket.localToSceneTransformProperty().removeListener(socketPositionChangeListener);
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

}