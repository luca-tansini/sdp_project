package EdgeNode;

import EdgeNode.EdgeNetworkMessage.ParentMessage;
import Sensor.Measurement;
import ServerCloud.Model.Statistics;
import com.google.gson.Gson;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;

import java.net.DatagramPacket;
import java.net.InetSocketAddress;
import java.time.Instant;
import java.util.HashMap;

/*
 * CoordinatorThread definisce il comportamento del coordinatore che aggrega le statistiche dei nodi figli e le manda al server
 * Si tratta di due thread innestati:
 *
 *      - Il principale si occupa di:
 *          - lanciare e fermare il thread secondario
 *          - leggere i messaggi che arrivano dalla rete dei nodi edge e rispondere con un ACK contenente la statistica globale più recente
 *
 *      - Il secondario si occupa di:
 *          - calcolare ogni 5 secondi la media delle statistiche più recenti ed inviarla al parent (o al server se è la radice)
 */
public class CoordinatorThread extends InternalNodeThread{

    private StateModel stateModel;

    public CoordinatorThread() {
        this.stateModel = StateModel.getInstance();
    }

    public void run(){

        Gson gson = new Gson();

        Object childLock = new Object();

        //Fa partire thread di timeout che scrive al server
        new Thread(new Runnable() {
            @Override
            public void run() {

                // HashMap in cui metto i timestamp delle statistiche più recenti già usate
                HashMap<String,Long> sentLocalTimestamps = new HashMap<>();
                HashMap<String,Long> sentPartialMeanTimestamps = new HashMap<>();

                while(true){
                    synchronized (childLock){
                        try {
                            childLock.wait(5000);
                        } catch(InterruptedException e){
                            e.printStackTrace();
                        }
                    }
                    if(!stateModel.isInternalNode) break;

                    HashMap<String, Measurement> statsLocal = new HashMap<>();
                    Measurement global = null;

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
                            global = new Measurement("coordinator"+stateModel.edgeNode.getNodeId(), "global", mean, Instant.now().toEpochMilli());
                            stateModel.stats.setGlobal(global);
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

                    //Invia gli update al server (solo se ha calcolato qualcosa di nuovo)
                    if(statsLocal.size() > 0) {
                        Statistics stats = new Statistics();
                        stats.setLocal(statsLocal);
                        if(global == null) System.out.println("DEBUG: CoordinatorThread - Mi sbagliavo, global poteva essere null");
                        stats.setGlobal(global);
                        String json = gson.toJson(stats);
                        Client client = Client.create();
                        WebResource webResource = client.resource("http://localhost:4242/edgenetwork/statistics");
                        webResource.type("application/json").post(ClientResponse.class, json);
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

            // Aggiunge a partialMean la misurazione del nodo child
            // E a local i dati locali dei singoli nodi da inoltrare
            synchronized (stateModel.statsLock) {
                stateModel.partialMean.put(msg.getMeasurement().getId(),msg.getMeasurement());
                if(msg.getLocal() != null)
                    stateModel.stats.getLocal().putAll(msg.getLocal());
            }
        }
    }
}
