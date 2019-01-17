package EdgeNode;

import EdgeNode.EdgeNetworkMessage.CoordinatorMessage;
import EdgeNode.EdgeNetworkMessage.ElectionMesssage;
import EdgeNode.EdgeNetworkMessage.HelloMessage;
import EdgeNode.GRPC.SensorsGRPCInterfaceImpl;
import Sensor.Measurement;
import ServerCloud.Model.*;
import com.google.gson.Gson;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import io.grpc.ServerBuilder;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Random;
import java.util.Scanner;

public class EdgeNode{

    private static Gson gson = new Gson();

    private Position position;
    private int nodeId;
    private String ipAddr;
    private int sensorsPort;
    private int nodesPort;
    private EdgeNodeRepresentation representation;
    private StateModel stateModel;


    public EdgeNode(Position position, int nodeId, String ipAddr, int sensorsPort, int nodesPort) {
        this.position = position;
        this.nodeId = nodeId;
        this.ipAddr = ipAddr;
        this.sensorsPort = sensorsPort;
        this.nodesPort = nodesPort;
        this.stateModel = StateModel.getInstance();
        this.stateModel.parent = this;
    }

    public EdgeNode(EdgeNodeRepresentation repr) {
        this(repr.getPosition(), repr.getNodeId(), repr.getIpAddr(), repr.getSensorsPort(), repr.getNodesPort());
    }


    /*
     * Il main:
     * - costruisce l'istanza di EdgeNode
     * - fa partire la gestione dei sensori
     * - si connette al server per inserirsi nella griglia
     */
    public static void main(String[] args){

        if(args.length != 4){
            System.out.println("usage: EdgeNode <nodeId> <ipAddr> <sensorsPort> <nodesPort>");
            return;
        }

        int nodeId = Integer.parseInt(args[0]);
        String ipAddr = args[1];
        int sensorsPort = Integer.parseInt(args[2]);
        int nodesPort = Integer.parseInt(args[3]);

        //Fa partire subito la comunicazione coi sensori
        EdgeNode node = new EdgeNode(null, nodeId, ipAddr, sensorsPort, nodesPort);
        node.startSensorsManagement();

        EdgeNodeRepresentation nodeRepr = new EdgeNodeRepresentation(new Position(0,0), nodeId, ipAddr, sensorsPort, nodesPort);
        Client client = Client.create();
        WebResource webResource = client.resource("http://localhost:4242/edgenetwork/nodes");

        Random rng = new Random();
        for(int i=0; i<10; i++){
            nodeRepr.getPosition().setX(rng.nextInt(100));
            nodeRepr.getPosition().setY(rng.nextInt(100));
            ClientResponse response = webResource.type("application/json").post(ClientResponse.class, gson.toJson(nodeRepr));
            if(response.getStatus() != 200){
                String error = response.getEntity(String.class);
                if(error.equals(Grid.ID_TAKEN)){
                    System.out.println(Grid.ID_TAKEN + " terminating...");
                    return;
                }
                System.out.println("Got error: "+error+". retrying...");
            }
            else {
                node.setPosition(nodeRepr.getPosition());
                NodeList nodes = gson.fromJson(response.getEntity(String.class), NodeList.class);
                node.stateModel.nodes = new SafetyNodeList(nodes);
                //Toglie se stesso dalla lista di nodi
                node.stateModel.nodes.remove(node.getRepresentation());
                node.startWork();
                return;
            }
        }

        System.out.println("Couldn't join edge network, terminating");
        //Stop sensorCommunication TODO:fare con shutdown
        node.stateModel.coordinatorUpdatesThread.stop();
        node.stateModel.gRPCServer.shutdownNow();
    }


    private void startWork(){

        startEdgeNetworkCommunication();

        if(stateModel.nodes.size() == 0){
            stateModel.setCoordinator(this.getRepresentation());
            System.out.println("self proclaimed coordinator");
            startCoordinatorWork();
        }
        else{
            helloSequence();
        }

        //Mostra il pannello
        showPanel();
    }


    private void helloSequence(){
        //Mando pachetti HELLO a tutti gli altri nodi
        for(EdgeNodeRepresentation node: stateModel.nodes){
            String request = gson.toJson(new HelloMessage(this.getRepresentation()));
            DatagramPacket packet = new DatagramPacket(request.getBytes(), request.length(), new InetSocketAddress(node.getIpAddr(), node.getNodesPort()));
            stateModel.edgeNetworkSocket.write(packet);
        }
        synchronized (stateModel.helloSequenceLock){
            try {stateModel.helloSequenceLock.wait(15000);} catch(InterruptedException e){e.printStackTrace();}
        }
        if(stateModel.getCoordinator() != null){
            //Qualcuno mi ha detto di essere il coordinatore
            System.out.println("got coordinator info: "+stateModel.getCoordinator());
        }
        else{
            synchronized (stateModel.electionStatusLock) {
                if (stateModel.electionStatus != StateModel.ElectionStatus.FINISHED) {
                    //Sono stato coinvolto in un'elezione
                    return;
                }
                //Non ho ricevuto niente da nessuno, faccio partire una mia elezione, alla peggio contatto nodi già in un'elezione e mi manderanno un ACK
                stateModel.electionStatus = StateModel.ElectionStatus.STARTED;
            }
            bullyElection();
        }
    }


    /*
     * Crea la socket UDP per la comunicazione sulla EdgeNetwork e
     * inizializza il thread pool
     */
    private void startEdgeNetworkCommunication(){

        try{
            DatagramSocket datagramSocket = new DatagramSocket(this.getNodesPort());
            stateModel.edgeNetworkSocket = new SharedDatagramSocket(datagramSocket);
        } catch (SocketException e){
            System.out.println("error: couldn't create socket on specified nodesPort:"+this.getNodesPort());
            System.exit(-1);
        }

        stateModel.edgeNetworkThreadPool = new EdgeNetworkWorkerThread[stateModel.THREAD_POOL_SIZE];

        for(int i=0; i<stateModel.THREAD_POOL_SIZE; i++) {
            stateModel.edgeNetworkThreadPool[i] = new EdgeNetworkWorkerThread();
            stateModel.edgeNetworkThreadPool[i].start();
        }
    }


    /*
     * Crea il buffer per accumulare le statistiche
     * Crea il server grpc che riceve gli stream dai sensori
     * Lancia il thread per mandare i dati al coordinatore
     */
    private void startSensorsManagement(){

        stateModel.sensorsMeasurementBuffer = new SharedBuffer<Measurement>();

        stateModel.gRPCServer = ServerBuilder.forPort(this.sensorsPort).addService(new SensorsGRPCInterfaceImpl()).build();
        try{stateModel.gRPCServer.start();} catch (IOException e){e.printStackTrace();}

        stateModel.coordinatorUpdatesThread = new CoordinatorUpdatesThread();
        stateModel.coordinatorUpdatesThread.start();
    }


    /*
     * Bully election algorithm (based on ID)
     * Sono sicuro che se esco da questo metodo ho un coordinatore
     * Questo medoto viene eseguito da al più un solo thread alla volta, è garantito da electionStatus che è atomica
     */
    public void bullyElection(){

        boolean failed = false;
        ArrayList<EdgeNodeRepresentation> higherId = new ArrayList<>();

        //Prepara un messaggio di HELLO_ELECTION
        String electionMsg = gson.toJson(new ElectionMesssage(ElectionMesssage.ElectionMessageType.ELECTION, this.getRepresentation()));

        //Manda pacchetti HELLO_ELECTION agli ID maggiori (e li salva in higherId)
        for(EdgeNodeRepresentation e: stateModel.nodes){
            if(e.getNodeId() > this.getNodeId()){
                higherId.add(e);
                stateModel.edgeNetworkSocket.write(new DatagramPacket(electionMsg.getBytes(), electionMsg.length(), new InetSocketAddress(e.getIpAddr(), e.getNodesPort())));
            }
        }

        //Se nessuno ha id maggiore ho vinto automaticamente
        if(higherId.size() == 0){
            synchronized (stateModel.electionStatusLock){
                stateModel.electionStatus = StateModel.ElectionStatus.WON;
            }
            stateModel.setCoordinator(this.getRepresentation());

            //Lancia il thread per il lavoro da coordinatore
            this.startCoordinatorWork();

            //Manda in giro pacchetti VICTORY
            String victoryMsg = gson.toJson(new ElectionMesssage(ElectionMesssage.ElectionMessageType.VICTORY, this.getRepresentation()));
            for (EdgeNodeRepresentation e: stateModel.nodes){
                stateModel.edgeNetworkSocket.write(new DatagramPacket(victoryMsg.getBytes(), victoryMsg.length(), new InetSocketAddress(e.getIpAddr(), e.getNodesPort())));
            }

            synchronized (stateModel.electionStatusLock){
                stateModel.electionStatus = StateModel.ElectionStatus.FINISHED;
            }

            return;
        }
        else{
            //Aspetto 2 secondi
            try {
                synchronized (stateModel.electionLock){
                    stateModel.electionLock.wait(2000);
                }} catch (InterruptedException e) { e.printStackTrace();}

            synchronized (stateModel.electionStatusLock){
                //Se l'elezione è finita ho ricevuto un VICTORY packet e qualcuno sarà il coordinatore
                if(stateModel.electionStatus == StateModel.ElectionStatus.FINISHED) {
                    return;
                }

                //Se non ho ricevuto nemmeno un ACK per due secondi posso assumere che siano tutti morti
                if(stateModel.electionStatus == StateModel.ElectionStatus.STARTED){
                    failed = true;
                }
            }
            if(failed) {
                //rimuovo tutti quelli che ho contattato con ID più alto del mio dalla lista (assumo siano morti) e ricomincio
                for (EdgeNodeRepresentation e : higherId) {
                    stateModel.nodes.remove(e);
                }
                bullyElection();
                return;
            }

            //Se invece ho ricevuto un ACK da qualcuno con id superiore al mio aspetto altri 5 secondi
            try {
                synchronized (stateModel.electionLock){
                    stateModel.electionLock.wait(5000);
                }} catch (InterruptedException e) { e.printStackTrace();}

            //Se dopo i 5 secondi l'elezione non è finita vuol dire che qualcuno è morto dopo aver mandato un ACK, ricomincio
            synchronized (stateModel.electionStatusLock){
                if(stateModel.electionStatus == StateModel.ElectionStatus.FINISHED){
                    return;
                }
                stateModel.electionStatus = StateModel.ElectionStatus.STARTED;
            }
            bullyElection();
        }
    }


    /*
     * Inizializza il buffer per ricevere le statistiche e
     * Lancia il thread che si occupa di raccogliere le statistiche e mandarle al server
     */
    public void startCoordinatorWork(){
        stateModel.coordinatorBuffer = new SharedBuffer<CoordinatorMessage>();
        stateModel.coordinatorThread = new CoordinatorThread();
        stateModel.coordinatorThread.start();
    }

    public void showPanel(){

        Scanner stdin = new Scanner(System.in);

        while (true) {
            System.out.print("\n\n\n\nPANNELLO DI CONTROLLO EDGENODE id:" + getNodeId());
            if (isCoordinator()) {
                System.out.print(" (coordinatore)");
            }
            System.out.println("\n\n"+stateModel.stats + "\n");
            System.out.println("premi invo per aggiornare o x per uscire: ");

            String in = stdin.nextLine();
            if(in.equals("x"))
                break;
        }

        System.out.println("stopping...");

        //Manda al server la notifica di stop
        Client client = Client.create();
        WebResource webResource = client.resource("http://localhost:4242/edgenetwork/nodes/"+this.nodeId);
        webResource.type("application/json").delete(ClientResponse.class);

        stateModel.shutdown = true;
        //TODO: dovrei gestire questa cosa con gli shutdown. Ma i thread in attesa sulle socket?
        for(EdgeNetworkWorkerThread t: stateModel.edgeNetworkThreadPool)
            t.stop();
        if(this.isCoordinator() && stateModel.coordinatorThread != null)
            stateModel.coordinatorThread.stop();
        if(stateModel.coordinatorUpdatesThread != null)
            stateModel.coordinatorUpdatesThread.stop();
        stateModel.edgeNetworkSocket.close();
        stateModel.gRPCServer.shutdownNow();

    }

    public void setPosition(Position position) {
        this.position = position;
    }

    public EdgeNodeRepresentation getRepresentation(){
        if(this.representation == null)
            this.representation =  new EdgeNodeRepresentation(this.position, this.getNodeId(), this.getIpAddr(), this.getSensorsPort(), this.getNodesPort());
        return this.representation;
    }

    public boolean isCoordinator(){
        synchronized (stateModel.coordinatorLock) {
            if (stateModel.getCoordinator() == null)
                return false;
            else
                return this.getNodeId() == stateModel.getCoordinator().getNodeId();
        }
    }

    public int getNodeId() {
        return nodeId;
    }

    public String getIpAddr() {
        return ipAddr;
    }

    public int getSensorsPort() {
        return sensorsPort;
    }

    public int getNodesPort() {
        return nodesPort;
    }
}
