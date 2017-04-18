package sk.uniza.fri.cp.BreadboardSim.Board;


import sk.uniza.fri.cp.BreadboardSim.Devices.Device;
import sk.uniza.fri.cp.BreadboardSim.Socket.Potential;
import sk.uniza.fri.cp.BreadboardSim.Socket.Socket;

import java.util.List;
import java.util.Set;

/**
 * @author Moris
 * @version 1.0
 * @created 17-mar-2017 16:16:34
 */
public class BoardEvent {
	private Socket socket;

	public BoardEvent(Socket socket){
		this.socket = socket;
	}

	public Socket getSocket(){
		return socket;
	}

    public void process(Set<Device> devices) {
        if(devices != null && socket != null){
			Potential potential = socket.getPotential();
			if(potential != null)
				potential.getDevicesWithInputs(devices);
		}
	}

}