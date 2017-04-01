package sk.uniza.fri.cp.Bus;

import javafx.beans.Observable;
import javafx.beans.property.*;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import org.reactfx.Change;
import org.reactfx.EventStream;
import org.reactfx.EventStreams;
import org.reactfx.util.Tuple2;

import java.util.Random;
import java.util.concurrent.CountDownLatch;

import static sk.uniza.fri.cp.Bus.k041Library.*;

/**
 * Sprostredkuva komunikaciu medzi CPU a doskou (fyzickou / simulovanou)
 * Singleton
 *
 * @author Moris
 * @version 1.0
 * @created 07-feb-2017 18:40:27
 */
public class Bus{
    //public static final int SYNCH_WAIT_TIME_MS = 500;     //cas cakania na nastavenie dat pri synchronnej komunikacii

	protected static Bus instance;
	protected SimpleBooleanProperty USBConnected;

	private IntegerProperty addressBus;
	private IntegerProperty dataBus;
	private IntegerProperty controlBus;

	private EventStream<Change<Number>> addressBusEventStream;
	private EventStream<Change<Number>> dataBusEventStream;
	private EventStream<Change<Number>> controlBusEventStream;

	private Random rand;

	//TODO Bus privatny konstruktor pri zruseni simulovanej zbernice
	Bus(){
		this.addressBus = new SimpleIntegerProperty(0);
		this.dataBus = new SimpleIntegerProperty(0);
		this.controlBus = new SimpleIntegerProperty(0);

		rand = new Random();

		USBConnected = new SimpleBooleanProperty(false);

        initControlBus();

		addressBusEventStream = EventStreams.changesOf(addressBus);
		dataBusEventStream = EventStreams.changesOf(dataBus);
		controlBusEventStream = EventStreams.changesOf(controlBus);
	}

	public static Bus getBus(){
		if(instance == null){
			instance = new Bus();
		}

		return instance;
	}

	public boolean connectUSB(){
		int ret = k041Library.USBInitDevice();
		if(ret != 0)
			USBConnected.setValue(false);
		else
			USBConnected.setValue(true);

		System.out.println("USBInitDevice: " + ret);
		return USBConnected.getValue();
	}

	public void disconnectUSB(){
		USBConnected.setValue(false);
	}

	public boolean isUsbConnected(){
		return USBConnected.getValue();
	}

    public ReadOnlyIntegerProperty addressBusProperty(){
        return ReadOnlyIntegerProperty.readOnlyIntegerProperty(addressBus);
    }

    public ReadOnlyIntegerProperty dataBusProperty(){
        return ReadOnlyIntegerProperty.readOnlyIntegerProperty(dataBus);
    }

    public ReadOnlyIntegerProperty controlBusProperty(){
        return ReadOnlyIntegerProperty.readOnlyIntegerProperty(controlBus);
    }

    public EventStream<Change<Number>> getAddressBusEventStream(){
        return addressBusEventStream;
    }

    public EventStream<Change<Number>> getDataBusEventStream(){
        return dataBusEventStream;
    }

    public EventStream<Change<Number>> getControlBusEventStream(){
        return controlBusEventStream;
    }

	synchronized public void reset(){
		setRandomAddress();
		setRandomData();

		// negovane riadiace signaly sa daju do "1"
		if(USBConnected.getValue()){
			USBSetCommands(0x1f,0);
		} else {
			initControlBus();
		}
	}

	synchronized public short getAddressBus() {
        return addressBus.getValue().shortValue();
	}

    synchronized public byte getDataBus() {
        if(USBConnected.getValue()) {
            byte data = (byte) USBReadData();
            System.out.println("USBReadData: " + data);
            return data;
        }
        else
            return dataBus.getValue().byteValue();
    }

    public int getControlBus(){
	    return controlBus.getValue();
    }

    synchronized public void setAddressBus(short addressBus) {
        if(USBConnected.getValue())
            USBSetAddress(Short.toUnsignedInt(addressBus));
        else
            this.addressBus.setValue(Short.toUnsignedInt(addressBus));
    }

	synchronized public void setDataBus(byte dataBus) {
		if(USBConnected.getValue())
			USBSetData(Byte.toUnsignedInt(dataBus));
		else
			this.dataBus.setValue(Byte.toUnsignedInt(dataBus));
	}

	synchronized public void setRandomAddress(){
		if(USBConnected.getValue())
			USBSetAddress(rand.nextInt());
		else
	    	this.addressBus.setValue(rand.nextInt(65535));
	}

	synchronized public void setRandomData(){
		if(USBConnected.getValue())
			USBSetData(rand.nextInt());
		else
        	this.dataBus.setValue(rand.nextInt(256));
	}

    synchronized public void setMW_(boolean MW_) {
        if(USBConnected.getValue())
            if(!MW_)
                USBSetCommands(0x1e,0);  // prikaz MW\ = 0
            else
                USBSetCommands(0x1f,0);  // zrusenie prikazu
        else
            mapToControlBus("MW_", MW_);
    }

    synchronized public void setMR_(boolean MR_) {
        if(USBConnected.getValue())
            if(!MR_)
                USBSetCommands(0x1d,2);  // prikaz MR\ = 0
            else
                USBSetCommands(0x1f,0);  // zrusenie prikazu
        else {
            this.dataBus.setValue(0);
            mapToControlBus("MR_", MR_);
        }
    }

    synchronized public void setIW_(boolean IW_) {
        if(USBConnected.getValue())
            if(!IW_)
                USBSetCommands(0x1b,0);  // prikaz IOW\ = 0
            else
                USBSetCommands(0x1f,0);  // zrusenie prikazu
        else
            mapToControlBus("IW_", IW_);
    }

    synchronized public void setIR_(boolean IR_) {
        if(USBConnected.getValue())
            if(!IR_)
                USBSetCommands(0x17,2);  // prikaz IOR\ = 0
            else
                USBSetCommands(0x1f,0);  // zrusenie prikazu
        else
            mapToControlBus("IR_", IR_);
    }

    synchronized public void setIA_(boolean IA_) {
        if(USBConnected.getValue())
            if(!IA_)
                USBSetCommands(0x0f,2);     // INTA\ = 0
            else
                USBSetCommands(0x1f,0);  // zrusenie prikazu
        else
            mapToControlBus("IA_", IA_);
    }

    synchronized public void setIT(boolean IT) {
        mapToControlBus("IT", IT);
    }

    synchronized public void setRY(boolean RY){
        mapToControlBus("RY", RY);
    }

    synchronized public boolean isMW_() {
		return mapFromControlBus("MW_");
	}

	synchronized public boolean isMR_() {
		return mapFromControlBus("MR_");
	}

	synchronized public boolean isIW_() {
		return mapFromControlBus("IW_");
	}

	synchronized public boolean isIR_() {
		return mapFromControlBus("IR");
	}

	synchronized public boolean isIA_() {
		return mapFromControlBus("IA_");
	}

	synchronized public boolean isIT() {
		if(USBConnected.getValue()) {
			// nacitanie aktualnej hodnoty INT
			USBSetCommands(0x1f, 2);
			USBReadData();
			int pom = USBReadData();
			USBSetCommands(0x1f, 0);
			return (pom & 0x100) != 0;
		}
		else
		    return mapFromControlBus("IT");
	}

	synchronized public boolean isRY() {
		if(USBConnected.getValue()) {
			// nacitanie aktualnej hodnoty RY
			USBReadData();
			int pom = USBReadData();
			return (pom & 0x8000) != 0;
		}
		else
			return mapFromControlBus("RY");
	}


	private void initControlBus(){
        mapToControlBus("MW_", true);
        mapToControlBus("MR_", true);
        mapToControlBus("IW_", true);
        mapToControlBus("IR_", true);
        mapToControlBus("IA_", true);

        mapToControlBus("IT", false);
        mapToControlBus("RY", false);
        mapToControlBus("BQ", false);
        mapToControlBus("BA", false);
    }

	private void mapToControlBus(String signal, boolean value){
        int pos = mapSignal(signal);

		if(value)
			controlBus.setValue( controlBus.getValue() | ( 1<<pos ) );
		else
			controlBus.setValue( controlBus.getValue() & ~( 1<<pos ) );
	}

    private boolean mapFromControlBus(String signal){
        int pos = mapSignal(signal);

        return (controlBus.getValue() & 1<<pos ) != 0;
    }

    private int mapSignal(String signal){
        switch (signal){
            case "MW_": return 8;
            case "MR_": return 7;
            case "IW_": return 6;
            case "IR_": return 5;
            case "IA_": return 4;
            case "IT": return 3;
            case "RY": return 2;
            case "BQ": return 1;
            case "BA": return 0;
        }

        return -1;
    }

}//end Bus