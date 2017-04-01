package sk.uniza.fri.cp.BreadboardSim;

import javafx.scene.Group;
import javafx.scene.Node;
import sk.uniza.fri.cp.BreadboardSim.Components.Component;

import java.util.List;

/**
 * iba staticka treida na vytvorenie groupy? faktorka?
 * @author Moris
 * @version 1.0
 * @created 17-mar-2017 16:16:36
 */
public class SocketsFactory {

	/**
	 * 
	 * @param component
	 * @param startId
	 * @param count
	 */
	public static Group getHorizontal(Component component, int startId, int count, List<Socket> collection){
		Group sockets = new Group();
		GridSystem grid = component.getBoard().getGrid();
		int gridSpaceX = grid.getSizeX();

		for (int i = 0; i < count; i++) {
			Socket socket = new Socket(component, startId+i);
			socket.setLayoutX(i*gridSpaceX);

			//spojenie socketov pomocou potencialu
			if(sockets.getChildren().size() > 0){
				new Potential((Socket) sockets.getChildren().get(0), socket);
			}

			sockets.getChildren().add(socket);
			collection.add(socket);
		}

		return sockets;
	}

	/**
	 * 
	 * @param component
	 * @param startId
	 * @param count
	 * @param numInStack
	 * @param spaceBetweenStack
	 */
	public static Group getHorizontal(Component component, int startId, int count, int numInStack, int spaceBetweenStack, List<Socket> collection){
		Group sockets = new Group();
		GridSystem grid = component.getBoard().getGrid();
		int gridSpaceX = grid.getSizeX();

		int posX = 0;
		for (int i = 0; i < count; i++) {
			Socket socket = new Socket(component, startId+i);
			socket.setLayoutX(posX*gridSpaceX);

			//spojenie socketov pomocou potencialu
			if(sockets.getChildren().size() > 0){
				new Potential((Socket) sockets.getChildren().get(0), socket);
			}

			sockets.getChildren().add(socket);
			collection.add(socket);

			if((i+1) % numInStack == 0)
				posX += spaceBetweenStack + 1;
			else
				posX++;
		}

		return sockets;
	}

	/**
	 * 
	 * @param component
	 * @param startId
	 * @param count
	 */
	public static Group getVertical(Component component, int startId, int count, List<Socket> collection){
		Group sockets = new Group();
		GridSystem grid = component.getBoard().getGrid();
		int gridSpaceY = grid.getSizeY();

		for (int i = 0; i < count; i++) {
			Socket socket = new Socket(component, startId+i);
			socket.setLayoutY(i*gridSpaceY);

			//spojenie socketov pomocou potencialu
			if(sockets.getChildren().size() > 0){
				new Potential((Socket) sockets.getChildren().get(0), socket);
			}

			sockets.getChildren().add(socket);
			collection.add(socket);
		}

		return sockets;
	}

	/**
	 *
	 * @param component
	 * @param startId
	 * @param count
	 * @param numInStack
	 * @param spaceBetweenStack
	 */
	public static Group getVertical(Component component, int startId, int count, int numInStack, int spaceBetweenStack, List<Socket> collection){
		Group sockets = new Group();
		GridSystem grid = component.getBoard().getGrid();
		int gridSpaceY = grid.getSizeY();

		int posX = 0;
		for (int i = 0; i < count; i++) {
			Socket socket = new Socket(component, startId+i);
			socket.setLayoutY(posX*gridSpaceY);

			//spojenie socketov pomocou potencialu
			if(sockets.getChildren().size() > 0){
				new Potential((Socket) sockets.getChildren().get(0), socket);
			}

			sockets.getChildren().add(socket);
			collection.add(socket);

			if((i+1) % numInStack == 0)
				posX += spaceBetweenStack + 1;
			else
				posX++;
		}

		return sockets;
	}

	public static Group getHorizontalPower(Component component, int startId, int count, Potential.Value pwrValue, List<PowerSocket> collection){
		Group sockets = new Group();
		GridSystem grid = component.getBoard().getGrid();
		int gridSpaceX = grid.getSizeX();

		for (int i = 0; i < count; i++) {
			PowerSocket socket = new PowerSocket(component, startId+i, pwrValue);
			socket.setLayoutX(i*gridSpaceX);
			sockets.getChildren().add(socket);
			collection.add(socket);
		}

		return sockets;
	}

	public static Group getVerticalPower(Component component, int startId, int count, Potential.Value pwrValue, List<PowerSocket> collection){
		Group sockets = new Group();
		GridSystem grid = component.getBoard().getGrid();
		int gridSpaceY = grid.getSizeY();

		for (int i = 0; i < count; i++) {
			PowerSocket socket = new PowerSocket(component, startId+i, pwrValue);
			socket.setLayoutY(i*gridSpaceY);
			sockets.getChildren().add(socket);
			collection.add(socket);
		}

		return sockets;
	}

}