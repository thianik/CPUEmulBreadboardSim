package sk.uniza.fri.cp.BreadboardSim.Board;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import sk.uniza.fri.cp.BreadboardSim.Devices.Device;
import sk.uniza.fri.cp.BreadboardSim.Socket.PowerSocket;
import sk.uniza.fri.cp.Bus.Bus;

import java.util.HashSet;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Simulačné jadro.
 * O beh vlákna so simuláciou sa stara Service.
 *
 * @author Tomáš Hianik
 * @version 1.0
 * @created 17-mar-2017 16:16:34
 */
public class BoardSimulator {

    private long startTime;
    private int events;

    private LinkedBlockingQueue<BoardEvent> eventsQueue; //naplánované udalosti
    private List<PowerSocket> powerSockets; //sokety, ktoré sa maju po spusteni napojit
    private BooleanProperty running; //priznak behu simulacie

    /**
     * Simulačná slučka nového vlákna
     */
    private Service simulatorService = new Service() {
		@Override
		protected Task createTask() {
			return new Task() {
				@Override
				protected Object call() throws Exception {
                    Thread.currentThread().setName("SimulationThread");

                    boolean steadyState = false;

                    //pripojenie napajania
                    powerSockets.forEach(PowerSocket::powerUp);

                    running.setValue(true);

                    Bus.getBus().setEventsQueue(eventsQueue);
                    Bus.getBus().simulationIsRunning(true);
                    Bus.getBus().dataIsChanging();

                    HashSet<Device> devicesToUpdate = new HashSet<>();
                    BoardEvent event;

                    startTime = System.currentTimeMillis();

                    //obsluha eventov
					while(!isCancelled()){
						try {
                            event = eventsQueue.poll();
                            if (event == null) {
                                if (eventsQueue.size() > 0) {
                                    event = eventsQueue.take();
                                } else {
                                    Bus.getBus().dataInSteadyState();
                                    steadyState = true;
                                    event = eventsQueue.take();
                                }
                            }

                            if (steadyState) {
                                Bus.getBus().dataIsChanging();
                                steadyState = false;
                            }

                            event.process(devicesToUpdate);

                            devicesToUpdate.forEach((Device::simulate));

							devicesToUpdate.clear();

                            events++;
                        } catch (InterruptedException e){
							if(isCancelled()) break;
						} catch (NullPointerException e){
							e.printStackTrace(System.err);
                            devicesToUpdate.clear();
                        }

                        if (System.currentTimeMillis() - startTime > 1000) {
                            System.out.println("Eventov " + events);
                            events = 0;
                            startTime = System.currentTimeMillis();
                        }
                    }

                    Bus.getBus().dataIsChanging();

					powerSockets.forEach(PowerSocket::powerDown);

                    while (eventsQueue.size() > 0) {
                        eventsQueue.take().process(devicesToUpdate);
                        devicesToUpdate.forEach((Device::simulate));
                        devicesToUpdate.clear();
                    }
                    running.setValue(false);
                    Bus.getBus().simulationIsRunning(false);

                    return null;
                }
            };
		}
	};

    /**
     * Objekt jadra simulátora.
     */
    BoardSimulator() {
        this.eventsQueue = new LinkedBlockingQueue<>();
        this.running = new SimpleBooleanProperty(false);

        simulatorService.setOnFailed(event -> {
            System.out.println(event);
            simulatorService.getException().printStackTrace(System.err);
        });
    }

    /**
     * Spustenie simulačného vlákna.
     *
     * @param powerSockets Najájacie sokety, ktoré sa majú simulovať ako prvé.
     */
    public void start(List<PowerSocket> powerSockets){
		this.powerSockets = powerSockets;

		simulatorService.restart();
    }

    /**
     * Zastavenie simulačného vlákna.
     */
    public void stop(){
		simulatorService.cancel();
	}

	/**
     * Pridanie novej udalosti do fronty.
     *
     * @param event Nová udalosť simulácie.
     */
	public void addEvent(BoardEvent event){
		try {
            eventsQueue.put(event);
        } catch (InterruptedException e) {
//            e.printStackTrace();
        }
	}

    /**
     * Proterty behu simululácie.
     *
     * @return Property - true, ak sa simulácia vykonáva, false inak.
     */
    public BooleanProperty runningProperty(){
        return this.running;
    }

}