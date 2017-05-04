package sk.uniza.fri.cp.BreadboardSim.Board;


import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
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
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.jdom2.util.IteratorIterable;
import sk.uniza.fri.cp.BreadboardSim.Components.*;
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
import java.util.Map;

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

    private double SCALE_DELTA = 1.1;
    private double SCALE_TOTAL = 1;

    public double getAppliedScale() {
        return SCALE_TOTAL;
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

		this.cursorPosition = new SimpleObjectProperty<>();


        this.setPannable(false);

		gridBackground.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> clearSelect());

        // Let the ScrollPane.viewRect only pan on middle button.
        //znefunkcni pridavanie objektov
//        this.getContent().addEventHandler(MouseEvent.ANY, event -> {
//            if (event.getButton() != MouseButton.MIDDLE) event.consume();
//        });

		//sandbox
		SchoolBreadboard sb = SchoolBreadboard.getSchoolBreadboard(this);
		this.addItem(sb);
        sb.moveTo((int) (width / gridSizePx / 2 - sb.getGridWidth() / 2), (int) (height / gridSizePx / 2 - sb.getGridHeight() / 2));

        this.hasChanged = false;

//        this.setFitToWidth(true);
//        this.setFitToHeight(true);

        //ZOOM
//        this.addEventFilter(ScrollEvent.ANY, e -> {
//            e.consume();
//            Node content = this.getContent();
//
//            if (e.getDeltaY() == 0) {
//                return;
//            }
//            double scaleFactor
//                    = (e.getDeltaY() > 0)
//                    ? SCALE_DELTA
//                    : 1 / SCALE_DELTA;
//
//            if (scaleFactor * SCALE_TOTAL >= 0.5) {
//                content.setScaleX(this.getContent().getScaleX() * scaleFactor);
//                content.setScaleY(this.getContent().getScaleY() * scaleFactor);
//                SCALE_TOTAL *= scaleFactor;
//
//            }
//        });

//


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

            // amount of scrolling in each direction in scrollContent coordinate
            // units
            Point2D scrollOffset = figureScrollOffset(scrollContent, this);

            contentGroup.setScaleX(contentGroup.getScaleX() * scaleFactor);
            contentGroup.setScaleY(contentGroup.getScaleY() * scaleFactor);

            SCALE_TOTAL *= scaleFactor;

            // move viewport so that old center remains in the center after the
            // scaling
            repositionScroller(scrollContent, this, scaleFactor, scrollOffset);
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
        );


        this.addEventFilter(MouseDragEvent.MOUSE_DRAG_OVER, event -> {
            if(addingItem != null){
                    Point2D point = this.getContent().sceneToLocal(event.getSceneX(), event.getSceneY());
                    Event.fireEvent(addingItem, new MouseEvent(MouseEvent.MOUSE_DRAGGED, point.getX() * SCALE_TOTAL, point.getY() * SCALE_TOTAL,
                            point.getX(), point.getY(), MouseButton.PRIMARY, 1, true,
                            true, true, true, true, true,
							true, true, true, true, null));
				}
			}
        );

        this.addEventHandler(MouseDragEvent.MOUSE_DRAG_RELEASED, event -> {
            if(addingItem != null){
                    Point2D point = this.getContent().sceneToLocal(event.getSceneX(), event.getSceneY());
                Event.fireEvent(addingItem, new MouseEvent(MouseEvent.MOUSE_RELEASED, event.getX(), event.getY(),
							event.getScreenX(), event.getScreenY(), MouseButton.PRIMARY, 1, true,
							true, true, true, true, true,
							true, true, true, true, null));
					addingItem = null;
				}
			}
        );

		this.addEventFilter(MouseDragEvent.MOUSE_DRAG_EXITED, new EventHandler<MouseDragEvent>() {
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


    //PERZISTENCIA DAT

    private static final String COMPONENTS_PACKAGE = Component.class.getPackage().getName() + ".";
    private static final String DEVICES_PACKAGE = Device.class.getPackage().getName() + ".";

    public boolean save(File schx) {

        Document jdomDoc = new Document();
        Element rootElement = new Element("Board");
        jdomDoc.setRootElement(rootElement);

        Element gridX;
        Element gridY;

        //vyvojova doska
        SchoolBreadboard schoolBreadboardInstance = SchoolBreadboard.getSchoolBreadboard(this);

        Element schoolBreadboard = new Element("SchoolBreadboard");
        gridX = new Element("gridX");
        gridX.addContent(Integer.toString(schoolBreadboardInstance.getGridPosX()));

        gridY = new Element("gridY");
        gridY.addContent(Integer.toString(schoolBreadboardInstance.getGridPosY()));

        schoolBreadboard.addContent(gridX);
        schoolBreadboard.addContent(gridY);

        rootElement.addContent(schoolBreadboard);


        //komponenty
        Element componentsElement = new Element("Components");

        //ziskanie komponentov na ploche
        List<Component> componentsList = layersManager.getComponents();
        //odstranenie komponentov vyvojovej dosky zo zoznamu
        componentsList.removeAll(schoolBreadboardInstance.getComponents());
        //vytvaranie elementov pre kazdy zvisny komponent
        for (Component component : componentsList) {
            //element pre komponent
            Element componentElement = new Element("Component");
            //atribut ID
            componentElement.setAttribute("id", component.getId());

            //nazov triedy
            Element className = new Element("class");
            className.addContent(component.getClass().getName().replace(COMPONENTS_PACKAGE, ""));
            componentElement.addContent(className);

            //pozicia X na gride
            gridX = new Element("gridX");
            gridX.addContent(Integer.toString(component.getGridPosX()));
            componentElement.addContent(gridX);

            //pozicia Y na gride
            gridY = new Element("gridY");
            gridY.addContent(Integer.toString(component.getGridPosY()));
            componentElement.addContent(gridY);

            //zaradenie pod element Components
            componentsElement.addContent(componentElement);
        }
        //pridanie elemenut Components pod komponents Board
        rootElement.addContent(componentsElement);


        //zariadenia
        Element devicesElement = new Element("Devices");

        List<Device> devicesList = layersManager.getDevices();
        //pre kazde zariadenie na ploche
        for (Device device : devicesList) {
            //element pre zariadenie
            Element deviceElement = new Element("Device");

            //nazov triedy
            Element className = new Element("class");
            className.addContent(device.getClass().getName().replace(DEVICES_PACKAGE, ""));
            deviceElement.addContent(className);

            //pozicia X na gride
            gridX = new Element("gridX");
            gridX.addContent(Integer.toString(device.getGridPosX()));
            deviceElement.addContent(gridX);

            //pozicia Y na gride
            gridY = new Element("gridY");
            gridY.addContent(Integer.toString(device.getGridPosY()));
            deviceElement.addContent(gridY);

            //element pre piny zariadenia
            Element pinsElement = new Element("pins");
            if (device.isConnected()) {
                List<Pin> pinsList = device.getPins();
                for (int pinIndex = 0; pinIndex < pinsList.size(); pinIndex++) {
                    //element pre pin
                    Element pinElement = new Element("pin");
                    //atribut cislo pinu
                    pinElement.setAttribute("number", Integer.toString(pinIndex + 1));

                    //soket
                    Socket connectedSocket = pinsList.get(pinIndex).getSocket();

                    //id komponentu ku ktoremu je pripojeny
                    Element connectedComponentId = new Element("componentId");
                    connectedComponentId.addContent(connectedSocket.getComponent().getId());
                    pinElement.addContent(connectedComponentId);

                    //id soketu ku ktoremu je pripojeny
                    Element connectedSocketId = new Element("socketId");
                    connectedSocketId.addContent(connectedSocket.getId());
                    pinElement.addContent(connectedSocketId);

                    pinsElement.addContent(pinElement);
                }
            }
            deviceElement.addContent(pinsElement);

            devicesElement.addContent(deviceElement);
        }
        rootElement.addContent(devicesElement);


        //kabliky
        Element wiresElement = new Element("Wires");

        List<Wire> wiresList = layersManager.getWires();
        //pre kazy kablik
        for (Wire wire : wiresList) {
            Element wireElement = new Element("Wire");
            //atribut s farbou
            wireElement.setAttribute("color", wire.getColor().toString());

            //konce kablika
            WireEnd[] ends = wire.getEnds();

            //zaciatocny soket
            Socket startSocket = ends[0].getSocket();
            //ak je pripojeny zaciatok kablika k soketu
            if (startSocket != null) {
                Element startElement = new Element("start");

                Element connectedComponentId = new Element("componentId");
                connectedComponentId.addContent(startSocket.getComponent().getId());
                startElement.addContent(connectedComponentId);

                Element connectedSocketId = new Element("socketId");
                connectedSocketId.addContent(startSocket.getId());
                startElement.addContent(connectedSocketId);

                wireElement.addContent(startElement);
            } else {
                //ak koniec nie je pripojeny, uloz jeho poziciu
                Element startElement = new Element("start");

                //x-ova pozicia na gride
                gridX = new Element("gridX");
                gridX.addContent(Integer.toString(ends[0].getGridPosX()));
                startElement.addContent(gridX);

                //y-ova pozicia na gride
                gridY = new Element("gridY");
                gridY.addContent(Integer.toString(ends[0].getGridPosY()));
                startElement.addContent(gridY);

                wireElement.addContent(startElement);
            }

            //koncovy soket
            Socket endSocket = ends[1].getSocket();
            //ak je pripojeny zaciatok kablika k soketu
            if (endSocket != null) {
                Element endElement = new Element("end");

                Element connectedComponentId = new Element("componentId");
                connectedComponentId.addContent(endSocket.getComponent().getId());
                endElement.addContent(connectedComponentId);

                Element connectedSocketId = new Element("socketId");
                connectedSocketId.addContent(endSocket.getId());
                endElement.addContent(connectedSocketId);

                wireElement.addContent(endElement);
            } else {
                //ak koniec nie je pripojeny, uloz jeho poziciu
                Element endElement = new Element("end");

                //x-ova pozicia na gride
                gridX = new Element("gridX");
                gridX.addContent(Integer.toString(ends[1].getGridPosX()));
                endElement.addContent(gridX);

                //y-ova pozicia na gride
                gridY = new Element("gridY");
                gridY.addContent(Integer.toString(ends[1].getGridPosY()));
                endElement.addContent(gridY);

                wireElement.addContent(endElement);
            }

            //jointy
            List<Joint> jointsList = wire.getJoints();
            Element jointsElement = new Element("Joints");
            if (jointsList.size() > 0) {

                for (Joint joint : jointsList) {
                    //element pre joint
                    Element jointElement = new Element("joint");

                    //x-ova pozicia na gride
                    gridX = new Element("gridX");
                    gridX.addContent(Integer.toString(joint.getGridPosX()));
                    jointElement.addContent(gridX);

                    //y-ova pozicia na gride
                    gridY = new Element("gridY");
                    gridY.addContent(Integer.toString(joint.getGridPosY()));
                    jointElement.addContent(gridY);

                    jointsElement.addContent(jointElement);
                }

            }
            wireElement.addContent(jointsElement);

            wiresElement.addContent(wireElement);
        }
        rootElement.addContent(wiresElement);


        XMLOutputter xmlOutputter = new XMLOutputter();
        //xmlOutputter.setFormat(Format.getPrettyFormat());

        try {
            xmlOutputter.output(jdomDoc, new FileWriter(schx));
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

    }

    public boolean load(File schx) {
        if (schx.getName().substring(schx.getName().lastIndexOf(".")).equalsIgnoreCase(".sch"))
            return loadSch(schx);

        SAXBuilder builder = new SAXBuilder();

        try {
            Document jdomDoc = (Document) builder.build(schx);
            Element rootElement = jdomDoc.getRootElement();

            int gridX;
            int gridY;

            //vycistenie plochy
            this.layersManager.clear();

            //posunutie schoolBreadboard na miesto kde bola ulozena
            Element schoolBreadboardElement = rootElement.getChild("SchoolBreadboard");

            gridX = Integer.parseInt(schoolBreadboardElement.getChildText("gridX"));
            gridY = Integer.parseInt(schoolBreadboardElement.getChildText("gridY"));
            SchoolBreadboard.getSchoolBreadboard(this).moveTo(gridX, gridY);

            //nacitanie komponentov

            Element componentsElement = rootElement.getChild("Components");
            List<Element> componentsElementsList = componentsElement.getChildren("Component");
            //prechadzanie jednotlivych elementov komponentov
            for (Element componentElement : componentsElementsList) {
                String className = COMPONENTS_PACKAGE + componentElement.getChild("class").getContent(0).getValue();
                Component component = (Component) Class.forName(className).getConstructor(Board.class).newInstance(this);
                component.setId(componentElement.getAttribute("id").getValue());
                this.layersManager.add(component);

                gridX = Integer.parseInt(componentElement.getChildText("gridX"));
                gridY = Integer.parseInt(componentElement.getChildText("gridY"));

                component.moveTo(gridX, gridY);
            }


            //nacitanie zariadeni
            List<Component> componentsOnBoard = layersManager.getComponents(); //pre pripajanie pinov k soketom

            Element devicesElement = rootElement.getChild("Devices");
            List<Element> devicesElementsList = devicesElement.getChildren("Device");
            for (Element deviceElement : devicesElementsList) {
                String className = DEVICES_PACKAGE + deviceElement.getChild("class").getContent(0).getValue();
                Device device = (Device) Class.forName(className).getConstructor(Board.class).newInstance(this);
                this.layersManager.add(device);

                //nastavenie pozicie
                gridX = Integer.parseInt(deviceElement.getChildText("gridX"));
                gridY = Integer.parseInt(deviceElement.getChildText("gridY"));
                device.moveTo(gridX, gridY);

                //pripojenie pinov k soketom
                Element pinsElement = deviceElement.getChild("pins");
                List<Element> pinElementstList = pinsElement.getChildren("pin");
                for (Element pinElement : pinElementstList) {
                    int pinNumber = Integer.parseInt(pinElement.getAttributeValue("number"));
                    String componentId = pinElement.getChildText("componentId");
                    int socketId = Integer.parseInt(pinElement.getChildText("socketId")); //id je jeho index

                    //hladanie komponentu ku ktoremu sa ma pripojit (klasicky foreach, nie je predpoklad velkeho mnozstva komponentov)
                    for (Component component : componentsOnBoard) {
                        if (component.getId().equalsIgnoreCase(componentId)) {
                            //pripojenie pinu zariadenia k soketu na komponente
                            component.getSocket(socketId).connect(
                                    device.getPins().get(pinNumber - 1));//cislo pinu -> index pinu

                            //priradenie zariadenia ku komponentu podla prveho pinu
                            if (pinNumber == 1)
                                component.addDevice(device);
                            break;
                        }
                    }
                }
            }


            //nacitanie kablikov
            Element wiresElement = rootElement.getChild("Wires");
            List<Element> wiresElementsList = wiresElement.getChildren("Wire");
            for (Element wireElement : wiresElementsList) {
                //vytvorenie kablika, konce nie su nikam pripojene
                Wire wire = new Wire(this);
                this.layersManager.add(wire);
                WireEnd[] wireEnds = wire.getEnds();

                //nastavenie farby
                Color wireColor = Color.valueOf(wireElement.getAttributeValue("color"));
                wire.changeColor(wireColor);

                //zaciatok kablika
                Element startElement = wireElement.getChild("start");
                //ak je pripojeny k soketu
                String componentId = startElement.getChildText("componentId");
                if (componentId != null) {
                    //zaciatok je pripojeny k soketu
                    int socketId = Integer.parseInt(startElement.getChildText("socketId"));

                    for (Component component : componentsOnBoard) {
                        if (component.getId().equalsIgnoreCase(componentId)) {
                            wireEnds[0].connectSocket(component.getSocket(socketId));
                            break;
                        }
                    }

                } else {
                    //zaciatok nie je pripojeny k soketu, ma iba poziciu
                    gridX = Integer.parseInt(startElement.getChildText("gridX"));
                    gridY = Integer.parseInt(startElement.getChildText("gridY"));
                    wireEnds[0].moveTo(gridX, gridY);
                }


                //koniec kablika
                Element endElement = wireElement.getChild("end");
                //ak je pripojeny k soketu
                componentId = endElement.getChildText("componentId");
                if (componentId != null) {
                    //zaciatok je pripojeny k soketu
                    int socketId = Integer.parseInt(endElement.getChildText("socketId"));

                    for (Component component : componentsOnBoard) {
                        if (component.getId().equalsIgnoreCase(componentId)) {
                            wireEnds[1].connectSocket(component.getSocket(socketId));
                            break;
                        }
                    }

                } else {
                    //zaciatok nie je pripojeny k soketu, ma iba poziciu
                    gridX = Integer.parseInt(endElement.getChildText("gridX"));
                    gridY = Integer.parseInt(endElement.getChildText("gridY"));
                    wireEnds[1].moveTo(gridX, gridY);
                }


                //jointy
                Element jointsElement = wireElement.getChild("Joints");
                List<Element> jointsElementsList = jointsElement.getChildren("joint");
                for (Element jointElement : jointsElementsList) {
                    gridX = Integer.parseInt(jointElement.getChildText("gridX"));
                    gridY = Integer.parseInt(jointElement.getChildText("gridY"));
                    wire.splitLastSegment().moveTo(gridX, gridY);
                }
            }

        } catch (Exception e ) {
            e.printStackTrace();
            return false;
        }

        return true;
//         catch (JDOMException e) {
//            e.printStackTrace();
//        } catch (IOException e) {
//            e.printStackTrace();
//        } catch (InstantiationException e) {
//            e.printStackTrace();
//        } catch (InvocationTargetException e) {
//            e.printStackTrace();
//        } catch (NoSuchMethodException e) {
//            e.printStackTrace();
//        } catch (IllegalAccessException e) {
//            e.printStackTrace();
//        } catch (ClassNotFoundException e) {
//            e.printStackTrace();
//        }

    }

    public boolean loadSch(File sch) {

        try (BufferedReader br = new BufferedReader(new FileReader(sch))) {
            //vycistenie plochy
            this.layersManager.clear();

            //skolska doska
            SchoolBreadboard schoolBreadboard = this.layersManager.getSchoolBreadboards().get(0);

            //nacitanie IC
            int chipCount = Integer.parseInt(br.readLine());

            for (int i = 0; i < chipCount; i++) {
                //kazdy IC ma umiestnenie a typ
                //umiestnenie
                int chipPosition = Integer.parseInt(br.readLine());
                //typ
                int chipType = Integer.parseInt(br.readLine());

                Chip chip = null;
                switch (chipType) {
                    case 0:
                        chip = new Gen7400(this);
                        break;
                    case 1:
                        chip = new Gen7402(this);
                        break;
                    case 2:
                        chip = new Gen7404(this);
                        break;
                    case 3:
                        chip = new Gen7408(this);
                        break;
                    case 4:
                        chip = new Gen7410(this);
                        break;
                    case 5:
                        chip = new Gen7430(this);
                        break;
                    case 6:
                        chip = new Gen7432(this);
                        break;
                    case 7:
                        chip = new Gen7486(this);
                        break;
                    case 8:
                        chip = new SN74125(this);
                        break;
                    case 9:
                        chip = new SN74138(this);
                        break;
                    case 10:
                        chip = new SN74151(this);
                        break;
                    //case 11: chip = new SN74153(this); break;
                    //case 12: chip = new SN74164(this); break;
                    case 13:
                        chip = new SN74148(this);
                        break;
                    case 14:
                        chip = new SN74573(this);
                        break;
                    case 15:
                        chip = new U6264B(this, 3);
                        break;
                    default:
                        return false;
                }

                this.layersManager.add(chip);

                int gridX = schoolBreadboard.getGridPosX() + 4 + chipPosition;
                int gridY = schoolBreadboard.getGridPosY() + 15;

                chip.moveTo(gridX, gridY);

                chip.searchForSockets();
                chip.tryToConnectToFoundSockets();
            }

            //nacitanie kablikov
            //pocet kablikov
            int wiresCount = Integer.parseInt(br.readLine());
            for (int i = 0; i < wiresCount; i++) {
                int startX = Integer.parseInt(br.readLine());
                int startY = Integer.parseInt(br.readLine());
                int endX = Integer.parseInt(br.readLine());
                int endY = Integer.parseInt(br.readLine());
                String colorRGB = br.readLine();

                //Color wireColor = Color.valueOf(colorRGB); //TODO farba kablika

                //vytvorenie kablika, konce nie su nikam pripojene
                Wire wire = new Wire(this);
                this.layersManager.add(wire);
                WireEnd[] wireEnds = wire.getEnds();

                Socket startSocket = getSocketAtPosOnFirstSchoolBreadboard(startX, startY);
                Socket endSocket = getSocketAtPosOnFirstSchoolBreadboard(endX, endY);

                if (startSocket == null || endSocket == null) return false;

                wireEnds[0].connectSocket(startSocket);
                wireEnds[1].connectSocket(endSocket);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return false;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }

    private Socket getSocketAtPosOnFirstSchoolBreadboard(int posX, int posY) {
        SchoolBreadboard schoolBreadboard = this.layersManager.getSchoolBreadboards().get(0);
        int index = 0;

        if (posY == 1) {
            //zbernica
            if (posX == 6 || posX == 7) {
                //prvy gnd (33 a 34)
                index = posX + 27;
            } else if (posX >= 14 && posX <= 29) {
                //adresna (15 <-> 0)
                index = Math.abs(posX - 29);
            } else if (posX >= 35 && posX <= 42) {
                //datova (23 <-> 16)
                index = Math.abs(posX - 58);
            } else if (posX >= 44 && posX <= 52) {
                //riadiaca (32 <-> 24)
                index = Math.abs(posX - 76);
            } else if (posX == 55 || posX == 56) {
                //druhy gnd (35 a 36)
                index = posX - 20;
            } else {
                return null;
            }

            return schoolBreadboard.getBusInterface().getSocket(index);

        } else if (posY >= 3 && posY <= 22) {
            //breadboard
            if (posY == 3) {
                //horna modra lajna
                int positionInLine = posX - 2;
                int spaces = positionInLine / 6;
                index = positionInLine - spaces;
            } else if (posY == 4) {
                //horna cervena lajna
                int positionInLine = posX - 2;
                int spaces = positionInLine / 6;
                index = 50 + positionInLine - spaces;
            } else if (posY >= 7 && posY <= 11) {
                //horne pole
                index = 100 + 5 * posX + posY - 7;
            } else if (posY >= 14 && posY <= 18) {
                //dolne pole
                index = 415 + 5 * posX + posY - 14;
            } else if (posY == 21) {
                //dolna modra lajna
                int positionInLine = posX - 2;
                int spaces = positionInLine / 6;
                index = 730 + positionInLine - spaces;
            } else if (posY == 22) {
                //dolna modra lajna
                int positionInLine = posX - 2;
                int spaces = positionInLine / 6;
                index = 780 + positionInLine - spaces;
            } else {
                return null;
            }

            return schoolBreadboard.getBreadboard().getSocket(index);
        } else if (posY >= 24 && posY <= 32) {
            //spodna cast vyvojovej dosky
            if (posX >= 0 && posX <= 16) {
                //7seg panel
                int segmentIndex = 0;

                if (posX <= 12) {
                    //zapojenie na segmenty
                    switch (posX) {
                        case 0:
                            segmentIndex = 0;
                            break;
                        case 4:
                            segmentIndex = 1;
                            break;
                        case 8:
                            segmentIndex = 2;
                            break;
                        case 12:
                            segmentIndex = 3;
                            break;
                    }

                    index = posY - 25;

                    return ((HexSegmentsPanel) schoolBreadboard.getHexSegmentsPanel()).getSegment(segmentIndex).getSocket(index);

                } else if (posX == 15) {
                    //stlpec s vcc a gnd
                    if (posY == 25) {
                        //prvy bod vcc
                        return ((HexSegmentsPanel) schoolBreadboard.getHexSegmentsPanel()).getSocket(0);
                    } else {
                        //gnd
                        index = 2 - 28 + posY;
                        return ((HexSegmentsPanel) schoolBreadboard.getHexSegmentsPanel()).getSocket(index);
                    }
                } else if (posX == 16) {
                    //stlpec s VCC a A-ckami
                    if (posY == 25) {
                        //druhy bod vcc
                        return ((HexSegmentsPanel) schoolBreadboard.getHexSegmentsPanel()).getSocket(1);
                    } else {
                        //zapojenie A-cok
                        segmentIndex = posY - 28;

                        return ((HexSegmentsPanel) schoolBreadboard.getHexSegmentsPanel()).getSegment(segmentIndex).getSocket(8);
                    }
                } else return null;
            } else if (posX == 18) {
                //probe
                return schoolBreadboard.getProbe().getSocket(0);
            } else if (posX >= 46 && posX <= 58) {
                //hw kalvesnica
                if (posY == 24) {
                    //gnd
                    return schoolBreadboard.getNumKeys().getSocket(posX - 55);
                } else if (posY == 25) {
                    //column vcc row

                    if (posX <= 49) {
                        //column
                        return schoolBreadboard.getNumKeys().getSocket(6 + posX - 46);
                    } else if (posX == 52 || posX == 53) {
                        //vcc
                        return schoolBreadboard.getNumKeys().getSocket(4 + posX - 52);
                    } else if (posX >= 55) {
                        //row 10 11 12 13
                        return schoolBreadboard.getNumKeys().getSocket(10 + posX - 55);
                    }
                }
            }
        }

        return null;
    }
}