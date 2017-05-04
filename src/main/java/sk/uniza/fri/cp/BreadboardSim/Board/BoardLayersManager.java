package sk.uniza.fri.cp.BreadboardSim.Board;


import javafx.scene.Group;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import sk.uniza.fri.cp.BreadboardSim.Components.Component;
import sk.uniza.fri.cp.BreadboardSim.SchoolBreadboard;
import sk.uniza.fri.cp.BreadboardSim.Devices.Device;
import sk.uniza.fri.cp.BreadboardSim.Wire.Joint;
import sk.uniza.fri.cp.BreadboardSim.Wire.Wire;

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
    private ArrayList<SchoolBreadboard> schoolBreadboards;
    private ArrayList<Component> components;
	private ArrayList<Device> devices;
	private ArrayList<Wire> wires;


    private int lastCompnentId = 0;

	public BoardLayersManager(Pane backgound){
		//inicializacia atributov
		this.backgroundLayer = backgound;
		this.componentsLayer = new Pane();
		this.devicesLayer = new Pane();
		this.wiresLayer = new Pane();
        this.schoolBreadboards = new ArrayList<>();
        this.components = new ArrayList<>();
		this.devices = new ArrayList<>();
		this.wires = new ArrayList<>();

        this.componentsLayer.setMinWidth(this.backgroundLayer.getBoundsInParent().getWidth());
        this.componentsLayer.setMinHeight(this.backgroundLayer.getBoundsInParent().getHeight());
        this.devicesLayer.setMinWidth(this.backgroundLayer.getBoundsInParent().getWidth());
        this.devicesLayer.setMinHeight(this.backgroundLayer.getBoundsInParent().getHeight());

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
            component.setId("c" + lastCompnentId++);
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

        if (object instanceof SchoolBreadboard) {
            SchoolBreadboard schoolBreadboard = (SchoolBreadboard) object;
            this.componentsLayer.getChildren().add(schoolBreadboard);
            this.schoolBreadboards.add(schoolBreadboard);
            //ak sa jedna o schoolbreadboard, pridaj vsetky komponenty do zoznamu ktore obsahuje zvlast
            for (Component schBComponent : schoolBreadboard.getComponents()) {
                if (schBComponent.getId() == null)
                    //ak este nebolo ID nastavene prirad mu nove
                    schBComponent.setId("c" + lastCompnentId++);
                else if (Integer.parseInt(schoolBreadboard.getId()) > lastCompnentId)
                    //ak uz bolo ID nastavene (napr. pri nacitavani), zisti ci nie je vacsie ako posledne triedy
                    lastCompnentId = Integer.parseInt(schoolBreadboard.getId());

                this.components.add(schBComponent);
            }
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

        if (object instanceof SchoolBreadboard) {
            SchoolBreadboard schoolBreadboard = ((SchoolBreadboard) object);
            this.componentsLayer.getChildren().remove(schoolBreadboard);
            this.schoolBreadboards.remove(schoolBreadboard);
            this.components.removeAll(schoolBreadboard.getComponents());
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

    public List<SchoolBreadboard> getSchoolBreadboards() {
        return new ArrayList<>(this.schoolBreadboards);
    }

	public List<Component> getComponents(){
        return new ArrayList<>(this.components);
    }

    public List<Device> getDevices() {
        return new ArrayList<>(this.devices);
    }

    public List<Wire> getWires() {
        return new ArrayList<>(this.wires);
    }

    /**
     * Odstránenie objektov na ploche okrem SchoolBreadboard
     */
    public void clear() {

        //cistenie kablikov
        //this.wiresLayer.getChildren().clear();
        //this.wires.clear();
        new ArrayList<>(this.wires).forEach(Wire::delete);

        //cistenie zariadeni
        //this.devicesLayer.getChildren().clear();
        //this.devices.clear();
        new ArrayList<>(this.devices).forEach(Device::delete);

        //cistenie komponentov
        SchoolBreadboard schoolBreadboard = SchoolBreadboard.getSchoolBreadboard(null);
        //this.componentsLayer.getChildren().retainAll(schoolBreadboard);
        //this.components.retainAll(schoolBreadboard.getComponents());
        ArrayList<Component> componentsToDelete = new ArrayList<>(this.components);
        componentsToDelete.removeAll(schoolBreadboard.getComponents());
        componentsToDelete.forEach(Component::delete);


    }
}