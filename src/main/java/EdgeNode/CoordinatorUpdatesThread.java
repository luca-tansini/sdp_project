package EdgeNode;

import EdgeNode.EdgeNetworkMessage.CoordinatorMessage;
import Sensor.Measurement;
import ServerCloud.Model.EdgeNodeRepresentation;
import com.google.gson.Gson;

import java.net.DatagramPacket;
import java.net.InetSocketAddress;
import java.time.Instant;
import java.util.ArrayList;

import static EdgeNode.StateModel.ElectionStatus.FINISHED;
import static EdgeNode.StateModel.ElectionStatus.STARTED;

public class CoordinatorUpdatesThread extends Thread {

    private StateModel stateModel;
    private ArrayList<Measurement> buffer= new ArrayList();
    private Gson gson = new Gson();
    private Measurement cached;

    public CoordinatorUpdatesThread(){
        this.stateModel = StateModel.getInstance();
    }

    @Override
    public void run() {

        while (!stateModel.shutdown){

            EdgeNodeRepresentation coordinator = stateModel.getCoordinator();
            if(coordinator != null && cached != null) {
                Measurement tmp = cached;
                cached = null;
                sendMeasurement(coordinator, tmp);
            }

            Measurement tmp = stateModel.sensorsMeasurementBuffer.take();
            if(tmp.getType().equals("quit"))
                break;
            buffer.add(tmp);
            if(buffer.size() == 40){
                double mean = 0;
                for(Measurement m: buffer)
                    mean += m.getValue();
                Measurement measurement = new Measurement(stateModel.parent.getNodeId()+"", "local", mean/40, Instant.now().toEpochMilli());

                coordinator = stateModel.getCoordinator();
                if(coordinator != null) {
                    sendMeasurement(coordinator, measurement);
                }
                else{
                    //Non ha senso avere più di un valore cachato
                    this.cached = measurement;
                }

                //Aggiorna nel model la statistica locale
                synchronized (stateModel.statsLock){
                    stateModel.stats.getLocal().put(stateModel.parent.getNodeId()+"", measurement);
                }

                //Sliding window, 50% overlap
                for(int i=0; i<20; i++)
                    buffer.remove(0);

            }
        }
    }

    private void sendMeasurement(EdgeNodeRepresentation coordinator, Measurement measurement){
        CoordinatorMessage msg = new CoordinatorMessage(CoordinatorMessage.CoordinatorMessageType.STATS_UPDATE, stateModel.parent.getRepresentation(), measurement);
        String json = gson.toJson(msg, CoordinatorMessage.class);
        stateModel.edgeNetworkSocket.write(new DatagramPacket(json.getBytes(), json.length(), new InetSocketAddress(coordinator.getIpAddr(), coordinator.getNodesPort())));
        synchronized (stateModel.coordinatorACKLock){
            stateModel.setAwaitingCoordinatorACK(true);
            try{stateModel.coordinatorACKLock.wait(3000);} catch (InterruptedException e){e.printStackTrace();}
        }
        if(stateModel.isAwaitingCoordinatorACK() && !stateModel.shutdown){
            System.out.println("DEBUG: CoordinatorUpdatesThread - Il coordinatore è morto!");
            cached = measurement;
            stateModel.setCoordinator(null);
            stateModel.setAwaitingCoordinatorACK(false);
            synchronized (stateModel.electionStatusLock){
                if(stateModel.electionStatus != FINISHED)
                    return;
                stateModel.electionStatus = STARTED;
            }
            stateModel.parent.bullyElection();
        }
    }
}
