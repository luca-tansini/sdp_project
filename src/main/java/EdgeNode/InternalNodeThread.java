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

        System.out.println("Starting internal node work!");

        Object childLock = new Object();

        //Fa partire thread di timeout che scrive al parent
        new Thread(new Runnable() {
            @Override
            public void run() {

                //HashMap in cui metto i timestamp delle statistiche più recenti già usate
                HashMap<String,Long> sentTimestamps = new HashMap<>();

                while(true){
                    synchronized (childLock){
                        try {
                            childLock.wait(5000);
                        } catch(InterruptedException e){
                            e.printStackTrace();
                        }
                    }
                    if(!stateModel.isInternalNode) break;

                    EdgeNodeRepresentation parent = stateModel.getNetworkTreeParent();

                    //Manda i pacchetti arretrati
                    if(cached != null && parent != null){
                        String tmp = cached;
                        cached = null;
                        sendMeasurement(parent, tmp);
                    }

                    HashMap<String, Measurement> statsLocal = new HashMap<>();

                    synchronized(stateModel.statsLock) {
                        //Calcola media con tutte le childLocalMeans più recenti
                        double mean = 0;
                        int count = 0;
                        for(String id : stateModel.childLocalMeans.keySet()) {
                            Measurement m = stateModel.childLocalMeans.get(id);
                            Long timestamp = sentTimestamps.get(id);
                            if(timestamp == null || timestamp < m.getTimestamp()){
                                mean += m.getValue();
                                count++;
                                statsLocal.put(id, m);
                                sentTimestamps.put(id,m.getTimestamp());
                            }
                        }
                        if(count != 0) {
                            mean /= count;
                            stateModel.localMean = new Measurement(""+stateModel.edgeNode.getNodeId(), "localMean", mean, Instant.now().toEpochMilli());
                        }
                        //Aggiunge a statsLocal tutte le misurazioni dei figli dei figli più recenti (per farle arrivare al server)
                        for(String id : stateModel.stats.getLocal().keySet()) {
                            Measurement m = stateModel.stats.getLocal().get(id);
                            Long timestamp = sentTimestamps.get(id);
                            if(timestamp == null || timestamp < m.getTimestamp()){
                                statsLocal.put(id, m);
                                sentTimestamps.put(id,m.getTimestamp());
                            }
                        }
                    }

                    //Invia gli update al parent (solo se ha calcolato qualcosa di nuovo)
                    if(statsLocal.size() > 0) {

                        //Aggiorna il riferimento al parent per sicurezza
                        parent = stateModel.getNetworkTreeParent();

                        ParentMessage parentMessage = new ParentMessage(ParentMessage.ParentMessageType.STATS_UPDATE, stateModel.edgeNode.getRepresentation(), stateModel.localMean, statsLocal);
                        String json = gson.toJson(parentMessage);

                        if(parent != null)
                            sendMeasurement(parent, json);
                        else
                            cached = json;
                    }
                }
            }
        }).start();

        //Legge gli update e risponde
        while (stateModel.isInternalNode){
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

            //Aggiunge alle statistiche locali i dati del nodo child per essere processati
            synchronized (stateModel.statsLock) {
                stateModel.childLocalMeans.put(msg.getLocalmean().getId(),msg.getLocalmean());
                stateModel.stats.getLocal().putAll(msg.getLocal());
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
            cached = json;

            //Se il mio parent era anche il coordinatore faccio partire elezioni
            if(stateModel.getNetworkTreeParent().equals(stateModel.getCoordinator())){
                stateModel.setCoordinator(null);
                stateModel.setAwaitingParentACK(false);
                synchronized (stateModel.electionStatusLock){
                    if(stateModel.electionStatus != FINISHED)
                        return;
                    stateModel.electionStatus = STARTED;
                }
                System.out.println("DEBUG: InternalNodeThread - il mio parent era anche il coordinatore, faccio partire elezioni");
                stateModel.edgeNode.bullyElection();
            }
            //Altrimenti comunica al coordinatore che è morto il parent (se c'è un coordinatore, altrimenti c'è un'elezione in corso)
            else {
                if (stateModel.getCoordinator() != null) {
                    String parentDownJson = gson.toJson(new TreeMessage(TreeMessage.TreeMessageType.PARENT_DOWN, parent, stateModel.edgeNode.getRepresentation()));
                    stateModel.edgeNetworkSocket.write(new DatagramPacket(parentDownJson.getBytes(), parentDownJson.length(), new InetSocketAddress(stateModel.getCoordinator().getIpAddr(), stateModel.getCoordinator().getNodesPort())));
                }
            }
        }
    }

}
