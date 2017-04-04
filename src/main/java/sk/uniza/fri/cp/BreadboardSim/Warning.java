package sk.uniza.fri.cp.BreadboardSim;


/**
 * @author Moris
 * @version 1.0
 * @created 17-mar-2017 16:16:36
 */
public class Warning {

	private HighlightGroup sender;
	private String description;

	public Warning(){

	}

	public void finalize() throws Throwable {

	}

	/**
	 * 
	 * @param sender
	 * @param description
	 */
	public Warning(HighlightGroup sender, String description){

	}

	public HighlightGroup getSender(){
		return sender;
	}

	public String getDescription(){
		return description;
	}

}