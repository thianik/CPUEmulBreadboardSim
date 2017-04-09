package sk.uniza.fri.cp.BreadboardSim.Devices.Pin;


import sk.uniza.fri.cp.BreadboardSim.Devices.Device;

/**
 * @author Moris
 * @version 1.0
 * @created 17-mar-2017 16:16:35
 */
public class InputPin extends Pin {

	public InputPin(Device device){
		super(device);
	}

	public InputPin(Device device, String name){
		super(device, name);
	}

}