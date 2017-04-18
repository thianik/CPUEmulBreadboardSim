package sk.uniza.fri.cp.Bus;

import javafx.beans.property.*;

import java.util.Random;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import static sk.uniza.fri.cp.Bus.k041Library.*;

/**
 * Zbernica zabezpečujúca komunikáciu medzi CPU a doskou (fyzickou / simulovanou).
 * Uchováva aktuálne hodnoty na adresnej, dátovej a riadiacej zbernici.
 * Singleton
 *
 * @author Tomáš Hianik
 * @version 1.0
 * @created 07-feb-2017 18:40:27
 */
public class Bus{

	private static Bus instance; //inštancia singletonu
	private SimpleBooleanProperty USBConnected; //indikátor pripojenia vývojovej dosky cez USB
    private Semaphore dataSemaphore; //semafor pre cakanie na nastavenie dat simulatorom

	private IntegerProperty addressBus;
	private IntegerProperty dataBus;
	private IntegerProperty controlBus;

	private Random rand;

	private Bus(){
		this.addressBus = new SimpleIntegerProperty(0);
		this.dataBus = new SimpleIntegerProperty(0);
		this.controlBus = new SimpleIntegerProperty(0);
        this.dataSemaphore = new Semaphore(0);

		rand = new Random();

		USBConnected = new SimpleBooleanProperty(false);

        initControlBus();
	}

    /**
     * Prístup k inštancií singletonu. (synchronizované)
     * 
     * @return Inśtancia zbernice.
     */
	synchronized public static Bus getBus(){
		if(instance == null){
			instance = new Bus();
		}

		return instance;
	}

    /**
     * Pokus o pripojenie a komunikáciu cez rozhranie USB s vývojovou doskou.
     * 
     * @return True ak sa podarilo pripojiť k rozhraniu, false inak.
     */
	public boolean connectUSB(){
		int ret = k041Library.USBInitDevice();
		if(ret != 0)
			USBConnected.setValue(false);
		else
			USBConnected.setValue(true);

		System.out.println("USBInitDevice: " + ret);
		return USBConnected.getValue();
	}

    /**
     * Odpojenie od USB rozhrania.
     */
	public void disconnectUSB(){
		USBConnected.setValue(false);
	}

    /**
     * Informácia, či je zbernica pripojená k USB.
     * 
     * @return True ak je pripojená cez USB, false inak.
     */
	public boolean isUsbConnected(){
		return USBConnected.getValue();
	}

    public IntegerProperty addressBusProperty(){
        return addressBus;
    }

    public IntegerProperty dataBusProperty(){
        return dataBus;
    }

    public IntegerProperty controlBusProperty(){
        return controlBus;
    }

    /**
     * Resetovanie stavu riadiacej zbernice, nastavenie náhodnej adresy a dát.
     */
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

    /**
     * Vrátenie hodnoty na adresnej zbernici.
     * 
     * @return hodnota na adresnej zbernici.
     */
	synchronized public short getAddressBus() {
        return addressBus.getValue().shortValue();
	}

    /**
     * Vrátenie hodnoty na dátovej zbernici.
     * 
     * @return Hodnota na dátovej zbernici.
     */
    synchronized public byte getDataBus() {
        if(USBConnected.getValue()) {
            byte data = (byte) USBReadData();
            System.out.println("USBReadData: " + data);
            return data;
        }
        else
            return dataBus.getValue().byteValue();
    }

    /**
     * Vrátenie hodnoty na riadiacej zbernici.
     * 
     * @return Hodnota na riadiacej zbernici.
     */
    synchronized public int getControlBus(){
	    return controlBus.getValue();
    }

    /**
     * Nastavenie hodnoty na adresnú zbernicu.
     * 
     * @param addressBus Nová hodnota adresnej zbernice.
     */
    synchronized public void setAddressBus(short addressBus) {
        if(USBConnected.getValue())
            USBSetAddress(Short.toUnsignedInt(addressBus));
        else
            this.addressBus.setValue(Short.toUnsignedInt(addressBus));
    }

    /**
     * Nastavenie hodnoty na dátovú zbernicu.
     * 
     * @param dataBus Nová hodnota dátovej zbernice.
     */
	synchronized public void setDataBus(byte dataBus) {
		if(USBConnected.getValue())
			USBSetData(Byte.toUnsignedInt(dataBus));
		else
			this.dataBus.setValue(Byte.toUnsignedInt(dataBus));
	}

    /**
     * Nastavenie náhodnej adresnej zbernice.
     */
	synchronized public void setRandomAddress(){
		if(USBConnected.getValue())
			USBSetAddress(rand.nextInt());
		else
	    	this.addressBus.setValue(rand.nextInt(65535));
	}

    /**
     * Nastaveni náhodnej dátovej zbernice.
     */
	synchronized public void setRandomData(){
		if(USBConnected.getValue())
			USBSetData(rand.nextInt());
		else
        	this.dataBus.setValue(rand.nextInt(256));
	}

    /**
     * Pasívne čakanie na ustálenie simulácie alebo nastavenie dát dátovej zbernice vývojovou doskou,
     * maximálne však 1 sekundu v prípade, ak je pripojený k simulátoru.
     * Ak je pripojenie riešené pomocou USB k reálnej doske, čaká sa 50ms. //TODO SYNCHRONIZACNY CAS uprava
     *
     * @return True ak prišiel v časovom úseku oznam o nastavení dát, false inak.
     * @throws InterruptedException Prerušenie počas čakania na nastavenie dát.
     */
    public boolean waitForSteadyState() throws InterruptedException {
        if (!this.isUsbConnected()) {
            //ak nie je pripojenie cez USB -> je pripojenie na simulátor
            //cakaj na data
            return dataSemaphore.tryAcquire(1, TimeUnit.SECONDS);
        } else {
            //ak je pripojenie cez USB, cakaj aka synchronne
            Thread.sleep(50);
            return true;
        }
    }

    /**
     * Oznámevnie zbernici, že dáta boli ustálené a je možné ich čítať.
     */
    public void dataInSteadyState() {
        dataSemaphore.release();
    }

    /**
     * Oznámevnie zbernici, že prebiehajú zmeny, ktoré môžu ovplyvniť dáta na dátovej zbernici a nie je teda bezpečné
     * z nej čítať.
     */
    public void dataIsChanging() {
        dataSemaphore.drainPermits();
    }

    /**
     * Nastavenie negovaného signálu MW - memory write.
     * 
     * @param MW_ Nová hodnota signálu MW.
     */
    synchronized public void setMW_(boolean MW_) {
        if(USBConnected.getValue())
            if(!MW_)
                USBSetCommands(0x1e,0);  // prikaz MW\ = 0
            else
                USBSetCommands(0x1f,0);  // zrusenie prikazu
        else
            mapToControlBus("MW_", MW_);
    }

    /**
     * Nastavenie negovaného signálu MR - memory read.
     * 
     * @param MR_ Nová hodnota signálu MR.
     */
    synchronized public void setMR_(boolean MR_) {
        if(USBConnected.getValue())
            if(!MR_)
                USBSetCommands(0x1d,2);  // prikaz MR\ = 0
            else
                USBSetCommands(0x1f,0);  // zrusenie prikazu
        else {
            //this.dataBus.setValue(0); //kvazi odpojenie od zbernice
            mapToControlBus("MR_", MR_);
        }
    }
    
    /**
     * Nastavenie negovaného signálu IW - input write.
     *
     * @param IW_ Nová hodnota signálu IW.
     */
    synchronized public void setIW_(boolean IW_) {
        if(USBConnected.getValue())
            if(!IW_)
                USBSetCommands(0x1b,0);  // prikaz IOW\ = 0
            else
                USBSetCommands(0x1f,0);  // zrusenie prikazu
        else
            mapToControlBus("IW_", IW_);
    }

    /**
     * Nastavenie negovaného signálu IR - input read.
     *
     * @param IR_ Nová hodnota signálu IR.
     */
    synchronized public void setIR_(boolean IR_) {
        if(USBConnected.getValue())
            if(!IR_)
                USBSetCommands(0x17,2);  // prikaz IOR\ = 0
            else
                USBSetCommands(0x1f,0);  // zrusenie prikazu
        else {
            //this.dataBus.setValue(0); //kvazi odpojenie od zbernice
            mapToControlBus("IR_", IR_);
        }
    }

    /**
     * Nastavenie negovaného signálu IA - interruption acknowledge.
     *
     * @param IA_ Nová hodnota signálu IA.
     */
    synchronized public void setIA_(boolean IA_) {
        if(USBConnected.getValue())
            if(!IA_)
                USBSetCommands(0x0f,2);     // INTA\ = 0
            else
                USBSetCommands(0x1f,0);  // zrusenie prikazu
        else
            mapToControlBus("IA_", IA_);
    }

    /**
     * Nastavenie signálu IT - interruption.
     *
     * @param IT Nová hodnota signálu IT.
     */
    synchronized public void setIT(boolean IT) {
        mapToControlBus("IT", IT);
    }

    /**
     * Nastavenie signálu RY - ready.
     *
     * @param RY Nová hodnota signálu RY.
     */
    synchronized public void setRY(boolean RY){
        mapToControlBus("RY", RY);
    }

    /**
     * Zistenie hodoty signálu IT - interruption.
     * 
     * @return Hodnota signálu IT.
     */
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

    /**
     * Inicializácia riadiacej zbernice do východzích hodnôt.
     */
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

    /**
     * Mapovanie hodnoty signálu podľa návzu signálu do riadiacej zbernice.
     * 
     * @param signal Názov signálu.
     * @param value Nová hodnota signálu.
     */
	synchronized private void mapToControlBus(String signal, boolean value){
        int pos = mapSignal(signal);

		if(value)
			controlBus.setValue( controlBus.getValue() | ( 1<<pos ) );
		else
			controlBus.setValue( controlBus.getValue() & ~( 1<<pos ) );
	}

    /**
     * Mapovanie hodnoty signálu podľa návzu signálu z riadiacej zbernice.
     *
     * @param signal Názov signálu.
     * @return Aktuálna hodnota signálu na zbernici.
     */
    synchronized private boolean mapFromControlBus(String signal){
        int pos = mapSignal(signal);

        return (controlBus.getValue() & 1<<pos ) != 0;
    }

    /**
     * Prevodník medzi názvom signálu a jemu odpovedajúcim bitom na riadiacej zbernici.
     *
     * @param signal Názov signálu.
     * @return Odpovedajúci bit na riadiacej zbernici.
     */
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

    //
//    synchronized public boolean isMW_() {
//		return mapFromControlBus("MW_");
//	}
//
//	synchronized public boolean isMR_() {
//		return mapFromControlBus("MR_");
//	}
//
//	synchronized public boolean isIW_() {
//		return mapFromControlBus("IW_");
//	}
//
//	synchronized public boolean isIR_() {
//		return mapFromControlBus("IR");
//	}
//
//	synchronized public boolean isIA_() {
//		return mapFromControlBus("IA_");
//	}
//    
//    /**
//     * Zistenie hodoty signálu RY - ready
//     *
//     * @return Hodnota signálu RY.
//     */
//    synchronized public boolean isRY() {
//        if(USBConnected.getValue()) {
//            // nacitanie aktualnej hodnoty RY
//            USBReadData();
//            int pom = USBReadData();
//            return (pom & 0x8000) != 0;
//        }
//        else
//            return mapFromControlBus("RY");
//    }


}//end Bus