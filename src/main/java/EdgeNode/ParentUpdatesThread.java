package EdgeNode;

import EdgeNode.EdgeNetworkMessage.ParentMessage;
import EdgeNode.EdgeNetworkMessage.TreeMessage;
import Sensor.Measurement;
import ServerCloud.Model.EdgeNodeRepresentation;
import com.google.gson.Gson;

import java.net.DatagramPacket;
import java.net.InetSocketAddress;
import java.time.Instant;
import java.util.ArrayList;

import static EdgeNode.StateModel.ElectionStatus.FINISHED;
import static EdgeNode.StateModel.ElectionStatus.STARTED;

/*
 * ParentUpdatesThread definisce il comportamento dei nodi foglia che comunicano con i sensori.
 * Si tratta di un thread che:
 *      - legge le misurazioni dei sensori
 *      - ogni 40 misurazioni lette (con sliding windows overlap 50%) fa la media e le invia al parent
 *      - se non riceve ACK dal parent lo dichiara morto e chiede al coordinatore il nuovo parent
 *      - se il parent era anche il coordinatore fa partire delle elezioni
 *      - mentre attende l'aggiornamento del parent può continuare a consumare le misurazioni dei sensori
 *        e tiene la media più recente in cache per inviarla nonappena sia disponibile un parent
 */
public class ParentUpdatesThread extends Thread {

    private StateModel stateModel;
    private ArrayList<Measurement> buffer= new ArrayList();
    private Gson gson = new Gson();
    private Measurement cached;

    public ParentUpdatesThread(){
        this.stateModel = StateModel.getInstance();
    }

    @Override
    public void run() {

        while (stateModel.sensorCommunicationOnline){

            EdgeNodeRepresentation parent = stateModel.getNetworkTreeParent();
            //Se il mio parent è null vuol dire che sono sia radice che foglia (unico nodo) e mi mando gli update da solo
            if(parent == null && stateModel.edgeNode.isCoordinator())
                parent = stateModel.edgeNode.getRepresentation();

            if(parent != null && cached != null) {
                Measurement tmp = cached;
                cached = null;
                sendMeasurement(parent, tmp);
            }

            Measurement tmp = stateModel.sensorsMeasurementBuffer.take();
            if(tmp.getType().equals("quit"))
                break;
            buffer.add(tmp);
            if(buffer.size() == 40){
                double mean = 0;
                for(Measurement m: buffer)
                    mean += m.getValue();
                Measurement measurement = new Measurement(stateModel.edgeNode.getNodeId()+"", "local", mean/40, Instant.now().toEpochMilli());

                if(parent != null) {
                    sendMeasurement(parent, measurement);
                }
                else{
                    //Non ha senso avere più di un valore cachato
                    this.cached = measurement;
                }

                //Aggiorna nel model la statistica locale
                synchronized (stateModel.statsLock){
                    stateModel.localMean = measurement;
                }

                //Sliding window, 50% overlap
                for(int i=0; i<20; i++)
                    buffer.remove(0);

            }
        }
    }

    private void sendMeasurement(EdgeNodeRepresentation parent, Measurement measurement){
        ParentMessage msg = new ParentMessage(ParentMessage.ParentMessageType.STATS_UPDATE, stateModel.edgeNode.getRepresentation(), measurement);
        String json = gson.toJson(msg);
        stateModel.edgeNetworkSocket.write(new DatagramPacket(json.getBytes(), json.length(), new InetSocketAddress(parent.getIpAddr(), parent.getNodesPort())));
        synchronized (stateModel.parentACKLock){
            stateModel.setAwaitingParentACK(true);
            try{stateModel.parentACKLock.wait(3000);} catch (InterruptedException e){e.printStackTrace();}
        }
        if(stateModel.isAwaitingParentACK() && stateModel.sensorCommunicationOnline){
            System.out.println("DEBUG: ParentUpdatesThread - Il parent è morto!");
            stateModel.setNetworkTreeParent(null);
            cached = measurement;

            //Se il mio parent era anche il coordinatore fccio partire elezioni
            if(parent.equals(stateModel.getCoordinator())){
                stateModel.setCoordinator(null);
                stateModel.setAwaitingParentACK(false);
                synchronized (stateModel.electionStatusLock){
                    if(stateModel.electionStatus != FINISHED)
                        return;
                    stateModel.electionStatus = STARTED;
                }
                System.out.println("DEBUG: ParentUpdatesThread - il mio parent era anche il coordinatore, faccio partire elezioni");
                stateModel.edgeNode.bullyElection();
            }
            //Altrimenti comunica al coordinatore che è morto il parent
            else{
                String parentDownJson = gson.toJson(new TreeMessage(TreeMessage.TreeMessageType.PARENT_DOWN, parent, stateModel.edgeNode.getRepresentation()));
                stateModel.edgeNetworkSocket.write(new DatagramPacket(parentDownJson.getBytes(), parentDownJson.length(), new InetSocketAddress(stateModel.getCoordinator().getIpAddr(), stateModel.getCoordinator().getNodesPort())));
            }
        }
    }
}
