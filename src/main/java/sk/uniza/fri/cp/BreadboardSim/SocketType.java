package sk.uniza.fri.cp.BreadboardSim;


/**
 * @author Moris
 * @version 1.0
 * @created 17-mar-2017 16:16:36
 */
public enum SocketType {
	IN,
	OUT,
	IO,
	OCO,
	NC;

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