package sk.uniza.fri.cp.BreadboardSim.Board;


import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.*;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import org.jdom2.*;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.jdom2.util.IteratorIterable;
import sk.uniza.fri.cp.BreadboardSim.Components.*;
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

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
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
        //this.addItem(new Gen7400(this));
        //this.addItem(new Gen7400(this));
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

    public void clearBoard() {
        this.layersManager.clear();
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
	}


    //PERZISTENCIA DAT

    private static final String COMPONENTS_PACKAGE = Component.class.getPackage().getName() + ".";
    private static final String DEVICES_PACKAGE = Device.class.getPackage().getName() + ".";

    public void save() {

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
        xmlOutputter.setFormat(Format.getPrettyFormat());

        try {
            xmlOutputter.output(jdomDoc, new FileWriter("test.xml"));
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void load() {
        SAXBuilder builder = new SAXBuilder();
        File xml = new File("test.xml");

        try {
            Document jdomDoc = (Document) builder.build(xml);
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


        } catch (JDOMException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

    }

}