package sk.uniza.fri.cp.BreadboardSim;


import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.*;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import sk.uniza.fri.cp.BreadboardSim.Components.*;
import sk.uniza.fri.cp.BreadboardSim.Devices.Chips.Gates.Gen7400;
import sk.uniza.fri.cp.BreadboardSim.Devices.Device;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Plocha s breadboardmi ku ktorym su pripajane komponenty
 * @author Moris
 * @version 1.0
 * @created 17-mar-2017 16:16:34
 */
public class Board extends ScrollPane {

//	private int widthGrid;
//	private int heightGrid;
	private double widthPx;
	private double heightPx;
	private ArrayList<Selectable> selected;
	private BoardSimulator simulator;
	private GridSystem gridSystem;
	private BoardLayersManager layersManager;

	private ObservableValue<Point2D> cursorPosition;

	private Item addingItem;

	private ScrollPane descriptionPane;

	//pomocne funkcie
	public static Text getLabelText(String text, int size){
		Text out = new Text(text);
		out.setFont(Font.font(size));
		out.setId("breadboardLabel");
		out.setStrokeWidth(0);

		return out;
	}

	//TODO neda sa posuvat ScrollPane v SplitPane
	public Board(double width, double height){
		//inicializacia atributov
		this.widthPx = width;
		this.heightPx = height;
		this.selected = new ArrayList<>();
		this.simulator = new BoardSimulator();
		this.gridSystem = new GridSystem(10);
		Pane gridBackground = gridSystem.generateBackground(this.widthPx, this.heightPx, Color.WHITESMOKE, Color.GRAY);
		this.layersManager = new BoardLayersManager(gridBackground);

		this.cursorPosition = new SimpleObjectProperty<>();

		this.setContent(this.layersManager.getLayers());
		this.setPannable(true);

		gridBackground.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> clearSelect());


		//sandbox
		SchoolBreadboard sb = SchoolBreadboard.getSchoolBreadboard(this);
		this.addItem(sb);
		sb.moveTo(2,2);
		this.addItem(new Gen7400(this));
		this.addItem(new Gen7400(this));
		this.addItem(new Breadboard(this));


		this.addEventHandler(MouseDragEvent.MOUSE_DRAG_ENTERED, new EventHandler<MouseDragEvent>() {
			@Override
			public void handle(MouseDragEvent event) {
				if(addingItem == null && event.getGestureSource() instanceof Item){
					Item item = ((Item) event.getGestureSource());
					Board board = ((Board) event.getSource());

					try {
						addingItem = item.getClass().getConstructor(Board.class).newInstance(board);
						addItem(addingItem);
						//addingItem.moveToMousePosition(event, true);
						Event.fireEvent(addingItem, new MouseEvent(MouseEvent.MOUSE_DRAGGED, event.getX(), event.getY(),
								event.getScreenX(), event.getScreenY(), MouseButton.PRIMARY, 1, true,
								true, true, true, true, true,
								true, true, true, true, null));

					} catch (InstantiationException e) {
						e.printStackTrace();
					} catch (IllegalAccessException e) {
						e.printStackTrace();
					} catch (InvocationTargetException e) {
						e.printStackTrace();
					} catch (NoSuchMethodException e) {
						e.printStackTrace();
					}


				}
			}
		});


		this.addEventFilter(MouseDragEvent.MOUSE_DRAG_OVER, new EventHandler<MouseEvent>() {
			@Override
			public void handle(MouseEvent event) {
				if(addingItem != null){
					Event.fireEvent(addingItem, new MouseEvent(MouseEvent.MOUSE_DRAGGED, event.getX(), event.getY(),
							event.getScreenX(), event.getScreenY(), MouseButton.PRIMARY, 1, true,
							true, true, true, true, true,
							true, true, true, true, null));
				}
			}
		});

		this.addEventHandler(MouseDragEvent.MOUSE_DRAG_RELEASED, new EventHandler<MouseDragEvent>() {
			@Override
			public void handle(MouseDragEvent event) {
				if(addingItem != null){
					Event.fireEvent(addingItem, new MouseEvent(MouseEvent.MOUSE_RELEASED, event.getX(), event.getY(),
							event.getScreenX(), event.getScreenY(), MouseButton.PRIMARY, 1, true,
							true, true, true, true, true,
							true, true, true, true, null));
					addingItem = null;
				}
			}
		});

		this.addEventFilter(MouseDragEvent.MOUSE_DRAG_EXITED, new EventHandler<MouseDragEvent>() {
			@Override
			public void handle(MouseDragEvent event) {
				if(addingItem != null) {

					//TODO pri odchode kurzora z board-u a zmazani pridavaneho itemu vyskakuje exception...
					//predpokladam, ze by to mohlo byt sposobene odpalenim eventu pri pohybe kurzora na okraji plochy
					//pricom po odchode sa item zmaze ale event sa k nemu este nedostal... resp. chyba nastava pri MOUSE_DRAG_EXITED_TARGET
					addingItem.delete();
					addingItem = null;
				}

				event.consume();
			}
		});

	}

	public GridSystem getGrid(){
		return gridSystem;
	}

	public double getWidthPx(){
	    return this.widthPx;
    }

    public double getHeightPx(){
        return this.heightPx;
    }

    public double getOriginSceneOffsetX(){
        return layersManager.getLayer("background").getLocalToSceneTransform().getTx();
    }

    public double getOriginSceneOffsetY(){
        return layersManager.getLayer("background").getLocalToSceneTransform().getTy();
    }

	public void setDescriptionPane(ScrollPane descriptionPane){
		this.descriptionPane = descriptionPane;
	}

	/**
	 * Pridá položku do akutálneho výberu.
	 * 
	 * @param item Nová položka
	 * @return
	 */
	public boolean addSelect(Selectable item){
		if(((Group) item).getParent() instanceof SchoolBreadboard)
			item = (SchoolBreadboard) ((Group) item).getParent();

		//ak je toto jediný vybraný item, ukáž informácie o ňom
		if(selected.size() == 0)
			 this.descriptionPane.setContent(item.getDescription());

		if(!selected.contains(item)) {
			item.select();
			return selected.add(item);
		}

		return false;
	}

	/**
	 * Odoberie položku z aktuálneho výberu
	 * 
	 * @param item Položka na odobratie
	 * @return
	 */
	public boolean removeSelect(Selectable item){
		item.deselect();
		return selected.remove(item);
	}

	public void clearSelect(){
		selected.forEach(item -> item.deselect());
		selected.clear();
		this.descriptionPane.setContent(null);
	}

	public void deleteSelect(){
		selected.forEach(item -> item.delete());
	}

	/**
	 * Pridá položku na plochu. Podla typu ju zaradí do odpovedajúcej vrstvy.
	 * Možné položky
	 * 
	 * @param item Nová položka na ploche.
	 * @return
	 */
	public boolean addItem(Object item){
		return layersManager.add(item);
	}

    public boolean removeItem(Object item){
        return layersManager.remove(item);
    }



	public void powerOn(){
		List<PowerSocket> powerSockets = new LinkedList<>();

    	layersManager.getComponents().forEach(component -> {
    		powerSockets.addAll(component.getPowerSockets());
		});

		simulator.start(powerSockets);
	}

	public void powerOff(){
		simulator.stop();
	}

	public boolean isSimulationRunning(){ return simRunningProperty().getValue(); }

	public ReadOnlyBooleanProperty simRunningProperty(){
		return simulator.runningProperty();
	}

	/*
	DETEKCIA KOLIZII -> PIN - SOKET
	 */

	/**
	 * Kontroluje prienik bounds vsetkych komponentov na ploche a daneho zariadenia.
	 * @param device Zariadenie, pre ktore sa ma kontrolovat prienik
	 * @return List komponentov s ktorymi je v prieniku. Ak nie je v prieniku so ziadnym komponentom, vracia null;
	 */
	public List<Component> checkForCollisionWithComponent(Device device){
		List<Component> components = null;

		//skontroluj vsetky komponenty, ci sa s nimi zariadenie prekryva
		for (Component component : layersManager.getComponents()) {
			if(component.getBoundsInParent().intersects(device.getBoundsInParent())){
				//ak zoznam neexistuje, vytvor ho
				if(components == null) components = new LinkedList<>();
				//ak ano, pridaj ho do zoznamu na zaciatok -> od najvrchnejsich k spodnym
				components.add(0,component);
			}
		}

		return components;
	}

	public Socket checkForCollisionWithSocket(Component component, Pin pin){
		//board.layersManager.getComponents().get(0).getSocketsForDevices().get(0).localToScene(board.layersManager.getComponents().get(0).getSocketsForDevices().get(0).getBoundsInLocal())
		for (Socket socket : component.getSocketsForDevices()) {
			if(socket.localToScene(socket.getBoundsInLocal()).intersects(pin.localToScene(pin.getBoundsInLocal())))
				return socket;
		}

		return null;
	}

	/**
	 * 
	 * @param event
	 */
	public void addEvent(BoardEvent event){
		simulator.addEvent(event);
	}

}