package sk.uniza.fri.cp.BreadboardSim;


import sk.uniza.fri.cp.BreadboardSim.Devices.Device;

import java.util.List;

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

	public void process(List<Device> devices){
		if(devices != null && socket != null){
			Potential potential = socket.getPotential();
			if(potential != null)
				potential.getDevicesWithInputs(devices);
		}
	}

}