package Sensor;

import EdgeNode.GRPC.SensorsGRPCInterfaceGrpc;
import EdgeNode.GRPC.SensorsGRPCInterfaceOuterClass;
import ServerCloud.Model.EdgeNodeRepresentation;
import ServerCloud.Model.Position;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;

import java.util.concurrent.TimeUnit;

import static com.sun.jersey.api.Responses.NOT_FOUND;

public class PM10SimulatorStream implements SensorStream  {

    private Position position;
    private String serverAddr;
    private EdgeNodeRepresentation targetNode;
    private Object lock = new Object();

    ManagedChannel channel;
    StreamObserver<SensorsGRPCInterfaceOuterClass.Measurement> requestStream;


    public PM10SimulatorStream(EdgeNodeRepresentation targetNode, Position position, String serverAddr) {
        this.position = position;
        this.serverAddr = serverAddr;
        this.targetNode = targetNode;
        setUpCommunication();
    }

    public EdgeNodeRepresentation getTargetNode() {
        synchronized (lock) {
            return targetNode;
        }
    }

    //Repeatedly asks CloudServer for nearestNode
    private void findTargetNode(){
        System.out.println("DEBUG: PM10SimulatorStream"+position+" - trying to find nearest node");
        Client client = Client.create();
        WebResource webResource = client.resource("http://"+serverAddr+":4242/sensor/getnearestnode");
        ClientResponse response = webResource.type("application/json").post(ClientResponse.class, position);
        EdgeNodeRepresentation targetNode = null;
        if(response.getStatus() != NOT_FOUND)
            targetNode = response.getEntity(EdgeNodeRepresentation.class);

        //Busy waiting (concessa) finchè non trova un nodo edge
        while (targetNode == null){
            try{Thread.sleep(1000);} catch (InterruptedException e){e.printStackTrace();}
            System.out.println("DEBUG: PM10SimulatorStream"+position+" - trying to find nearest node");
            response = webResource.type("application/json").post(ClientResponse.class, position);
            if(response.getStatus() != NOT_FOUND)
                targetNode = response.getEntity(EdgeNodeRepresentation.class);
        }

        if(this.getTargetNode() == null || !this.getTargetNode().equals(targetNode)) {
            System.out.println("DEBUG: PM10SimulatorStream" + position + " - found nearest node: " + targetNode);
            updateTargetNode(targetNode);
        }
    }

    //Aggiorna il nodo edge con cui comunica
    public void updateTargetNode(EdgeNodeRepresentation newTargetNode){
        synchronized (lock){

            //Chiude la comunicazione precedente
            if(requestStream != null) {
                requestStream.onCompleted();
                requestStream = null;
            }
            if(channel != null) {
                channel.shutdown();
                try{
                    if(!channel.awaitTermination(2, TimeUnit.SECONDS))
                        System.out.println("DEBUG: PM10SimulatorStream"+position+" - awaitTermination returned false");
                } catch (InterruptedException e){e.printStackTrace();}
                channel = null;
            }

            //Inizia comunicazione nuova
            targetNode = newTargetNode;
            setUpCommunication();
        }
    }

    private void setUpCommunication(){
        channel = ManagedChannelBuilder.forTarget(targetNode.getIpAddr()+":"+targetNode.getSensorsPort()).usePlaintext(true).build();
        SensorsGRPCInterfaceGrpc.SensorsGRPCInterfaceStub stub = SensorsGRPCInterfaceGrpc.newStub(channel);
        requestStream = stub.sendMeasurement(new StreamObserver<SensorsGRPCInterfaceOuterClass.Null>() {
            @Override
            public void onNext(SensorsGRPCInterfaceOuterClass.Null value){}

            @Override
            public void onError(Throwable t) {
                System.out.println("DEBUG THROWABLE: "+t.getMessage());
                //Se arriva un errore è perchè il server è morto e ha chiuso lo stream
                synchronized (lock){
                    System.out.println("DEBUG: PM10SimulatorStream"+position+" - targetNode died!");
                    targetNode = null;
                    requestStream = null;
                    if(channel != null)
                        channel.shutdownNow();
                    channel = null;
                }
                findTargetNode();
            }

            @Override
            public void onCompleted(){}
        });
    }

    @Override
    public void sendMeasurement(Measurement m) {

        SensorsGRPCInterfaceOuterClass.Measurement msg = SensorsGRPCInterfaceOuterClass.Measurement.newBuilder()
                .setId(m.getId())
                .setType(m.getType())
                .setValue(m.getValue())
                .setTimestamp(m.getTimestamp())
                .build();

        synchronized (lock) {
            if(targetNode != null && channel != null && requestStream != null)
                requestStream.onNext(msg);
        }
    }

}