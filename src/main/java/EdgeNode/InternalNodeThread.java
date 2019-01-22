package EdgeNode;

import EdgeNode.EdgeNetworkMessage.ParentMessage;
import EdgeNode.EdgeNetworkMessage.TreeMessage;
import Sensor.Measurement;
import ServerCloud.Model.EdgeNodeRepresentation;
import com.google.gson.Gson;

import java.net.DatagramPacket;
import java.net.InetSocketAddress;
import java.time.Instant;
import java.util.HashMap;

import static EdgeNode.StateModel.ElectionStatus.FINISHED;
import static EdgeNode.StateModel.ElectionStatus.STARTED;

/*
 * ParentUpdatesThread definisce il comportamento dei nodi interni (non coordinatore) che aggregano le statistiche dei nodi figli.
 * Si tratta di due thread innestati:
 *
 *      - Il principale si occupa di:
 *          - lanciare e fermare il thread secondario
 *          - leggere i messaggi che arrivano dalla rete dei nodi edge e rispondere con un ACK contenente la statistica globale più recente
 *
 *      - Il secondario si occupa di:
 *          - calcolare ogni 5 secondi la media delle statistiche più recenti ed inviarla al parent (o al server se è la radice)
 */
public class InternalNodeThread extends Thread{

    private StateModel stateModel;

    //Tiene in memoria il pacchetto più recente che non è ancora riuscito a mandare
    private String cached = null;
    private Gson gson = new Gson();

    public InternalNodeThread() {
        this.stateModel = StateModel.getInstance();
    }

    public void run(){

        Object childLock = new Object();

        // Fa partire thread di timeout che scrive al parent
        new Thread(new Runnable() {
            @Override
            public void run() {

                // HashMap in cui metto i timestamp delle statistiche più recenti già usate
                HashMap<String,Long> sentLocalTimestamps = new HashMap<>();
                HashMap<String,Long> sentPartialMeanTimestamps = new HashMap<>();

                while(true){

                    EdgeNodeRepresentation parent = stateModel.getNetworkTreeParent();

                    // Manda i pacchetti arretrati (prima di dormire)
                    if(cached != null && parent != null){
                        // Aspetta un secondo a mandare il pacchetto cachato per dare tempo al parent
                        // di far partire l'InternalNodeThread nel caso sia stato appena promosso
                        try{Thread.sleep(1000);} catch (InterruptedException e){e.printStackTrace();}
                        String tmp = cached;
                        cached = null;
                        sendMeasurement(parent, tmp);
                    }

                    synchronized (childLock){
                        try {
                            childLock.wait(5000);
                        } catch(InterruptedException e){
                            e.printStackTrace();
                        }
                    }
                    if(!stateModel.isInternalNode()) break;

                    HashMap<String, Measurement> statsLocal = new HashMap<>();
                    Measurement partialMean = null;

                    synchronized(stateModel.statsLock) {
                        // Calcola media con tutte le PartialMean più recenti
                        double mean = 0;
                        int count = 0;
                        for(String id : stateModel.partialMean.keySet()) {
                            Measurement m = stateModel.partialMean.get(id);
                            Long timestamp = sentPartialMeanTimestamps.get(id);
                            if(timestamp == null || timestamp < m.getTimestamp()){
                                mean += m.getValue();
                                count++;
                                sentPartialMeanTimestamps.put(id,m.getTimestamp());
                            }
                        }
                        if(count != 0) {
                            mean /= count;
                            partialMean = new Measurement(""+stateModel.edgeNode.getNodeId(), "partialMean", mean, Instant.now().toEpochMilli());
                        }
                        // Aggiunge a statsLocal tutte le misurazioni locali più recenti (per farle arrivare al server)
                        for(String id : stateModel.stats.getLocal().keySet()) {
                            Measurement m = stateModel.stats.getLocal().get(id);
                            Long timestamp = sentLocalTimestamps.get(id);
                            if(timestamp == null || timestamp < m.getTimestamp()){
                                statsLocal.put(id, m);
                                sentLocalTimestamps.put(id,m.getTimestamp());
                            }
                        }
                    }

                    //Invia gli update al parent (solo se ha qualcosa di nuovo) (se ho delle nuove statsLocal ho di sicuro ricalcolato la media)
                    if(statsLocal.size() > 0) {
                        ParentMessage parentMessage = new ParentMessage(ParentMessage.ParentMessageType.STATS_UPDATE, stateModel.edgeNode.getRepresentation(), partialMean, statsLocal);
                        String json = gson.toJson(parentMessage);

                        //Aggiorna il riferimento al parent per sicurezza
                        parent = stateModel.getNetworkTreeParent();
                        if(parent != null)
                            sendMeasurement(parent, json);
                        else
                            cached = json;
                    }
                }
            }
        }).start();

        //Legge gli update e risponde
        while (stateModel.isInternalNode()){
            ParentMessage msg = stateModel.internalNodeBuffer.take();
            if(msg.getParentMessageType() == ParentMessage.ParentMessageType.QUIT) {
                synchronized (childLock){
                    childLock.notify();
                }
                break;
            }

            //Risponde con la media globale più recente che conosce
            ParentMessage response = new ParentMessage(ParentMessage.ParentMessageType.ACK, stateModel.edgeNode.getRepresentation(), stateModel.stats.getGlobal());
            String json = gson.toJson(response, ParentMessage.class);
            stateModel.edgeNetworkSocket.write(new DatagramPacket(json.getBytes(), json.length(), new InetSocketAddress(msg.getSender().getIpAddr(), msg.getSender().getNodesPort())));

            // Aggiunge a partialMean la misurazione del nodo child
            // E a local i dati locali dei singoli nodi da inoltrare
            synchronized (stateModel.statsLock) {
                stateModel.partialMean.put(msg.getMeasurement().getId(),msg.getMeasurement());
                if(msg.getLocal() != null)
                    stateModel.stats.getLocal().putAll(msg.getLocal());
                else
                    stateModel.stats.getLocal().put(msg.getMeasurement().getId(),msg.getMeasurement());
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
            System.out.println("DEBUG: InternalNodeThread - Il parent è morto!");
            stateModel.setNetworkTreeParent(null);
            stateModel.setAwaitingParentACK(false);
            stateModel.nodes.remove(parent);
            cached = json;

            //Se il mio parent era anche il coordinatore faccio partire elezioni
            if(parent.equals(stateModel.getCoordinator())){
                stateModel.setCoordinator(null);
                synchronized (stateModel.electionStatusLock){
                    if(stateModel.electionStatus != FINISHED)
                        return;
                    stateModel.electionStatus = STARTED;
                }
                System.out.println("DEBUG: InternalNodeThread - il mio parent era anche il coordinatore, faccio partire elezioni");
                stateModel.edgeNode.bullyElection();
            }
            //Altrimenti comunica al coordinatore che è morto il parent
            else{
                String parentDownJson = gson.toJson(new TreeMessage(TreeMessage.TreeMessageType.PARENT_DOWN, parent, stateModel.edgeNode.getRepresentation()));
                stateModel.edgeNetworkSocket.write(new DatagramPacket(parentDownJson.getBytes(), parentDownJson.length(), new InetSocketAddress(stateModel.getCoordinator().getIpAddr(), stateModel.getCoordinator().getNodesPort())));
                // Devo controllare che anche il coordinatore non sia morto, perchè
                // nel caso in cui mio padre sia morto subito prima del coordinatore potrei finire in deadlock
                synchronized (stateModel.coordinatorACKLock) {
                    stateModel.setAwaitingCoordinatorACK(true);
                    try {
                        stateModel.coordinatorACKLock.wait(5000);
                    } catch (InterruptedException e) {e.printStackTrace();}
                }
                if(stateModel.isAwaitingCoordinatorACK()){
                    System.out.print("DEBUG: InternalNodeThread - Anche il coordinatore è morto");
                    stateModel.setCoordinator(null);
                    synchronized (stateModel.electionStatusLock){
                        if(stateModel.electionStatus != FINISHED) {
                            System.out.println(", ma ho già delle elezioni in corso");
                            return;
                        }
                        stateModel.electionStatus = STARTED;
                    }
                    System.out.println(", faccio partire elezioni");
                    stateModel.edgeNode.bullyElection();
                }
            }
        }
    }
}
