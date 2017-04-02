package sk.uniza.fri.cp.BreadboardSim;


/**
 * Vymenovanie možných typov soketov. Slúžia na určenie priority hodnoty potenciálu obvodu.
 *
 * @version 1.1
 * @created 17-mar-2017 16:16:36
 */
public enum SocketType {
	OUT, //obyčaný výstup na najvyššiu prioritu pri určovaní hodnoty potenciálu
	WEAK_OUT,   //vystup cez rezistory, moze sa napojit priamo na VCC alebo GND, slabsi OUT
                //neexistuje k nemu pin, typ sa nastavuje priamo pri vytvarani NumKeys (mozne doplnenie pinu v buducnosti)
	OCO, //vystup s otvorenym kolektorom, dokaze iba stiahnut hodnotu na LOW, nedokaze ju vyhnat na HIGH ak nie je
	IO, //tri stavy - HIGH, LOW, HIGH IMPEDANCE
	IN, //vstup, nema na hodnotu potencialu vplyv
	NC; //nepripojene, nema na hodnotu potencialu vplyv

	/**
	 * 
	 * @param st1
	 * @param st2
	 */
	public static boolean isConflict(SocketType st1, SocketType st2){
		return false;
	}

	/**
	 * 
	 * @param other
	 */
	public boolean isConflict(SocketType other){
		return false;
	}
}