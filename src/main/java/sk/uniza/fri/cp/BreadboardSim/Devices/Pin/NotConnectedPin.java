package sk.uniza.fri.cp.BreadboardSim.Devices.Pin;

import sk.uniza.fri.cp.BreadboardSim.Devices.Device;

/**
 * Created by Moris on 24.3.2017.
 */
public class NotConnectedPin extends Pin {

    public NotConnectedPin(Device device) {
        super(device);
    }

    @Override
    public String getName() {
        return "NC";
    }
}
