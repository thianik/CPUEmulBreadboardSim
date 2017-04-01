package sk.uniza.fri.cp.BreadboardSim;


import sk.uniza.fri.cp.BreadboardSim.Devices.Device;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Moris
 * @version 1.0
 * @created 17-mar-2017 16:16:35
 */
public class Potential {

	public enum Value{ HIGH, LOW, NC }

	private Socket socket1;
	private Socket socket2;
	private Potential parent1;
	private Potential parent2;
	private Potential child;
	volatile private Value value;
	private SocketType type;
	private boolean shortCircuit; //nastal skrat na potenciali?

	/**
	 * 
	 * @param socket1
	 * @param socket2
	 */
	public Potential(Socket socket1, Socket socket2){
		this.socket1 = socket1;
		this.socket2 = socket2;
		this.update();
	}

	public Socket getSocket1(){
		return this.socket1;
	}

	public Socket getSocket2(){
		return this.socket2;
	}

	public synchronized Potential getPotential(){
		if(child != null){
			return child.getPotential();
		}

		return this;
	}

	public ArrayList<Socket> getInputs(){
		return null;
	}

	public ArrayList<Socket> getConnectedSockets(){
		return null;
	}

	public void getDevicesWithInputs(List<Device> listToFill) {
		if(listToFill == null) return;

		if (this.parent1 == null && this.parent2 == null) {
			if (this.socket1 != null && (this.type == SocketType.IN || this.type == SocketType.IO)) {
				listToFill.add(this.socket1.getDevice());
			}

			if (this.socket2 != null &&  (this.type == SocketType.IN || this.type == SocketType.IO)) {
				listToFill.add(this.socket2.getDevice());
			}
		} else {
			if (this.parent1 != null) {
				this.parent1.getDevicesWithInputs(listToFill);
			}
			if (this.parent2 != null) {
				this.parent2.getDevicesWithInputs(listToFill);
			}
		}
	}

	public synchronized void update(){
		if (this.parent1 != null) {
			this.parent1.child = null;
		}
		if (this.parent2 != null) {
			this.parent2.child = null;
		}

		if (this.socket1 != null) {
			this.parent1 = this.socket1.getPotential();
		}
		if (this.socket2 != null) {
			this.parent2 = this.socket2.getPotential();
		}

		if (this.parent1 == this) {
			this.parent1 = null;
		}
		if (this.parent2 == this) {
			this.parent2 = null;
		}

		//ak je jeden z predkov zoskratovany, je skratovany aj tento potencial
		this.shortCircuit = false;

		if (this.parent1 != null) {
			this.parent1.child = this;
			this.shortCircuit = this.parent1.shortCircuit;
		}
		if (this.parent2 != null) {
			this.parent2.child = this;
			this.shortCircuit |= this.parent2.shortCircuit;
		}

		//odpojenie potomka pred aktualizaciou typu a hodnoty (aj na nom by sa volali)
		Potential oldChild = this.child;
		this.child = null;

		this.updateType();

		this.setValue(Value.NC);

		if (oldChild != null)
			oldChild.update();
	}

	public synchronized Value getValue(){
		return value;
	}

	/**
	 * Nastavenie novej hodnoty potencálu.
	 * Pri zmene hodnoty potenciálu sa berie do úvahy jeho typ, typ a hodnota predkov, ak nejakých má.
	 * Kontroluje sa aj skrat pri spojeni dvoch rozdielnych vystupv.
	 *
	 * @param newVal Nova pozadovana hodnota potencialu
	 * @return True - hodnota sa spravne aktualizovala / False - nastal skrat
	 */
	public synchronized boolean setValue(Value newVal){

		if (this.parent1 != null && this.parent2 != null ){
			//ak ma potencial oboch predkov

			if(this.shortCircuit){
				//ak je potencial zoskratovany od predkov, nastav nahodnu hodnotu
				if(Math.random() < 0.5)
					this.value = Value.LOW;
				else
					this.value = Value.HIGH;
			}
			else if (this.type == SocketType.NC || this.type == SocketType.IN){
				//potencial je typu vstup alebo nepripojeny -> neovplyvnuje hodnotu vysledneho potencialu
				this.value = Value.NC;
			}
			else if (this.parent1.getValue() == Value.NC && this.parent2.getValue() == Value.NC){
				//ani jeden z rodicov nie je napojeny -> neprebera ziadnu hodnotu
				this.value = Value.NC;
			}
			else if (this.type == SocketType.OCO){
				//ak je potencial typu otvoreny kolektor,
				if(this.parent1.getValue() == Value.HIGH || this.parent2.getValue() == Value.HIGH){
					this.value = Value.HIGH;
				} else {
					this.value = Value.LOW;
				}
			} else if (this.type == SocketType.IO){
				this.value = newVal;	//TODO kontrola? aj nasledujuca vetva.. .a vlastne vsetky... a poriadne
			}
			else if(this.type == SocketType.OUT){
				//ak je potencial typu OUT -> aspon jeden z predkov je tiez OUT
				if(this.parent1.type == SocketType.OUT && this.parent2.type == SocketType.OUT){
					//ak su oba predkovia typu OUT -> moze dojst ku skratu pri roznych hodnotach napatia
					if(this.parent1.value != this.parent2.value){
						//bum, doslo ku skratu
						this.shortCircuit = true;
						if(Math.random() < 0.5)	this.value = Value.LOW;
						else this.value = Value.HIGH;

						System.out.println("SKRAT");
					} else {
						//nedoslo ku skratu, na obochy vystupoch su rovnake hodnoty
						this.value = this.parent1.value;
					}
				}
				else if(this.parent1.type == SocketType.OUT){
					//ak je iba prvy typu OUT, berie si jeho hodnotu
					this.value = this.parent1.value;
				}
				else {
					//ak je iba druhy typu OUT, berie si jeho hodnotu
					this.value = this.parent2.value;
				}
			}
		} else if(this.parent1 != null){
			this.value = this.parent1.getValue();
		} else if(this.parent2 != null){
			this.value = this.parent2.getValue();
		} else {
			this.value = newVal;
		}


		//ak ma potencial potomka, aktualizuj jeho hodnotu a vrat oznamenie o moznom skrate (false ak nastal)
		if(this.child != null)
			return this.child.setValue(this.value);

		//ak potencial nema potomka, vrat hodnotu false ak doslo ku skratu lebo sa nenastavila pozadovana hodnota
		return !this.shortCircuit;
	}

	public SocketType getType(){
		return this.type;
	}

	public synchronized void setType(SocketType newType){
		if (this.parent1 == null && this.parent2 == null) {
			this.type = newType;

			if (this.child != null)
				this.child.updateType();
		} else {
			this.updateType();
		}
	}

	public Potential getChild(){
		return this.child;
	}

	public synchronized void updateType(){
		SocketType type1 = SocketType.NC;
		SocketType type2 = SocketType.NC;

		//ak potencial ma aspon jedneho predka
		if(this.parent1 != null || this.parent2 != null){
			if(this.parent1 != null)
				type1 = this.parent1.getType();

			if(this.parent2 != null)
				type2 = this.parent2.getType();

			if(type1 == SocketType.OUT || type2 == SocketType.OUT){ //ak je jeden z predkov vystup
				this.type = SocketType.OUT;
			} else if(type1 == SocketType.OCO || type2 == SocketType.OCO){ //ak ma jeden z predkov otvoreny kolektor
				this.type = SocketType.OCO;
			} else if(type1 == SocketType.IO|| type2 == SocketType.IO){ //ak je predok vstup/vystup
				this.type = SocketType.IO;
			} else if(type1 == SocketType.IN || type2 == SocketType.IN){ //ak je predok iba vstup
				this.type = SocketType.IN;
			} else {
				this.type = SocketType.NC;
			}

		} else { //ak nema ani jedneho predka, je nepripojeny
			this.type = SocketType.NC;
		}

		//aktualizuj typ potomka
		if(this.child != null)
			this.child.updateType();
	}

	public void delete(){
		this.parent1.child = null;
		this.parent2.child = null;
		if (this.child != null) {
			this.child.update();
		}
	}



}