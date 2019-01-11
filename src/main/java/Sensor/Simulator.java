package Sensor;

import java.util.Calendar;
import java.util.Random;

public abstract class Simulator extends Thread {

    protected volatile boolean stopCondition = false;
    protected Random rnd = new Random();
    private long midnight;
    private SensorStream stream;
    private String id;
    private String type;

    public Simulator(String id, String type, SensorStream stream){

        this.id = id;
        this.type = type;
        this.stream = stream;
        this.midnight = computeMidnightMilliseconds();
    }

    public void stopMeGently() {
        stopCondition = true;
    }

    protected void sendMeasurement(double measurement){
        stream.sendMeasurement(new Measurement(id, type, measurement, deltaTime()));
    }

    public SensorStream getStream(){
        return stream;
    }

    protected void sensorSleep(long milliseconds){
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public abstract void run();

    private long computeMidnightMilliseconds(){
        Calendar c = Calendar.getInstance();
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
    }

    private long deltaTime(){
        return System.currentTimeMillis()-midnight;
    }

    public String getIdentifier(){
        return id;
    }

}
