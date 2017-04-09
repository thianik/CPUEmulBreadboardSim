package sk.uniza.fri.cp.BreadboardSim.Devices.Pin;


import sk.uniza.fri.cp.BreadboardSim.Devices.Device;

/**
 * @author Moris
 * @version 1.0
 * @created 17-mar-2017 16:16:35
 */
public class OutputPin extends Pin {

	private PinDriver driver;

	public OutputPin(Device device){
		super(device);
		this.driver = PinDriver.PUSH_PULL;
	}

	public OutputPin(Device device, String name){
		super(device, name);
		this.driver = PinDriver.PUSH_PULL;
	}

	public OutputPin(Device device, PinDriver pinDriver){
		super(device);
		this.driver = pinDriver;
	}

    public OutputPin(Device device, PinDriver pinDriver, String name) {
        super(device, name);
        this.driver = pinDriver;
    }

	public PinDriver getPinDriver(){
		return driver;
	}
}