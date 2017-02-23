package sk.uniza.fri.cp.Bus;


import sk.uniza.fri.cp.CPUEmul.CPU;

import java.util.Random;
import java.util.concurrent.CountDownLatch;

/**
 * Sprostredkuva komunikaciu medzi CPU a doskou (fyzickou / simulovanou)
 * Singleton
 *
 * @author Moris
 * @version 1.0
 * @created 07-feb-2017 18:40:27
 */
public class Bus {
    //public static final int SYNCH_WAIT_TIME_MS = 500;     //cas cakania na nastavenie dat pri synchronnej komunikacii

	protected static Bus instance;

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
	Bus(){
        MW_ = MR_ = IW_ = IR_ = IA_ = true;

		rand = new Random();
	}

	public static Bus getBus(){
		if(instance == null){
			instance = new Bus();
		}

		return instance;
	}

	synchronized public short getAddressBus() {
		return addressBus;
	}

	synchronized public void setAddressBus(short addressBus) {
		this.addressBus = addressBus;
	}

	synchronized public byte getDataBus() {
		return dataBus;
	}

	synchronized public void setDataBus(byte dataBus) {
		this.dataBus = dataBus;
	}

	synchronized public void setRandomAddress(){
	    this.addressBus = (short) rand.nextInt(65535);
	}

	synchronized public void setRandomData(){
        this.dataBus = (byte) rand.nextInt(256);
	}

	synchronized public boolean isMW_() {
		return MW_;
	}

	synchronized public void setMW_(boolean MW_) {
        this.MW_ = MW_;
	}

    synchronized public void setMW_(boolean MW_, CountDownLatch cdlRY) {
        this.MW_ = MW_;
        this.cdlRY = cdlRY;
    }

	synchronized public boolean isMR_() {
		return MR_;
	}

    synchronized public void setMR_(boolean MR_) {
        this.MR_ = MR_;
    }

	synchronized public void setMR_(boolean MR_, CountDownLatch cdlRY) {
	    this.MR_ = MR_;
        this.cdlRY = cdlRY;
	}

	synchronized public boolean isIW_() {
		return IW_;
	}

	synchronized public void setIW_(boolean IW_) {
        this.IW_ = IW_;
	}

    synchronized public void setIW_(boolean IW_, CountDownLatch cdlRY) {
        this.IW_ = IW_;
        this.cdlRY = cdlRY;
    }

	synchronized public boolean isIR_() {
		return IR_;
	}

	synchronized public void setIR_(boolean IR_) {
        this.IR_ = IR_;
	}

    synchronized public void setIR_(boolean IR_, CountDownLatch cdlRY) {
        this.IR_ = IR_;
        this.cdlRY = cdlRY;
    }

	synchronized public boolean isIA_() {
		return IA_;
	}

	synchronized public void setIA_(boolean IA_) {
		this.IA_ = IA_;
	}

	synchronized public boolean isIT() {
		return IT;
	}

	synchronized public void setIT(boolean IT) {
		this.IT = IT;
	}

	synchronized public boolean isRY() {
		return RY;
	}

	synchronized public void setRY(boolean RY){
        this.RY = RY;
        if(cdlRY != null && RY) {
            cdlRY.countDown();
            cdlRY = null;
        }
	}

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
}//end Bus