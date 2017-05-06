package sk.uniza.fri.cp.BreadboardSim.Board;


import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.reactfx.EventSource;
import sk.uniza.fri.cp.BreadboardSim.Devices.Device;
import sk.uniza.fri.cp.BreadboardSim.Socket.PowerSocket;
import sk.uniza.fri.cp.Bus.Bus;

import java.util.HashSet;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author Moris
 * @version 1.0
 * @created 17-mar-2017 16:16:34
 */
public class BoardSimulator {
    public static final Logger LOGGER = LogManager.getLogger("MainLogger");
    public static final Logger QUEUELOGGER = LogManager.getLogger("QueueLogger");

	private LinkedBlockingQueue<BoardEvent> eventsQueue;

	private List<PowerSocket> powerSockets;
	private BooleanProperty running;

    private AtomicBoolean steadyState = new AtomicBoolean(false); //TODO steadystate ako property a podla toho riadit emittor

	private Service simulatorService = new Service() {
		@Override
		protected Task createTask() {
			return new Task() {
				@Override
				protected Object call() throws Exception {
                    Thread.currentThread().setName("SimulationThread");

                    int processedEvents = 0;

                    //pripojenie napajania
                    powerSockets.forEach(PowerSocket::powerUp);

                    running.setValue(true);
                    steadyState.set(false);

                    Bus.getBus().setEventsQueue(eventsQueue);
                    Bus.getBus().simulationIsRunning(true);
                    Bus.getBus().dataIsChanging();

                    HashSet<Device> devicesToUpdate = new HashSet<>();
                    BoardEvent event;
                    //obsluha eventov
					while(!isCancelled()){
						try {
                            event = eventsQueue.poll();
                            if (event == null) {
                                if (eventsQueue.size() > 0) {
                                    event = eventsQueue.take();
                                } else {
                                    Bus.getBus().dataInSteadyState();
                                    steadyState.set(true);
                                    event = eventsQueue.take();
                                }
                            }

                            if (steadyState.get()) {
                                Bus.getBus().dataIsChanging();
                                steadyState.set(false);
                            }

                            event.process(devicesToUpdate);
                            processedEvents++;

                            devicesToUpdate.forEach((Device::simulate));

							devicesToUpdate.clear();
						} catch (InterruptedException e){
							if(isCancelled()) break;
						} catch (NullPointerException e){
							e.printStackTrace(System.err);
                            devicesToUpdate.clear();
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
                    steadyState.set(true);
                    Bus.getBus().simulationIsRunning(false);

                    System.out.println("Processed events: " + processedEvents);
                    return null;
                }
            };
		}
	};

	public BoardSimulator(){
        this.eventsQueue = new LinkedBlockingQueue<>();
        this.running = new SimpleBooleanProperty(false);

		simulatorService.onFailedProperty();
		simulatorService.setOnFailed(new EventHandler<WorkerStateEvent>() {
			@Override
			public void handle(WorkerStateEvent event) {
				System.out.println(event);
				simulatorService.getException().printStackTrace(System.err);
			}
		});
	}


	public void start(List<PowerSocket> powerSockets){
		this.powerSockets = powerSockets;

		simulatorService.restart();
    }


	public void stop(){
		simulatorService.cancel();
	}

	/**
     *
     * @param event
	 */
	public void addEvent(BoardEvent event){
		try {

//            if (event.getSocket() != null)
//                QUEUELOGGER.debug("[PRIDAVAM k " + eventsQueue.size() + "] soket: " + event.getSocket() + " na komponente: " + event.getSocket().getComponent() +
//                        (event.getSocket().getDevice() != null ? (" zariadenie: " + event.getSocket().getDevice() + " a pin: " + event.getSocket().getPin().getName()) : "") +
//                        (event instanceof BoardChangeEvent ? " na hodnotu: " + ((BoardChangeEvent) event).getValue() : ""));

//            Bus.getBus().dataIsChanging();
            eventsQueue.put(event);

        } catch (InterruptedException e) {
//            e.printStackTrace();
        }
	}

	public BooleanProperty runningProperty(){
        return this.running;
    }

    public boolean inSteadyState() {
        return steadyState.get();
    }

    private AtomicBoolean wait = new AtomicBoolean(false);

    /**
     * Cakanie na nastavenie priznakov
     */
    public void waitForMe(boolean value) {
        wait.set(value);
    }
}