package sk.uniza.fri.cp.BreadboardSim.Components;


import javafx.geometry.Bounds;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;
import javafx.scene.shape.StrokeLineCap;
import sk.uniza.fri.cp.BreadboardSim.*;
import sk.uniza.fri.cp.BreadboardSim.Board.Board;
import sk.uniza.fri.cp.BreadboardSim.Devices.Device;
import sk.uniza.fri.cp.BreadboardSim.Socket.PowerSocket;
import sk.uniza.fri.cp.BreadboardSim.Socket.Socket;
import sk.uniza.fri.cp.BreadboardSim.Wire.Wire;

import java.util.*;
import java.util.List;

/**
 * @author Moris
 * @version 1.0
 * @created 17-mar-2017 16:16:34
 */
public abstract class Component extends Item {

    protected int gridWidth;
    protected int gridHeight;

	private LinkedList<ConnectedDevice> connectedDevices;

    //	private Shape selectionShape; //prekrytie pri selecte
    private LinkedList<Wire> connectedWires;
    private ArrayList<Socket> sockets; //pole vsetkych soketov na komponente
    private LinkedList<PowerSocket> powerSockets;

    /**
     * Konštruktor pre itempicker
     */
    public Component() {
    }

    public Component(Board board) {
        super(board);
		this.powerSockets = new LinkedList<>();
        this.sockets = new ArrayList<>();
        this.connectedDevices = new LinkedList<>();
		this.connectedWires = new LinkedList<>();
	}

	public List<PowerSocket> getPowerSockets(){
		return powerSockets;
	}

	public void addPowerSocket(PowerSocket ps){
		this.powerSockets.add(ps);
	}

	public void addAllPowerSockets(PowerSocket... ps){
		this.powerSockets.addAll(Arrays.asList(ps));
	}

	public void addAllPowerSockets(List<PowerSocket> ps){
        this.addAllSockets(ps);
        this.powerSockets.addAll(ps);
    }

    /**
     * Registrácia soketu viditeľného na komponente. Soketu je pri registrácií priradené unkiátne id v rámci komponentu.
     *
     * @param socket Soket viditeľný a prístupný na komponente.
     */
    protected void addSocket(Socket socket) {
        //id soketu na zaklade velkosti pola soketov -> id je jeho index
        socket.setId(Integer.toString(this.sockets.size()));
        this.sockets.add(socket);
    }

    protected void addAllSockets(Socket... sockets) {
        for (Socket socket : sockets)
            addSocket(socket);
    }

    protected void addAllSockets(List<? extends Socket> sockets) {
        for (Socket socket : sockets)
            addSocket(socket);
    }

    public ArrayList<Socket> getSockets() {
        return sockets;
    }

    public Socket getSocket(int id) {
        return this.sockets.get(id);
    }

	public boolean addDevice(Device device){
		return connectedDevices.add(new ConnectedDevice(device));
	}

	public boolean removeDevice(Device device){
		return connectedDevices.removeIf(cd -> cd.getDevice() == device);
	}

	public boolean addWire(Wire wire){ return this.connectedWires.add(wire); }

    public boolean removeWire(Wire wire){
        return connectedWires.remove(wire);
    }

	public void updateConnectedDevicesPosition(){
		connectedDevices.forEach(ConnectedDevice::updatePos);
	}

	public int getGridWidth() {
	    return gridWidth;
    };

	public int getGridHeight() {
	    return gridHeight;
    };

	@Override
	public void moveTo(int gridPosX, int gridPosY) {
		super.moveTo(gridPosX, gridPosY);

		updateConnectedDevicesPosition();
	}

//	@Override
//	public void select() {
//		super.select();
//
//		if(!this.isSelectable()) return;
//
//		double offset = 1;
//
//		Bounds bounds = this.getBoundsInLocal();
//		this.selectionShape = new Rectangle(bounds.getWidth() + 2*offset, bounds.getHeight() + 2*offset);
//
//		this.selectionShape.setFill(null);
//		for (double value : STROKE_DASH_ARRAY)
//			this.selectionShape.getStrokeDashArray().add(value);
//
//		//this.selectionShape.getStrokeDashArray().add(Collections.(STOKE_DASH_ARRAY));
//		this.selectionShape.setStrokeWidth(2);
//		this.selectionShape.setStroke(Color.BLACK);
//		this.selectionShape.setStrokeLineCap(StrokeLineCap.ROUND);
//		this.selectionShape.setOpacity(0.8);
//
//		this.selectionShape.setLayoutX(-offset);
//		this.selectionShape.setLayoutY(-offset);
//
//		this.getChildren().add(this.selectionShape);
//	}
//
//	@Override
//	public void deselect() {
//		super.deselect();
//
//		this.getChildren().remove(this.selectionShape);
//	}

	@Override
	public boolean equals(Object obj) {
		//ak maju oba objekty ako predka rovnaky SchoolBreadboard, berieme ze je to jeden komponent
		if(obj instanceof Component
				&& ((Component) obj).getParent() instanceof SchoolBreadboard
				&& this.getParent() instanceof SchoolBreadboard ){
			if(((Component) obj).getParent() == this.getParent())
				return true;
		}

		return super.equals(obj);
	}

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    @Override
    public void delete() {
        super.delete();

        //zmazanie pripojenych zariadeni
        this.connectedDevices.forEach(ConnectedDevice::delete);

        //zamaznie pripojenych kablikov
        //kedze su v zozname z ktoreho cerpame a zaroven ich mazeme, treba povolat pomoc
        for (Wire wire : connectedWires.toArray(new Wire[0]))
            wire.delete();

    }

    private class ConnectedDevice{

        private Device device;
        private int deviceGridOffsetX;
        private int deviceGridOffsetY;

        ConnectedDevice(Device device){
            this.device = device;
            this.deviceGridOffsetX = device.getGridPosX() - getGridPosX();
            this.deviceGridOffsetY = device.getGridPosY() - getGridPosY();
        }

        void updatePos(){
            device.moveTo(getGridPosX() + deviceGridOffsetX, getGridPosY() + deviceGridOffsetY);
        }

        Device getDevice() {
            return device;
        }

        void delete(){
            this.device.delete();
        }
    }


    @Override
    public Pane getImage() {
        return super.getImage();
    }

    @Override
    public AnchorPane getDescription() {
        return super.getDescription();
    }
}