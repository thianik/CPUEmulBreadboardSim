package sk.uniza.fri.cp.BreadboardSim;


import sk.uniza.fri.cp.BreadboardSim.Devices.Device;

import java.util.List;

/**
 * @author Moris
 * @version 1.0
 * @created 17-mar-2017 16:16:34
 */
public class BoardChangeEvent extends BoardEvent {

	private Potential.Value newValue;

	public BoardChangeEvent(Socket socket, Potential.Value newValue){
		super(socket);
		this.newValue = newValue;
	}

	public Potential.Value getValue(){
		return newValue;
	}

	@Override
	public void process(List<Device> devices){
		Socket socket = getSocket();
		if(devices != null && socket != null){
			Potential potential = socket.getPotential();

			if(potential != null) {
				Potential.Value oldValue = potential.getValue();
				socket.setPotential(newValue);

				if(oldValue != newValue)
					potential.getDevicesWithInputs(devices);
			}
		}
	}

}