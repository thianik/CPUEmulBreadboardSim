package sk.uniza.fri.cp.BreadboardSim.Board;


import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;
import org.reactfx.EventSource;
import sk.uniza.fri.cp.BreadboardSim.Devices.Device;
import sk.uniza.fri.cp.BreadboardSim.Socket.PowerSocket;
import sk.uniza.fri.cp.Bus.Bus;

import java.util.HashSet;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author Moris
 * @version 1.0
 * @created 17-mar-2017 16:16:34
 */
public class BoardSimulator {

	private LinkedBlockingQueue<BoardEvent> eventsQueue;

	private List<PowerSocket> powerSockets;
	private BooleanProperty running;

    private AtomicBoolean steadyState = new AtomicBoolean(false); //TODO steadystate ako property a podla toho riadit emittor

    public static EventSource<Void> tick = new EventSource<>();

	private Service simulatorService = new Service() {
		@Override
		protected Task createTask() {
			return new Task() {
				@Override
				protected Object call() throws Exception {


                    int processedEvents = 0;

                    //pripojenie napajania
                    powerSockets.forEach(PowerSocket::powerUp);

                    running.setValue(true);
                    steadyState.set(false);
                    Bus.getBus().simulationIsRunning(true);
                    Bus.getBus().dataIsChanging();

                    HashSet<Device> devicesToUpdate = new HashSet<>();

					//obsluha eventov
					while(!isCancelled()){
						try {
                            if (eventsQueue.size() == 0) {
                                Bus.getBus().dataInSteadyState();
                                steadyState.set(true);
                            }

                            BoardEvent event = eventsQueue.take();

                            if (steadyState.get()) {
                                Bus.getBus().dataIsChanging();
                                steadyState.set(false);
                            }
//                            if(!Bus.getBus().isIA_())
//                            System.out.println("------------------------------------------------------------------------ PROCESS " + event.getSocket().getComponent());

                            event.process(devicesToUpdate);
                            processedEvents++;

//                            if(!Bus.getBus().isIA_())
//                            System.out.println("----------------------------------- SIMULATE ");
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
			eventsQueue.put(event);
        } catch (InterruptedException e) {
            //e.printStackTrace();
        }
	}

	public BooleanProperty runningProperty(){
        return this.running;
    }

    public boolean inSteadyState() {
        return steadyState.get();
    }
}