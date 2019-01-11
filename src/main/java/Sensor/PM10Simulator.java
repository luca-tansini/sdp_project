package Sensor;

public class PM10Simulator extends Simulator {


    private final double A = 50;
    private final double W = 0.05;
    private static int ID = 1;

    public PM10Simulator(String id, SensorStream stream){
        super(id, "PM10", stream);
    }

    //use this constructor to initialize the pm10's simulator in your project
    public PM10Simulator(SensorStream stream){
        this("pm10-"+(ID++), stream);
    }

    @Override
    public void run() {

        double i = rnd.nextInt();
        long waitingTime;

        while(!stopCondition){

            double pm10 = getPM10Value(i);
            sendMeasurement(pm10);

            waitingTime = 400 + (int)(Math.random()*300);
            sensorSleep(waitingTime);

            i+=0.2;

        }

    }

    private double getPM10Value(double t){
        return Math.abs(A * Math.sin(W*t) + rnd.nextGaussian()*0.3);

    }
}
