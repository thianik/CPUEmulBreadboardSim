package sk.uniza.fri.cp.BreadboardSim;


import javafx.event.EventHandler;
import javafx.scene.input.MouseDragEvent;
import javafx.scene.input.MouseEvent;

import java.util.ArrayList;

/**
 * @author Moris
 * @version 1.0
 * @created 17-mar-2017 16:16:35
 */
public class SocketEvents {

	/**
	 * aktualizacia konca kablika, ak sa vytvara
	 */
	private EventHandler<MouseEvent> onMouseDragged;
	private EventHandler<MouseEvent> onMouseReleased;
	/**
	 * zvyraznenie
	 */
	private EventHandler<MouseEvent> onMouseOver;
	/**
	 * zoznam socketov, ktore boli zvyraznene (aby ich bolo mozne odvyraznit)
	 */
	private ArrayList<Socket> highlighted;
	/**
	 * zaciatok vytvarania kablika, zvyraznienie spojenych socketov
	 */
	private EventHandler<MouseEvent> onMousePressed;
	/**
	 * startFullDrag()
	 */
	private EventHandler<MouseDragEvent> onMouseDragDetected;
	/**
	 * zvyraznenie pripojenych socketov pripojenych k somuto socketu
	 */
	private EventHandler<MouseDragEvent> onMouseDragEntered;
	/**
	 * zruzenie zvyraznenia pripojenych socketov pripojenych k somuto socketu
	 */
	private EventHandler<MouseDragEvent> onMouseDragExited;

	public SocketEvents(){

	}

	public void finalize() throws Throwable {

	}

	/**
	 * 
	 * @param socket
	 */
	public void apply(Socket socket){

	}

}