package sk.uniza.fri.cp.Bus;


/**
 * Sprostredkuva komunikaciu medzi CPU a doskou (fyzickou / simulovanou)
 * @author Moris
 * @version 1.0
 * @created 07-feb-2017 18:40:27
 */
public class Bus {

	private short address;
	private short control;
	private byte data;

	public Bus(){

	}

	public void finalize() throws Throwable {

	}
	public short getAddress(){
		return address;
	}

	public static Bus getBus(){
		return null;
	}

	public byte getData(){
		return data;
	}

	public boolean isBA(){
		return false;
	}

	public boolean isBQ(){
		return false;
	}

	public boolean isIA(){
		return false;
	}

	public boolean isIR(){
		return false;
	}

	public boolean isIT(){
		return false;
	}

	public boolean isIW(){
		return false;
	}

	public boolean isMR(){
		return false;
	}

	public boolean isMW(){
		return false;
	}

	public boolean isRY(){
		return false;
	}

	/**
	 * 
	 * @param newVal
	 */
	public void setAddress(short newVal){
		address = newVal;
	}

	/**
	 * 
	 * @param newVal
	 */
	public void setBA(boolean newVal){

	}

	/**
	 * 
	 * @param newVal
	 */
	public void setBQ(boolean newVal){

	}

	/**
	 * 
	 * @param newVal
	 */
	public void setData(byte newVal){
		data = newVal;
	}

	/**
	 * 
	 * @param newVal
	 */
	public void setIA(boolean newVal){

	}

	/**
	 * 
	 * @param newVal
	 */
	public void setIR(boolean newVal){

	}

	/**
	 * 
	 * @param newVal
	 */
	public void setIT(boolean newVal){

	}

	/**
	 * 
	 * @param newVal
	 */
	public void setIW(boolean newVal){

	}

	/**
	 * 
	 * @param newVal
	 */
	public void setMR(boolean newVal){

	}

	/**
	 * 
	 * @param newVal
	 */
	public void setMW(boolean newVal){

	}

	public void setRandomAddress(){

	}

	public void setRandomData(){

	}

	/**
	 * 
	 * @param newVal
	 */
	public void setRY(boolean newVal){

	}
}//end Bus