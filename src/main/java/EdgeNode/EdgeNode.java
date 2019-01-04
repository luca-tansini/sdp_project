package EdgeNode;

import EdgeNode.EdgeNetworkMessage.WhoisCoordRequestMessage;
import ServerCloud.Model.EdgeNodeRepresentation;
import ServerCloud.Model.Grid;
import ServerCloud.Model.NodeList;
import com.google.gson.Gson;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.util.Random;

public class EdgeNode extends EdgeNodeRepresentation {

    private static final int THREAD_POOL_SIZE = 5;

    private NodeList nodeList;
    private volatile EdgeNodeRepresentation coordinator;

    private SharedDatagramSocket edgeNetworkSocket;
    private EdgeNetworkConsumerThread edgeNetworkThreadPool[];

    private volatile boolean shutdown;

    public static void main(String args[]){

        if(args.length != 4){
            System.out.println("usage: EdgeNode <nodeId> <ipAddr> <sensorsPort> <nodesPort>");
            return;
        }

        int nodeId = Integer.parseInt(args[0]);
        String ipAddr = args[1];
        int sensorsPort = Integer.parseInt(args[2]);
        int nodesPort = Integer.parseInt(args[3]);

        Random rng = new Random();
        EdgeNodeRepresentation nodeRepr = new EdgeNodeRepresentation(0, 0, nodeId, ipAddr, sensorsPort, nodesPort);
        Client client = Client.create();
        WebResource webResource = client.resource("http://localhost:4242/edgenetwork/nodes");

        for(int i=0; i<10; i++){
            nodeRepr.setxPos(rng.nextInt(100));
            nodeRepr.setyPos(rng.nextInt(100));
            ClientResponse response = webResource.type("application/json").post(ClientResponse.class, nodeRepr);
            if(response.getStatus() != 200){
                String error = response.getEntity(String.class);
                if(error.equals(Grid.ID_TAKEN)){
                    System.out.println(Grid.ID_TAKEN + " terminating...");
                    return;
                }
                System.out.println("Got error: "+error+". retrying...");
            }
            else {
                EdgeNode node = new EdgeNode(nodeRepr);
                NodeList nodes = response.getEntity(NodeList.class);
                node.setNodeList(nodes);
                node.startWork();
                return;
            }
        }

        System.out.println("Couldn't join edge network, terminating");

    }

    private void startWork(){

        startEdgeNetworkCommunication();

        if(nodeList.getNodes().size() == 1){
            this.coordinator = this;
        }
        else{
            //Chiedo chi è il coordinatore a tutti gli altri nodi
            int i = 0;
            while(this.coordinator == null && i < this.nodeList.getNodes().size()) {
                EdgeNodeRepresentation node = this.nodeList.getNodes().get(i);
                if(node.getNodeId() == this.getNodeId()){
                    i++;
                    continue;
                }
                String request = new Gson().toJson(new WhoisCoordRequestMessage(this.getRepresentation()));
                DatagramPacket packet = new DatagramPacket(request.getBytes(), request.length(), new InetSocketAddress(node.getIpAddr(), node.getNodesPort()));
                try {
                    this.edgeNetworkSocket.write(packet);
                } catch (IOException e){
                    System.out.println("Main thread got IOException while asking EdgeNode"+node.getNodeId()+" for coordinator info:");
                    e.printStackTrace();
                }
                i++;
            }

            if(this.coordinator == null) {
                //Timeout di 2 secondi per aspettare risposte poi elezione
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if (this.coordinator == null) {
                    System.out.println("E' tempo di andare ai seggi! (ELEZIONE)");
                    return;
                    //TODO: elezione
                }
            }
            System.out.println("Habemus coordinator: "+this.coordinator);
        }

        //Loop di attesa
        System.out.println("press a key to stop");
        try {
            System.in.read();
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("stopping...");
        this.shutdown = true;
        for(EdgeNetworkConsumerThread t: this.edgeNetworkThreadPool)
            t.stop();
        this.edgeNetworkSocket.close();
    }

    /*
     * Crea la socket per la comunicazione sulla EdgeNetwork e
     * inizializza il thread pool
     */
    private void startEdgeNetworkCommunication(){

        try{
            DatagramSocket datagramSocket = new DatagramSocket(this.getNodesPort());
            this.edgeNetworkSocket = new SharedDatagramSocket(datagramSocket);

        } catch (SocketException e){
            System.out.println("error: couldn't create socket on specified nodesPort:"+this.getNodesPort());
            System.exit(-1);
        }

        this.edgeNetworkThreadPool = new EdgeNetworkConsumerThread[THREAD_POOL_SIZE];

        for(int i=0; i<THREAD_POOL_SIZE; i++) {
            this.edgeNetworkThreadPool[i] = new EdgeNetworkConsumerThread(this, this.edgeNetworkSocket);
            this.edgeNetworkThreadPool[i].start();
        }
    }


    public EdgeNode(EdgeNodeRepresentation repr) {
        super(repr.getxPos(), repr.getyPos(), repr.getNodeId(), repr.getIpAddr(), repr.getSensorsPort(), repr.getNodesPort());
        shutdown = false;
    }

    public NodeList getNodeList() {
        return nodeList;
    }

    public void setNodeList(NodeList nodeList) {
        this.nodeList = nodeList;
    }

    public EdgeNodeRepresentation getCoordinator() {
        return coordinator;
    }

    public EdgeNodeRepresentation getRepresentation(){
        return new EdgeNodeRepresentation(this.getxPos(), this.getyPos(), this.getNodeId(), this.getIpAddr(), this.getSensorsPort(), this.getNodesPort());
    }

    public void setCoordinator(EdgeNodeRepresentation coordinator) {
        this.coordinator = coordinator;
    }

    public boolean isCoordinator(){
        if(coordinator == null)
            return false;
        else
            return this.getNodeId() == this.coordinator.getNodeId();
    }

    public boolean isShutdown() {
        return shutdown;
    }
}
