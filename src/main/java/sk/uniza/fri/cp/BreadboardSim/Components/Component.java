package sk.uniza.fri.cp.BreadboardSim.Components;


import javafx.geometry.Bounds;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeType;
import sk.uniza.fri.cp.BreadboardSim.Board;
import sk.uniza.fri.cp.BreadboardSim.PowerSocket;
import sk.uniza.fri.cp.BreadboardSim.Socket;
import sk.uniza.fri.cp.BreadboardSim.Devices.Device;
import sk.uniza.fri.cp.BreadboardSim.Item;

import java.util.*;
import java.util.List;

/**
 * @author Moris
 * @version 1.0
 * @created 17-mar-2017 16:16:34
 */
public abstract class Component extends Item {

	private LinkedList<PowerSocket> powerSockets;
	protected ArrayList<Socket> socketsForDevices;

    protected int gridWidth;
    protected int gridHeight;

    private int id;
	private LinkedList<ConnectedDevice> connectedDevices;

	private Shape selectionShape; //prekrytie pri selecte

	public Component(Board board, int id){
		super(board);
		this.id = id;
		this.powerSockets = new LinkedList<>();
		this.socketsForDevices = new ArrayList<>();
		this.connectedDevices = new LinkedList<>();
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
		this.powerSockets.addAll(ps);
	}

	public ArrayList<Socket> getSocketsForDevices(){
		return socketsForDevices;
	}

	public int getComponentId(){
		return this.id;
	}


	public boolean addDevice(Device device){
		return connectedDevices.add(new ConnectedDevice(device));
	}

	public boolean removeDevice(Device device){
		return connectedDevices.removeIf(cd -> cd.getDevice() == device);
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
	}



	@Override
	public void select() {
		super.select();

		double offset = 1;

		Bounds bounds = this.getBoundsInLocal();
		this.selectionShape = new Rectangle(bounds.getWidth() + 2*offset, bounds.getHeight() + 2*offset);

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

		this.getChildren().remove(this.selectionShape);
	}

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
}