package sk.uniza.fri.cp.BreadboardSim;


import javafx.beans.property.ObjectProperty;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.Parent;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.Pane;
import sk.uniza.fri.cp.BreadboardSim.Components.Component;
import sk.uniza.fri.cp.BreadboardSim.Devices.Device;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Moris
 * @version 1.0
 * @created 17-mar-2017 16:16:34
 */
public class BoardLayersManager {

	private Group layers;

	private Pane backgroundLayer;
	private Pane componentsLayer;
	private Pane devicesLayer;
	private Pane wiresLayer;
	private ArrayList<Component> components;
	private ArrayList<Device> devices;
	private ArrayList<Wire> wires;

	public BoardLayersManager(Pane backgound){
		//inicializacia atributov
		this.backgroundLayer = backgound;
		this.componentsLayer = new Pane();
		this.devicesLayer = new Pane();
		this.wiresLayer = new Pane();
		this.components = new ArrayList<>();
		this.devices = new ArrayList<>();
		this.wires = new ArrayList<>();

		this.layers = new Group(backgroundLayer, componentsLayer, devicesLayer, wiresLayer);

//		componentsLayer.setManaged(false);
//		devicesLayer.setManaged(false);
//		wiresLayer.setManaged(false);

		this.componentsLayer.setPickOnBounds(false);
		this.devicesLayer.setPickOnBounds(false);
		this.wiresLayer.setPickOnBounds(false);
	}

	public Group getLayers(){
		return layers;
	}

	public boolean add(Object object){
		if(object instanceof Wire){
			Wire wire = (Wire) object;
			this.wiresLayer.getChildren().add(wire);
			this.wires.add(wire);
			return true;
		}

		if(object instanceof Component){
			Component component = (Component) object;
			this.componentsLayer.getChildren().add(component);
			this.components.add(component);
			return true;
		}

		if(object instanceof Device){
			Device device = (Device) object;
			this.devicesLayer.getChildren().add(device);
			this.devices.add(device);
			return true;
		}

		return false;
	}

	public boolean remove(Object object){
		if(object instanceof Wire){
			Wire wire = (Wire) object;
			this.wiresLayer.getChildren().remove(wire);
			this.wires.remove(wire);
			return true;
		}

		if(object instanceof Joint){
			((Joint) object).getWire().removeJoint( (Joint)object);
		}

		if(object instanceof Component){
			Component component = (Component) object;
			this.componentsLayer.getChildren().remove(component);
			this.components.remove(component);
			return true;
		}

		if(object instanceof Device){
			Device device = (Device) object;
			this.devicesLayer.getChildren().remove(device);
			this.devices.remove(device);
			return true;
		}

		return false;
	}

	public Pane getLayer(String name){
		switch (name){
			case "background": return backgroundLayer;
			case "components": return componentsLayer;
			case "devices": return devicesLayer;
			case "wires": return wiresLayer;
		}

		return backgroundLayer;
	}

	public List<Component> getComponents(){
		return components;
	}
}