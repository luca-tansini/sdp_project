package EdgeNode;

import EdgeNode.EdgeNetworkMessage.CoordinatorMessage;
import Sensor.Measurement;
import ServerCloud.Model.Statistics;
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
    private Measurement global;
    private HashMap<String,Measurement> locals;
    private Object statsLock = new Object();

    public CoordinatorThread() {
        this.stateModel = StateModel.getInstance();
        //Il global iniziale ha due valori non significativi
        this.global = new Measurement("coordinator"+stateModel.parent.getNodeId(), "global",0,0);
        this.locals = new HashMap<>();
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

                    System.out.println("DEBUG: CoordinatorThread - Coordinatore si sveglia per inviare update");

                    HashMap<String, ArrayList<Measurement>> statsLocal = new HashMap<>();

                    synchronized(statsLock) {
                        //Calcola media con tutte le statistiche locali più recenti
                        double mean = 0;
                        int count = 0;
                        for(String id : locals.keySet()) {
                            Measurement m = locals.get(id);
                            Long timestamp = sentTimestamps.get(id);
                            if(timestamp == null || timestamp < m.getTimestamp()){
                                mean += m.getValue();
                                count++;
                                ArrayList l = new ArrayList<>();
                                l.add(m);
                                statsLocal.put(id, l);
                                sentTimestamps.put(id,m.getTimestamp());
                            }
                        }
                        if(count != 0) {
                            mean /= count;
                            global = new Measurement("coordinator" + stateModel.parent.getNodeId(), "global", mean, Instant.now().toEpochMilli());
                        }
                    }

                    //Invia gli update al server (solo se ha calcolato qualcosa di nuovo)
                    if(statsLocal.size() > 0) {
                        Statistics stats = new Statistics();
                        stats.setLocal(statsLocal);
                        ArrayList l = new ArrayList();
                        l.add(global);
                        stats.setGlobal(l);

                        System.out.println("DEBUG: CoordinatorThread - STATS:");
                        System.out.println(stats);

                        String json = gson.toJson(stats);
                        System.out.println("DEBUG: CoordinatorThread - GSON:\n"+json);

                        ClientResponse response = webResource.type("application/json").post(ClientResponse.class, json);
                        if(response.getStatus() != 200){
                            System.out.println("DEBUG: CoordinatorThread - Server Response: "+response.getEntity(String.class));
                        }

                    }
                    else{
                        System.out.println("DEBUG: CoordinatorThread - Non avevo niente di nuovo d inviare");
                    }
                }
            }
        }).start();

        //Legge gli update e risponde
        while (!stateModel.shutdown){
            CoordinatorMessage msg = stateModel.coordinatorBuffer.take();
            System.out.println("Coordinator msg: "+msg.getMeasurement());
            synchronized (statsLock) {
                this.locals.put(msg.getMeasurement().getId(), msg.getMeasurement());
            }
            CoordinatorMessage response = new CoordinatorMessage(CoordinatorMessage.CoordinatorMessageType.ACK, stateModel.parent.getRepresentation(), global);
            String json = gson.toJson(response, CoordinatorMessage.class);
            stateModel.edgeNetworkSocket.write(new DatagramPacket(json.getBytes(), json.length(), new InetSocketAddress(msg.getSender().getIpAddr(), msg.getSender().getNodesPort())));
        }
    }
}
