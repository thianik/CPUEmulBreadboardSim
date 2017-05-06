package sk.uniza.fri.cp.BreadboardSim.Board;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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
public class BoardChangeEvent extends BoardEvent {
    public static final Logger LOGGER = LogManager.getLogger("MainLogger");
    public static final Logger QUEUELOGGER = LogManager.getLogger("QueueLogger");

	private Potential.Value newValue;

	public BoardChangeEvent(Socket socket, Potential.Value newValue){
		super(socket);
		this.newValue = newValue;
	}

	public Potential.Value getValue(){
		return newValue;
	}

	@Override
    public void process(Set<Device> devices) {
//	    String msg = "[SPRACOVAVAM] soket: " + getSocket() + " na komponente: " + getSocket().getComponent() +
//                (getSocket().getDevice()!=null?(" zariadenie: " + getSocket().getDevice() + " a pin: " + getSocket().getPin().getName()):"") + " na hodnotu: " + getValue();
//	    LOGGER.debug(msg);
//        QUEUELOGGER.debug(msg);

        Socket socket = getSocket();
		if(devices != null && socket != null){
			Potential potential = socket.getPotential();

			if(potential != null) {
				Potential.Value oldValue = potential.getValue();

                //ak nenastal skrat, ak by nastal a boli by pripojene zariadenia ktore ho vyvolali, zacal by sa cyklit
                if(socket.setPotential(newValue))
                    //socket.setPotential(newValue);
                    //a ak sa hodnota zmenila
                    if (oldValue != socket.getPotential().getValue())
                        //vrat zariadenia s ovplyvnenymi vstupmi
                        potential.getDevicesWithInputs(devices);
            }
		}
	}

}