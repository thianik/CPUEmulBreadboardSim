package sk.uniza.fri.cp.BreadboardSim.Board;


import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.*;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import org.jdom2.*;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.XMLOutputter;
import sk.uniza.fri.cp.BreadboardSim.Components.*;
import sk.uniza.fri.cp.BreadboardSim.DescriptionPane;
import sk.uniza.fri.cp.BreadboardSim.Devices.Chips.*;
import sk.uniza.fri.cp.BreadboardSim.Devices.Chips.Gates.*;
import sk.uniza.fri.cp.BreadboardSim.Devices.Device;
import sk.uniza.fri.cp.BreadboardSim.Devices.Pin.Pin;
import sk.uniza.fri.cp.BreadboardSim.Item;
import sk.uniza.fri.cp.BreadboardSim.SchoolBreadboard;
import sk.uniza.fri.cp.BreadboardSim.Socket.PowerSocket;
import sk.uniza.fri.cp.BreadboardSim.Selectable;
import sk.uniza.fri.cp.BreadboardSim.Socket.Socket;
import sk.uniza.fri.cp.BreadboardSim.Wire.Joint;
import sk.uniza.fri.cp.BreadboardSim.Wire.Wire;
import sk.uniza.fri.cp.BreadboardSim.Wire.WireEnd;

import java.io.*;
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
    private boolean hasChanged = false;

	private Item addingItem;

    private DescriptionPane descriptionPane;

	//pomocne funkcie
	public static Text getLabelText(String text, int size){
		Text out = new Text(text);
		out.setFont(Font.font(size));
		out.setId("breadboardLabel");
		out.setStrokeWidth(0);

		return out;
	}

    private double SCALE_DELTA = 1.1;
    //private double scale_total = 1;
    private SimpleDoubleProperty scale_total = new SimpleDoubleProperty(1);

    public double getAppliedScale() {
        return scale_total.getValue();
    }

    public SimpleDoubleProperty zoomScaleProperty() {
        return scale_total;
    }

    public Board(double width, double height, int gridSizePx) {
        //inicializacia atributov
		this.widthPx = width;
		this.heightPx = height;
		this.selected = new ArrayList<>();
		this.simulator = new BoardSimulator();
        this.gridSystem = new GridSystem(gridSizePx);
        Pane gridBackground = gridSystem.generateBackground(this.widthPx, this.heightPx, Color.WHITESMOKE, Color.GRAY);
		this.layersManager = new BoardLayersManager(gridBackground);

        this.setPannable(false);

        gridBackground.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> {
            if (event.getButton().name().equalsIgnoreCase("PRIMARY"))
                clearSelect();
        });

        // Let the ScrollPane.viewRect only pan on middle button.
        //znefunkcni pridavanie objektov
//        this.getContent().addEventHandler(MouseEvent.ANY, event -> {
//            if (event.getButton() != MouseButton.MIDDLE) event.consume();
//        });

		//sandbox
        SchoolBreadboard sb = new SchoolBreadboard(this);
        this.addItem(sb);
        sb.moveTo((int) (width / gridSizePx / 2 - sb.getGridWidth() / 2), (int) (height / gridSizePx / 2 - sb.getGridHeight() / 2));

        this.hasChanged = false;


        //ZOOM credit: http://stackoverflow.com/questions/16680295/javafx-correct-scaling
        final Group contentGroup = this.layersManager.getLayers();
        final StackPane zoomPane = new StackPane(contentGroup);
        final Group scrollContent = new Group(zoomPane);
        this.setContent(scrollContent);

        this.viewportBoundsProperty().addListener(new ChangeListener<Bounds>() {
            @Override
            public void changed(ObservableValue<? extends Bounds> observable,
                                Bounds oldValue, Bounds newValue) {
                zoomPane.setMinSize(newValue.getWidth(), newValue.getHeight());
            }
        });

        this.setPrefViewportHeight(200);
        this.setPrefViewportHeight(200);

        zoomPane.setOnScroll(event -> {
            event.consume();

            if (event.getDeltaY() == 0) {
                return;
            }

            double scaleFactor = (event.getDeltaY() > 0) ? SCALE_DELTA
                    : 1 / SCALE_DELTA;

            if (scaleFactor * scale_total.get() >= 0.3 && scaleFactor * scale_total.get() <= 3) {
                // amount of scrolling in each direction in scrollContent coordinate
                // units
                Point2D scrollOffset = figureScrollOffset(scrollContent, this);

                contentGroup.setScaleX(contentGroup.getScaleX() * scaleFactor);
                contentGroup.setScaleY(contentGroup.getScaleY() * scaleFactor);

                scale_total.setValue(scale_total.doubleValue() * scaleFactor);


                // move viewport so that old center remains in the center after the
                // scaling
                repositionScroller(scrollContent, this, scaleFactor, scrollOffset);
            }
        });

        // Panning via drag....
        final ObjectProperty<Point2D> lastMouseCoordinates = new SimpleObjectProperty<Point2D>();
        scrollContent.setOnMousePressed(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                lastMouseCoordinates.set(new Point2D(event.getX(), event.getY()));
            }
        });

        ScrollPane scroller = this;
        scrollContent.setOnMouseDragged(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                double deltaX = event.getX() - lastMouseCoordinates.get().getX();
                double extraWidth = scrollContent.getLayoutBounds().getWidth() - scroller.getViewportBounds().getWidth();
                double deltaH = deltaX * (scroller.getHmax() - scroller.getHmin()) / extraWidth;
                double desiredH = scroller.getHvalue() - deltaH;
                scroller.setHvalue(Math.max(0, Math.min(scroller.getHmax(), desiredH)));

                double deltaY = event.getY() - lastMouseCoordinates.get().getY();
                double extraHeight = scrollContent.getLayoutBounds().getHeight() - scroller.getViewportBounds().getHeight();
                double deltaV = deltaY * (scroller.getHmax() - scroller.getHmin()) / extraHeight;
                double desiredV = scroller.getVvalue() - deltaV;
                scroller.setVvalue(Math.max(0, Math.min(scroller.getVmax(), desiredV)));
            }
        });



        this.addEventHandler(MouseDragEvent.MOUSE_DRAG_ENTERED, event -> {
                hasChanged = true;
                if(addingItem == null && event.getGestureSource() instanceof Item){
					Item item = ((Item) event.getGestureSource());
					Board board = ((Board) event.getSource());

					try {
						addingItem = item.getClass().getConstructor(Board.class).newInstance(board);
						addItem(addingItem);
                        Point2D point = this.getContent().sceneToLocal(event.getSceneX(), event.getSceneY());
                        Event.fireEvent(addingItem, new MouseEvent(MouseEvent.MOUSE_DRAGGED, event.getSceneX(), event.getSceneY(),
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
        );

        this.addEventFilter(MouseDragEvent.MOUSE_DRAG_OVER, event -> {
            if(addingItem != null){
                    Point2D point = this.getContent().sceneToLocal(event.getSceneX(), event.getSceneY());
                Event.fireEvent(addingItem, new MouseEvent(MouseEvent.MOUSE_DRAGGED, event.getSceneX(), event.getSceneY(),
                        event.getScreenX(), event.getScreenY(), MouseButton.PRIMARY, 1, true,
                            true, true, true, true, true,
							true, true, true, true, null));
				}
			}
        );

        this.addEventFilter(MouseDragEvent.MOUSE_DRAG_RELEASED, event -> {
            if(addingItem != null){
                    Point2D point = this.getContent().sceneToLocal(event.getSceneX(), event.getSceneY());
                Event.fireEvent(addingItem, new MouseEvent(MouseEvent.MOUSE_RELEASED, event.getSceneX(), event.getSceneY(),
                        event.getScreenX(), event.getScreenY(), MouseButton.PRIMARY, 1, true,
                        true, true, true, true, true,
                        true, true, true, true, null));
					addingItem = null;
				}
			}
        );

        this.addEventHandler(MouseDragEvent.MOUSE_DRAG_EXITED, new EventHandler<MouseDragEvent>() {
            @Override
            public void handle(MouseDragEvent event) {
				if(addingItem != null) {
                    addingItem.delete();
                    addingItem = null;
				}

				event.consume();
			}
		});

	}


    private Point2D figureScrollOffset(Node scrollContent, ScrollPane scroller) {
        double extraWidth = scrollContent.getLayoutBounds().getWidth() - scroller.getViewportBounds().getWidth();
        double hScrollProportion = (scroller.getHvalue() - scroller.getHmin()) / (scroller.getHmax() - scroller.getHmin());
        double scrollXOffset = hScrollProportion * Math.max(0, extraWidth);
        double extraHeight = scrollContent.getLayoutBounds().getHeight() - scroller.getViewportBounds().getHeight();
        double vScrollProportion = (scroller.getVvalue() - scroller.getVmin()) / (scroller.getVmax() - scroller.getVmin());
        double scrollYOffset = vScrollProportion * Math.max(0, extraHeight);
        return new Point2D(scrollXOffset, scrollYOffset);
    }

    private void repositionScroller(Node scrollContent, ScrollPane scroller, double scaleFactor, Point2D scrollOffset) {
        double scrollXOffset = scrollOffset.getX();
        double scrollYOffset = scrollOffset.getY();
        double extraWidth = scrollContent.getLayoutBounds().getWidth() - scroller.getViewportBounds().getWidth();
        if (extraWidth > 0) {
            double halfWidth = scroller.getViewportBounds().getWidth() / 2;
            double newScrollXOffset = (scaleFactor - 1) * halfWidth + scaleFactor * scrollXOffset;
            scroller.setHvalue(scroller.getHmin() + newScrollXOffset * (scroller.getHmax() - scroller.getHmin()) / extraWidth);
        } else {
            scroller.setHvalue(scroller.getHmin());
        }
        double extraHeight = scrollContent.getLayoutBounds().getHeight() - scroller.getViewportBounds().getHeight();
        if (extraHeight > 0) {
            double halfHeight = scroller.getViewportBounds().getHeight() / 2;
            double newScrollYOffset = (scaleFactor - 1) * halfHeight + scaleFactor * scrollYOffset;
            scroller.setVvalue(scroller.getVmin() + newScrollYOffset * (scroller.getVmax() - scroller.getVmin()) / extraHeight);
        } else {
            scroller.setHvalue(scroller.getHmin());
        }
    }


    /**
     * Vracia informaciu o zmene od nastavenia mark.
     *
     * @return
     */
    public boolean hasChanged() {
        return hasChanged;
    }

    public void clearChange() {
        this.hasChanged = false;
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

    //test
    public Point2D sceneToBoard(double sceneX, double sceneY) {
        return layersManager.getLayer("background").sceneToLocal(sceneX, sceneY);
    }

    public double getOriginSceneOffsetX(){
        return layersManager.getLayer("background").getLocalToSceneTransform().getTx();
    }

    public double getOriginSceneOffsetY(){
        return layersManager.getLayer("background").getLocalToSceneTransform().getTy();
    }

    public void setDescriptionPane(DescriptionPane descriptionPane) {
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
            this.descriptionPane.setDescription(item);

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
        this.descriptionPane.clear();
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
	public boolean addItem(Object item) {
        hasChanged = true;
        return layersManager.add(item);
    }

    public boolean removeItem(Object item) {
        hasChanged = true;
        return layersManager.remove(item);
    }

    public void clearBoard() {
        this.layersManager.clear();
    }

	public void powerOn(){
        if (!simulator.runningProperty().getValue()) {
            List<PowerSocket> powerSockets = new LinkedList<>();

            layersManager.getComponents().forEach(component -> {
                powerSockets.addAll(component.getPowerSockets());
            });

            simulator.start(powerSockets);
        }
    }

	public void powerOff(){
		simulator.stop();
	}

	public boolean isSimulationRunning(){ return simRunningProperty().getValue(); }

    public ReadOnlyBooleanProperty simRunningProperty(){
        return simulator.runningProperty();
    }

    public BoardSimulator getSimulator() {
        return this.simulator;}

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
            if (component.localToScene(component.getBoundsInLocal())
                    .intersects(device.localToScene(device.getBoundsInLocal()))) {
                //ak zoznam neexistuje, vytvor ho
                if (components == null)
                    components = new LinkedList<>();
                //ak ano, pridaj ho do zoznamu na zaciatok -> od najvrchnejsich k spodnym

				components.add(0,component);
			}
		}

        if (components != null && components.size() > 1) {
            //ak bolo najdenych viacero komponentov
            Component topMostComponent = components.get(0);
            if (topMostComponent.localToScene(topMostComponent.getBoundsInLocal())
                    .contains(device.localToScene(device.getBoundsInLocal()))) {
                //a ak zariadenie je cele nad najvyssim komponentom, vrat iba tento jeden komponent
                //to aby sa zariadenie nepripajalo skrze viacero komponentov
                components.clear();
                components.add(topMostComponent);
            }
        }

		return components;
	}

	public Socket checkForCollisionWithSocket(Component component, Pin pin){
        //board.layersManager.getComponents().get(0).getSockets().get(0).localToScene(board.layersManager.getComponents().get(0).getSockets().get(0).getBoundsInLocal())
        for (Socket socket : component.getSockets()) {
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
        //if(!isSimulationRunning()) System.out.println("pridavanie eventu aj ked simulacia nebezi");
    }

    public Point2D getMousePositionOnGrid(MouseEvent event) {
        Point2D local = layersManager.getLayer("background").sceneToLocal(event.getSceneX(), event.getSceneY());
        return gridSystem.getBox(local.getX(), local.getY(), getAppliedScale());
    }

    public boolean save(File file) {
        return SchemeLoader.save(file, layersManager);
    }

    public boolean load(File file) {
        return SchemeLoader.load(file, layersManager);
    }
}