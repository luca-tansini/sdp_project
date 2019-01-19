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
                    if(stateModel.shutdown) break;

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
                            stateModel.localMean = new Measurement("coordinator"+stateModel.edgeNode.getNodeId(), "global", mean, Instant.now().toEpochMilli());
                            stateModel.stats.setGlobal(stateModel.localMean);
                        }
                        //Aggiunge a statsLocal tutte le misurazioni locali (tranne quelle dei figli diretti che stanno in childLocalMeans) per farle arrivare al server
                        for(String id : stateModel.stats.getLocal().keySet()) {
                            Measurement m = stateModel.stats.getLocal().get(id);
                            Long timestamp = sentTimestamps.get(id);
                            if(timestamp == null || timestamp < m.getTimestamp()){
                                statsLocal.put(id, m);
                                sentTimestamps.put(id,m.getTimestamp());
                            }
                        }
                        //Aggiunge la sua stessa statistica locale alle statistiche locali da inviare al server
                        statsLocal.put(""+stateModel.edgeNode.getNodeId(), stateModel.localMean);
                    }

                    //Invia gli update al server (solo se ha calcolato qualcosa di nuovo)
                    if(statsLocal.size() > 0) {
                        Statistics stats = new Statistics();
                        stats.setLocal(statsLocal);
                        stats.setGlobal(stateModel.stats.getGlobal());
                        String json = gson.toJson(stats);
                        Client client = Client.create();
                        WebResource webResource = client.resource("http://localhost:4242/edgenetwork/statistics");
                        webResource.type("application/json").post(ClientResponse.class, json);
                    }
                }
            }
        }).start();

        //Legge gli update e risponde
        while (!stateModel.shutdown){
            ParentMessage msg = stateModel.internalNodeBuffer.take();
            if(msg.getParentMessageType() == ParentMessage.ParentMessageType.QUIT) {
                synchronized (childLock){
                    childLock.notify();
                }
                break;
            }

            //Risponde con la media globale più recente
            ParentMessage response = new ParentMessage(ParentMessage.ParentMessageType.ACK, stateModel.edgeNode.getRepresentation(), stateModel.stats.getGlobal());
            String json = gson.toJson(response, ParentMessage.class);
            stateModel.edgeNetworkSocket.write(new DatagramPacket(json.getBytes(), json.length(), new InetSocketAddress(msg.getSender().getIpAddr(), msg.getSender().getNodesPort())));

            //Aggiunge alle statistiche locali i dati del nodo child per essere processati
            synchronized (stateModel.statsLock) {
                stateModel.childLocalMeans.put(msg.getLocalmean().getId(),msg.getLocalmean());
                if(msg.getLocal() != null)
                    stateModel.stats.getLocal().putAll(msg.getLocal());
            }
        }
    }
}
