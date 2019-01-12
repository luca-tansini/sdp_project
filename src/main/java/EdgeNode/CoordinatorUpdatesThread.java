package EdgeNode;

import EdgeNode.EdgeNetworkMessage.CoordinatorMessage;
import Sensor.Measurement;
import ServerCloud.Model.EdgeNodeRepresentation;
import com.google.gson.Gson;

import java.net.DatagramPacket;
import java.net.InetSocketAddress;
import java.time.Instant;
import java.util.ArrayList;

import static EdgeNode.EdgeNode.ElectionStatus.FINISHED;
import static EdgeNode.EdgeNode.ElectionStatus.STARTED;

public class CoordinatorUpdatesThread extends Thread {

    private EdgeNode parent;
    private SharedDatagramSocket socket;
    private ArrayList<Measurement> buffer= new ArrayList();
    private Gson gson = new Gson();
    private Measurement cached;

    public CoordinatorUpdatesThread(EdgeNode parent, SharedDatagramSocket socket){
        this.parent = parent;
        this.socket = socket;
    }

    @Override
    public void run() {

        while (!parent.isShutdown()){

            EdgeNodeRepresentation coordinator = parent.getCoordinator();
            if(coordinator != null && cached != null) {
                System.out.println("DEBUG: CoordinatorUpdatesThread mando misurazione cached");
                Measurement tmp = cached;
                cached = null;
                sendMeasurement(coordinator, tmp);
            }

            buffer.add(parent.getSensorsMeasurementBuffer().take());
            if(buffer.size() == 40){
                double mean = 0;
                for(Measurement m: buffer)
                    mean += m.getValue();
                Measurement measurement = new Measurement(parent.getNodeId()+"", "local", mean/40, Instant.now().toEpochMilli());
                //Sliding window, 50% overlap
                for(int i=0; i<20; i++)
                    buffer.remove(0);

                coordinator = parent.getCoordinator();
                if(coordinator != null) {
                    System.out.println("DEBUG: CoordinatorUpdatesThread mando misurazione");
                    sendMeasurement(coordinator, measurement);
                }
                else{
                    //Non ha senso avere più di un valore cachato
                    this.cached = measurement;
                }
            }

        }
    }

    private void sendMeasurement(EdgeNodeRepresentation coordinator, Measurement measurement){
        CoordinatorMessage msg = new CoordinatorMessage(CoordinatorMessage.CoordinatorMessageType.STATS_UPDATE, parent.getRepresentation(), measurement);
        String json = gson.toJson(msg, CoordinatorMessage.class);
        parent.getEdgeNetworkSocket().write(new DatagramPacket(json.getBytes(), json.length(), new InetSocketAddress(coordinator.getIpAddr(), coordinator.getNodesPort())));
        synchronized (parent.getCoordinatorACKLock()){
            parent.setAwaitingCoordinatorACK(true);
            try{parent.getCoordinatorACKLock().wait(2000);} catch (InterruptedException e){e.printStackTrace();}
        }
        if(parent.isAwaitingCoordinatorACK()){
            System.out.println("DEBUG: CoordinatorUpdatesThread: Il coordinatore è morto!");
            cached = measurement;
            parent.setCoordinator(null);
            parent.setAwaitingCoordinatorACK(false);
            synchronized (parent.getElectionStatusLock()){
                if(parent.getElectionStatus() != FINISHED)
                    return;
                parent.setElectionStatus(STARTED);
            }
            System.out.println("DEBUG: CoordinatorUpdatesThread: faccio partire elezione");
            parent.bullyElection();
        }
    }

}
