package sk.uniza.fri.cp.BreadboardSim.Board;

import sk.uniza.fri.cp.BreadboardSim.Devices.Device;
import sk.uniza.fri.cp.BreadboardSim.Socket.Potential;
import sk.uniza.fri.cp.BreadboardSim.Socket.Socket;

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

				//ak nenastal skrat, ak by nastal a boli by pripojene zariadenia ktore ho vyvolali, zacal by sa cyklit
                if(socket.setPotential(newValue))
                    //a ak sa hodnota zmenila
                    if (oldValue != socket.getPotential().getValue())
                        //vrat zariadenia s ovplyvnenymi vstupmi
	    				potential.getDevicesWithInputs(devices);
			}
		}
	}

}