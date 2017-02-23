package sk.uniza.fri.cp.Bus;

import java.util.concurrent.CountDownLatch;

/**
 * Created by Moris on 20.2.2017.
 */
public class BusSimulated extends Bus {

    private static final int ANSWER_TIMEOUT = 400;

    private BusSimulated() {
        super();
    }

    public static Bus getBus(){
        if(instance == null){
            instance = new BusSimulated();
        }

        return instance;
    }

    //SYNCHRONNE

    @Override
    public synchronized void setMW_(boolean MW_) {
        super.setMW_(MW_);

        if(!MW_)
            System.out.println("Data write: " + Integer.toBinaryString( Byte.toUnsignedInt( super.getDataBus() ) ).substring(8) );
    }

    @Override
    public synchronized void setIW_(boolean IW_) {
        super.setIW_(IW_);

        if(!IW_)
            System.out.println("Data write: " + Integer.toBinaryString( Byte.toUnsignedInt( super.getDataBus() ) ).substring(8) );
    }

    @Override
    public synchronized void setMR_(boolean MR_) {
        super.setMR_(MR_);

        if(!MR_)
            super.setDataBus((byte) 170);
        else
            super.setRandomData();
    }

    @Override
    public synchronized void setIR_(boolean IR_) {
        super.setIR_(IR_);

        if(!IR_)
            super.setDataBus((byte) 170);
        else
            super.setRandomData();
    }

    //ASYNCHRONNE

    @Override
    synchronized public void setMW_(boolean MW_, CountDownLatch cdlRY) {
        super.setMW_(MW_, cdlRY);

        answerToWriteAsync(MW_);
    }

    @Override
    public synchronized void setIW_(boolean IW_, CountDownLatch cdlRY) {
        super.setIW_(IW_, cdlRY);

        answerToWriteAsync(IW_);
    }

    @Override
    public synchronized void setMR_(boolean MR_, CountDownLatch cdlRY) {
        super.setMR_(MR_, cdlRY);

        answerToReadAsync(MR_);
    }

    @Override
    public synchronized void setIR_(boolean IR_, CountDownLatch cdlRY) {
        super.setIR_(IR_, cdlRY);

        answerToReadAsync(IR_);
    }

    private void answerToWriteAsync(boolean signal){
        //ked ide MW do nuly - perif. odpoveda RDY 1 po zapise
        if(!signal) {
            Thread answer = new Thread(new AnswerWriteAsync(this));
            answer.setDaemon(true);
            answer.start();
        } else { //ked ide MW do jednotky, perif. odpoveda zrusenim RDY
            super.setRY(false);
        }
    }

    private void answerToReadAsync(boolean signal){
        //ked ide MW do nuly - perif. odpoveda RDY 1 po zapise
        if(!signal) {
            Thread answer = new Thread(new AnswerReadAsync(this));
            answer.setDaemon(true);
            answer.start();
        } else {
            setRandomData();
            setRY(false);
        }
    }

    private static class AnswerWriteAsync implements Runnable{
        Bus bus;

        AnswerWriteAsync(Bus bus){
            this.bus = bus;
        }

        @Override
        public void run() {
            try {
                Thread.sleep(ANSWER_TIMEOUT);
                System.out.println("Data write: " + bus.getDataBus());
                bus.setRY(true);

            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private static class AnswerReadAsync implements Runnable{
        Bus bus;

        AnswerReadAsync(Bus bus){
            this.bus = bus;
        }

        @Override
        public void run() {
            try {
                Thread.sleep(ANSWER_TIMEOUT);
                bus.setDataBus((byte) 85);
                bus.setRY(true);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
