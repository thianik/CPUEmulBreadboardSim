package sk.uniza.fri.cp.Bus;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.concurrent.Task;

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
public class BusSimple {
    //public static final int SYNCH_WAIT_TIME_MS = 500;     //cas cakania na nastavenie dat pri synchronnej komunikacii

	protected static BusSimple instance;

	protected SimpleBooleanProperty USBConnected;


	private short addressBus;
	private byte dataBus;

	//signaly
	//vystupne
	private boolean MW_;
	private boolean MR_;
	private boolean IW_;
	private boolean IR_;
	private boolean IA_;
	//vstupne
	private boolean IT;
	private boolean RY;
	private boolean BQ;
	private boolean BA;

	private CountDownLatch cdlRY;

	private Random rand;

	//TODO Bus privatny konstruktor pri zruseni simulovanej zbernice
	BusSimple(){
        MW_ = MR_ = IW_ = IR_ = IA_ = true;

		rand = new Random();

		USBConnected = new SimpleBooleanProperty(false);
	}

	public static BusSimple getBus(){
		if(instance == null){
			instance = new BusSimple();
		}

		return instance;
	}

	public boolean connectUSB(){
		int ret = k041Library.USBInitDevice();
		if(ret == 0)
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

	synchronized public void reset(){
		setRandomAddress();
		setRandomData();

		// negovane riadiace signaly sa daju do "1"
		if(USBConnected.getValue()){
			USBSetCommands(0x1f,0);
		} else {
			MW_ = MR_ = IW_ = IR_ = IA_ = true;
			IT = RY = BQ = BA = false;
		}
	}

	synchronized public short getAddressBus() {
		return addressBus;
	}

	synchronized public void setAddressBus(short addressBus) {
		if(USBConnected.getValue())
			USBSetAddress(Short.toUnsignedInt(addressBus));
		else
			this.addressBus = addressBus;
	}

	synchronized public byte getDataBus() {
		if(USBConnected.getValue()) {
			byte data = (byte) USBReadData();
			System.out.println("USBReadData: " + data);
			return data;
		}
		else
			return dataBus;
	}

	synchronized public void setDataBus(byte dataBus) {
		if(USBConnected.getValue())
			USBSetData(Byte.toUnsignedInt(dataBus));
		else
			this.dataBus = dataBus;
	}

	synchronized public void setRandomAddress(){
		if(USBConnected.getValue())
			USBSetAddress(rand.nextInt());
		else
	    	this.addressBus = (short) rand.nextInt(65535);
	}

	synchronized public void setRandomData(){
		if(USBConnected.getValue())
			USBSetData(rand.nextInt());
		else
        	this.dataBus = (byte) rand.nextInt(256);
	}

	synchronized public boolean isMW_() {
		return MW_;
	}

	synchronized public void setMW_(boolean MW_) {
		if(USBConnected.getValue())
			if(!MW_)
				USBSetCommands(0x1e,0);  // prikaz MW\ = 0
			else
				USBSetCommands(0x1f,0);  // zrusenie prikazu
		else
        	this.MW_ = MW_;
	}

    synchronized public void setMW_(boolean MW_, CountDownLatch cdlRY) {
		setMW_(MW_);
        this.cdlRY = cdlRY;

		if(USBConnected.getValue())	activeWaitForRY();
    }

	synchronized public boolean isMR_() {
		return MR_;
	}

    synchronized public void setMR_(boolean MR_) {
		if(USBConnected.getValue())
			if(!MR_)
				USBSetCommands(0x1d,2);  // prikaz MR\ = 0
			else
				USBSetCommands(0x1f,0);  // zrusenie prikazu
		else
        	this.MR_ = MR_;
    }

	synchronized public void setMR_(boolean MR_, CountDownLatch cdlRY) {
	    setMR_(MR_);
        this.cdlRY = cdlRY;

		if(USBConnected.getValue())	activeWaitForRY();
	}

	synchronized public boolean isIW_() {
		return IW_;
	}

	synchronized public void setIW_(boolean IW_) {
		if(USBConnected.getValue())
			if(!IW_)
				USBSetCommands(0x1b,0);  // prikaz IOW\ = 0
			else
				USBSetCommands(0x1f,0);  // zrusenie prikazu
		else
        	this.IW_ = IW_;
	}

    synchronized public void setIW_(boolean IW_, CountDownLatch cdlRY) {
        setIW_(IW_);
        this.cdlRY = cdlRY;

		if(USBConnected.getValue())	activeWaitForRY();
    }

	synchronized public boolean isIR_() {
		return IR_;
	}

	synchronized public void setIR_(boolean IR_) {
		if(USBConnected.getValue())
			if(!IR_)
				USBSetCommands(0x17,2);  // prikaz IOR\ = 0
			else
				USBSetCommands(0x1f,0);  // zrusenie prikazu
		else
        	this.IR_ = IR_;
	}

    synchronized public void setIR_(boolean IR_, CountDownLatch cdlRY) {
        setIR_(IR_);
        this.cdlRY = cdlRY;

		if(USBConnected.getValue())	activeWaitForRY();
    }

	synchronized public boolean isIA_() {
		return IA_;
	}

	synchronized public void setIA_(boolean IA_) {
		if(USBConnected.getValue())
			if(!IA_)
				USBSetCommands(0x0f,2);     // INTA\ = 0
			else
				USBSetCommands(0x1f,0);  // zrusenie prikazu
		else
			this.IA_ = IA_;
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
			return IT;
	}

	synchronized public void setIT(boolean IT) {
		this.IT = IT;
	}

	synchronized public boolean isRY() {
		if(USBConnected.getValue()) {
			// nacitanie aktualnej hodnoty RY
			USBReadData();
			int pom = USBReadData();
			return (pom & 0x8000) != 0;
		}
		else
			return RY;
	}

	synchronized public void setRY(boolean RY){
        this.RY = RY;

        if(cdlRY != null && RY) {
            cdlRY.countDown();
            cdlRY = null;
        }
	}


	//TODO Dorobit BQ a BA signaly na zbernici pre USB ak to je potebne
	synchronized public boolean isBQ() {
		return BQ;
	}

	synchronized public void setBQ(boolean BQ) {
		this.BQ = BQ;
	}

	synchronized public boolean isBA() {
		return BA;
	}

	synchronized public void setBA(boolean BA) {
		this.BA = BA;
	}

	private void activeWaitForRY(){
		Task waitTask = new Task<Void>() {
			@Override
			protected Void call() throws Exception {
				while(true){
					if (isCancelled() || isRY()) break;
					//cakaj na RY
				}
				if(cdlRY != null)
					cdlRY.countDown();
				cdlRY = null;
				return null;
			}
		};


		Thread waitThread = new Thread(waitTask);
		waitThread.setDaemon(true);
		waitThread.start();
	}

}//end Bus