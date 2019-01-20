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
 * ParentUpdatesThread definisce la comunicazione con i sensori.
 * Si tratta di un thread che:
 *      - legge le misurazioni dei sensori
 *      - ogni 40 misurazioni lette (con sliding windows overlap 50%) fa la media e le invia al parent
 *      - se non riceve ACK dal parent lo dichiara morto e chiede al coordinatore il nuovo parent
 *      - se il parent era anche il coordinatore fa partire delle elezioni
 *      - mentre attende l'aggiornamento del parent può continuare a consumare le misurazioni dei sensori
 *        e tiene la media più recente in cache per inviarla nonappena sia disponibile un parent
 *
 * Se il nodo è un nodo interno non invia direttamente le misurazioni al parent,
 * è sufficiente metterle in stats.local, sarà poi l'InternalNodeThread a processarle e inviarle al parent
 *
 */
public class ParentUpdatesThread extends Thread {

    private StateModel stateModel;
    private ArrayList<Measurement> buffer= new ArrayList();
    private Gson gson = new Gson();
    private String cached;

    public ParentUpdatesThread(){
        this.stateModel = StateModel.getInstance();
    }

    @Override
    public void run() {

        while (stateModel.sensorCommunicationOnline){

            EdgeNodeRepresentation parent = stateModel.getNetworkTreeParent();

            if(stateModel.isInternalNode && cached != null) {
                synchronized (stateModel.statsLock){
                    ParentMessage msg = gson.fromJson(cached, ParentMessage.class);
                    stateModel.partialMean.put(msg.getMeasurement().getId(),msg.getMeasurement());
                }
                cached = null;
            }
            if(parent != null && cached != null) {
                String tmp = cached;
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


                // Aggiorna nel model la statistica locale
                synchronized (stateModel.statsLock){
                    stateModel.stats.getLocal().put(measurement.getId(),measurement);
                    // Se sono un nodo interno metto la media locale in partialMean
                    // così verrà usata da InternalNodeThread per calcolare la media successiva
                    if(stateModel.isInternalNode)
                        stateModel.partialMean.put(measurement.getId(), measurement);
                }

                // Se non sono un nodo interno mando le statistiche al parent via rete
                if(!stateModel.isInternalNode){
                    ParentMessage msg = new ParentMessage(ParentMessage.ParentMessageType.STATS_UPDATE, stateModel.edgeNode.getRepresentation(), measurement, stateModel.stats.getLocal());
                    String json = gson.toJson(msg);
                    if (parent != null) {
                        sendMeasurement(parent, json);
                    } else {
                        //Non ha senso avere più di un valore cachato
                        this.cached = json;
                    }
                }

                //Sliding window, 50% overlap
                for(int i=0; i<20; i++)
                    buffer.remove(0);

            }
        }
    }

    private void sendMeasurement(EdgeNodeRepresentation parent, String json){
        stateModel.edgeNetworkSocket.write(new DatagramPacket(json.getBytes(), json.length(), new InetSocketAddress(parent.getIpAddr(), parent.getNodesPort())));
        synchronized (stateModel.parentACKLock){
            stateModel.setAwaitingParentACK(true);
            try{stateModel.parentACKLock.wait(3000);} catch (InterruptedException e){e.printStackTrace();}
        }
        if(stateModel.isAwaitingParentACK() && stateModel.sensorCommunicationOnline){
            System.out.println("DEBUG: ParentUpdatesThread - Il parent è morto!");
            stateModel.setNetworkTreeParent(null);
            stateModel.nodes.remove(parent);
            cached = json;

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
