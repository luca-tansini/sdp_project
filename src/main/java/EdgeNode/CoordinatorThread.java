package EdgeNode;

import EdgeNode.EdgeNetworkMessage.CoordinatorMessage;
import Sensor.Measurement;
import ServerCloud.Model.Statistics;
import ServerCloud.Model.StatisticsHistory;
import com.google.gson.Gson;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;

import java.net.DatagramPacket;
import java.net.InetSocketAddress;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;

//TODO: gestione statistiche
public class CoordinatorThread extends Thread{

    private StateModel stateModel;

    public CoordinatorThread() {
        this.stateModel = StateModel.getInstance();
    }

    public void run(){

        System.out.println("Starting coordinator work!");

        Gson gson = new Gson();

        //Fa partire thread di timeout che scrive al server
        new Thread(new Runnable() {
            @Override
            public void run() {

                //HashMap in cui metto i timestamp delle statistiche più recenti già usate
                HashMap<String,Long> sentTimestamps = new HashMap<>();
                Client client = Client.create();
                WebResource webResource = client.resource("http://localhost:4242/edgenetwork/statistics");

                while(!stateModel.shutdown){
                    try{Thread.sleep(5000);} catch (InterruptedException e){e.printStackTrace();}

                    HashMap<String, Measurement> statsLocal = new HashMap<>();

                    synchronized(stateModel.statsLock) {
                        //Calcola media con tutte le statistiche locali più recenti
                        double mean = 0;
                        int count = 0;
                        for(String id : stateModel.stats.getLocal().keySet()) {
                            Measurement m = stateModel.stats.getLocal().get(id);
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
                            stateModel.stats.setGlobal(new Measurement("coordinator" + stateModel.parent.getNodeId(), "global", mean, Instant.now().toEpochMilli()));
                        }
                    }

                    //Invia gli update al server (solo se ha calcolato qualcosa di nuovo)
                    if(statsLocal.size() > 0) {
                        Statistics stats = new Statistics();
                        stats.setLocal(statsLocal);
                        stats.setGlobal(stateModel.stats.getGlobal());
                        String json = gson.toJson(stats);
                        webResource.type("application/json").post(ClientResponse.class, json);
                    }
                }
            }
        }).start();

        //Legge gli update e risponde
        while (!stateModel.shutdown){
            CoordinatorMessage msg = stateModel.coordinatorBuffer.take();
            synchronized (stateModel.statsLock) {
                stateModel.stats.getLocal().put(msg.getMeasurement().getId(), msg.getMeasurement());
            }
            CoordinatorMessage response = new CoordinatorMessage(CoordinatorMessage.CoordinatorMessageType.ACK, stateModel.parent.getRepresentation(), stateModel.stats.getGlobal());
            String json = gson.toJson(response, CoordinatorMessage.class);
            stateModel.edgeNetworkSocket.write(new DatagramPacket(json.getBytes(), json.length(), new InetSocketAddress(msg.getSender().getIpAddr(), msg.getSender().getNodesPort())));
        }
    }
}
