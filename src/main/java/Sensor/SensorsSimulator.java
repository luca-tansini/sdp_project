package Sensor;

public class SensorsSimulator {

    public static void main(String args[]){

        if(args.length != 2){
            System.out.println("usage: SensorsSimulator <ServerCloudIpAddr> <NumberOfSensors>");
            return;
        }

        String serverAddr = args[0];
        int numSensors = Integer.parseInt(args[1]);

        for(int i=0; i<numSensors; i++){
            new SensorThread(serverAddr).start();
        }

    }

}
