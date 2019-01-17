package Sensor;

import ServerCloud.Model.EdgeNodeRepresentation;
import ServerCloud.Model.Position;
import com.google.gson.Gson;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;

import java.util.Random;

import static com.sun.jersey.api.Responses.NOT_FOUND;

public class SensorThread extends Thread {

    private String serverAddr;
    private Position position;
    private PM10SimulatorStream stream;
    private PM10Simulator simulator;

    public SensorThread(String serverAddr){
        this.serverAddr = serverAddr;
        Random rng = new Random();
        this.position = new Position(rng.nextInt(100), rng.nextInt(100));
    }

    public void run() {

        Gson gson = new Gson();

        //Si collega al server per chiedere il nodo più vicino
        Client client = Client.create();
        WebResource webResource = client.resource("http://"+serverAddr+":4242/sensor/getnearestnode");
        ClientResponse response = webResource.type("application/json").post(ClientResponse.class, gson.toJson(position));
        EdgeNodeRepresentation targetNode = null;
        if(response.getStatus() != NOT_FOUND)
            targetNode = gson.fromJson(response.getEntity(String.class),EdgeNodeRepresentation.class);

        //Busy waiting (concessa) finchè non trova un nodo edge
        while (targetNode == null){
            try{Thread.sleep(1000);} catch (InterruptedException e){e.printStackTrace();}
            response = webResource.type("application/json").post(ClientResponse.class, gson.toJson(position));
            if(response.getStatus() != NOT_FOUND)
                targetNode = gson.fromJson(response.getEntity(String.class),EdgeNodeRepresentation.class);
        }

        //Costruisce lo stream di cui mantiene un riferimento per aggiornare il nodo più vicino
        stream = new PM10SimulatorStream(targetNode, position, serverAddr);

        //Lancia il PM10 simulator
        simulator = new PM10Simulator(stream);
        simulator.start();

        //Inizia un loop infinito in cui ogni 10 secondi circa chiede al server chi sia il nodo più vicino e aggiorna lo stream
        //Nel caso in cui non ci siano nodi disponibili lascia che sia il sensore ad accorgersene
        while(true){
            try{Thread.sleep(10000);} catch (InterruptedException e){e.printStackTrace();}
            response = webResource.type("application/json").post(ClientResponse.class, gson.toJson(position));
            if(response.getStatus() != NOT_FOUND) {
                targetNode = gson.fromJson(response.getEntity(String.class),EdgeNodeRepresentation.class);
                if(!targetNode.equals(stream.getTargetNode())) {
                    stream.updateTargetNode(targetNode);
                }
            }
        }
    }
}
