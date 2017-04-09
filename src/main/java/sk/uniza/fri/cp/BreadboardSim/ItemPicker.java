package sk.uniza.fri.cp.BreadboardSim;


import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.MouseDragEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import sk.uniza.fri.cp.BreadboardSim.Components.Component;
import sk.uniza.fri.cp.BreadboardSim.Devices.Chips.Chip;
import sk.uniza.fri.cp.BreadboardSim.Devices.Device;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.LinkedList;

/**
 * @author Moris
 * @version 1.0
 * @created 17-mar-2017 16:16:35
 */
public class ItemPicker extends VBox {

	private ScrollPane contentPane;
	private ScrollPane descriptionPane;

	private FlowPane devicesPane;
	private FlowPane componentsPane;

	public ItemPicker(){
		this.contentPane = new ScrollPane();
		VBox.setVgrow(this.contentPane, Priority.ALWAYS);

		//panel s ikonami zariadeni
		this.devicesPane = new FlowPane();
		this.devicesPane.setVgap(5);
		this.devicesPane.setHgap(5);

		//panel s ikonami komponentov
		this.componentsPane = new FlowPane();
		this.componentsPane .setVgap(5);
		this.componentsPane .setHgap(5);

		//tlacidla pre zmenu zobrazovanych itemov
		Button btnDevices = new Button("Zariadenia");;
		btnDevices.setOnMouseClicked(event -> contentPane.setContent(devicesPane));

		Button btnComponents = new Button("Komponenty");
		btnComponents.setOnMouseClicked(event -> contentPane.setContent(componentsPane));

		HBox buttonsBox = new HBox(btnDevices, btnComponents);
		buttonsBox.setSpacing(10);
		buttonsBox.setAlignment(Pos.CENTER);

		//nastavenie defaultneho zobrazenia
		this.contentPane.setContent(this.devicesPane);
		this.contentPane.setFitToWidth(true);

		this.getChildren().addAll(buttonsBox, this.contentPane);
	}

	public void registerItem(Item item){
		if(item instanceof Device)
			devicesPane.getChildren().add(item);
		else if(item instanceof Component)
			componentsPane.getChildren().add(item);

		item.addEventFilter(MouseEvent.DRAG_DETECTED, event -> 	item.startFullDrag() );
		item.addEventFilter(MouseEvent.MOUSE_CLICKED, event -> {
			if(this.descriptionPane != null) this.descriptionPane.setContent(item.getDescription());
		});
	}

	public void setPanelForDescription(ScrollPane descriptionPane){
		this.descriptionPane = descriptionPane;
	}
}