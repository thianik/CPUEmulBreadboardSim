package sk.uniza.fri.cp.CPUEmul;


/**
 * updateuje stav pri zmene stavu CPU (ChangeListener na Message Task-u)
 * @author Moris
 * @version 1.0
 * @created 07-feb-2017 18:40:27
 */
public class CPUController {

	private enumDisplayOptions stateDispayRegisters;
	private enumDisplayOptions stateDisplayStackAddr;
	private enumDisplayOptions stateDisplayStackData;

	/**
	 *
	 * @param lineNumber
	 */
	public void heightlightLine(int lineNumber){

	}

	private void updateState(){

	}

	/**
	 * HANDLERS
	 */

	private void handleClearConsoleAction(){

	}

	/**
	 * nacita text studenta (program) do suboru
	 */
	private void handleLoadAction(){

	}

	private void handleSaveAction(){

	}


	/**
	 * zoberie program studenta a pokusi sa ho parsovat na instrukcie... zaroven
	 * zablokuje moznost parsovania az pokial sa nezmeni cast studentovho kodu
	 */
	private void handleParseAction(){

	}

	private void handlePauseAction(){

	}

	private void handleResetAction(){

	}

	private void handleStartAction(){

	}

	private void handleStepAction(){

	}

	private void handleStopAction(){

	}



	/**
	 * pridava a odobera debugovacie zastavenia v programe
	 */
	private void handleSetBreakAction(){

	}

	private void handleUnbreakAction(){

	}

	private void handleUnbreakAllAction(){

	}


	/**
	 * obsluha zmeny zobrazenia registrov
	 */
	private void handleRegDispGroupAction(){

	}

	private void handleStackAddrDispGroupAction(){

	}

	private void handleStackDataDispGroupAction(){

	}

}//end CPUController