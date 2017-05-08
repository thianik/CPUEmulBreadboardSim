package sk.uniza.fri.cp.BreadboardSim.Board;

import sk.uniza.fri.cp.BreadboardSim.Devices.Device;
import sk.uniza.fri.cp.BreadboardSim.Socket.Potential;
import sk.uniza.fri.cp.BreadboardSim.Socket.Socket;

import java.util.Set;

/**
 * Objekt reprezentujúci zmenovú udalosť v simulácií.
 * Zmenou je hodnota potenciálnu na sokete.
 *
 * @author Tomáš Hianik
 * @version 1.0
 * @created 17-mar-2017 16:16:34
 */
public class BoardChangeEvent extends BoardEvent {
	private Potential.Value newValue;

    /**
     * Zmenová udalosť.
     *
     * @param socket   Soket, na ktorom sa má zameniť hodnota potenciálu.
     * @param newValue Nová hodnota potenciálu
     */
    public BoardChangeEvent(Socket socket, Potential.Value newValue){
        super(socket);
        this.newValue = newValue;
    }

    /**
     * Nová hodnota potenciálu, ktorá sa má nataviť.
     *
     * @return Nová hodnota potenciálu.
     */
    public Potential.Value getValue(){
        return newValue;
    }

    /**
     * Spracovanie udalosti.
     * Ak nastane skrat, nedoplnia sa zariadenia na akutaizáciu.
     *
     * @param devices Zoznam zariadení, ktorých vstupné piny sú zmenou ovplyvnené.
     *                Spracovanie doplní nové zariadenia na koniec zoznamu.
     */
    @Override
    public void process(Set<Device> devices) {
        Socket socket = getSocket();
        if(devices != null && socket != null){
            Potential potential = socket.getPotential();

            if(potential != null) {
                Potential.Value oldValue = potential.getValue();

                //ak nenastal skrat, ak by nastal a boli by pripojene zariadenia ktore ho vyvolali, zacal by sa cyklit
                if(socket.setPotential(newValue))
                    //a ak sa hodnota zmenila
                    if (oldValue != socket.getPotential().getValue())
                        //vrat zariadenia s ovplyvnenymi vstupmi
                        potential.getDevicesWithInputs(devices);
            }
        }
    }

}