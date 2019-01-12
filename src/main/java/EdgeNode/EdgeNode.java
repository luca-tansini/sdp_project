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
import io.grpc.Server;
import io.grpc.ServerBuilder;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Random;

public class EdgeNode{

    private static final int THREAD_POOL_SIZE = 5;

    private Position position;
    private int nodeId;
    private String ipAddr;
    private int sensorsPort;
    private int nodesPort;

    private SharedNodeList nodes;
    private EdgeNodeRepresentation representation;

    private EdgeNodeRepresentation coordinator;
    private final Object coordinatorLock = new Object();
    private CoordinatorThread coordinatorThread;
    private SharedBuffer<CoordinatorMessage> coordinatorBuffer;

    private SharedDatagramSocket edgeNetworkSocket;
    private EdgeNetworkWorkerThread[] edgeNetworkThreadPool;

    private Measurement globalStats;
    private Measurement localStats;

    private volatile boolean shutdown;

    public static void main(String[] args){

        if(args.length != 4){
            System.out.println("usage: EdgeNode <nodeId> <ipAddr> <sensorsPort> <nodesPort>");
            return;
        }

        int nodeId = Integer.parseInt(args[0]);
        String ipAddr = args[1];
        int sensorsPort = Integer.parseInt(args[2]);
        int nodesPort = Integer.parseInt(args[3]);

        //Immediately start sensorsCommunication
        EdgeNode node = new EdgeNode(null, nodeId, ipAddr, sensorsPort, nodesPort);

        EdgeNodeRepresentation nodeRepr = new EdgeNodeRepresentation(new Position(0,0), nodeId, ipAddr, sensorsPort, nodesPort);
        Client client = Client.create();
        WebResource webResource = client.resource("http://localhost:4242/edgenetwork/nodes");

        Random rng = new Random();
        for(int i=0; i<10; i++){
            nodeRepr.getPosition().setX(rng.nextInt(100));
            nodeRepr.getPosition().setY(rng.nextInt(100));
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
                node.setPosition(nodeRepr.getPosition());
                NodeList nodes = response.getEntity(NodeList.class);
                node.nodes = new SharedNodeList(nodes.getNodes());
                //Toglie se stesso dalla lista di nodi
                node.nodes.remove(node.getRepresentation());
                node.startWork();
                return;
            }
        }

        System.out.println("Couldn't join edge network, terminating");
        //Stop sensorCommunication
        node.coordinatorUpdatesThread.stop();
        node.gRPCServer.shutdownNow();
    }

    private void startWork(){

        startEdgeNetworkCommunication();

        if(nodes.size() == 0){
            this.setCoordinator(this.getRepresentation());
            System.out.println("self proclaimed coordinator");
            startCoordinatorWork();
        }
        else{
            helloSequence();
        }

        //Loop di attesa interruzione
        System.out.println("press a key to stop");
        try {System.in.read();} catch (IOException e) {e.printStackTrace();}
        System.out.println("stopping...");

        //Manda al server la notifica di stop
        Client client = Client.create();
        WebResource webResource = client.resource("http://localhost:4242/edgenetwork/nodes/"+this.nodeId);
        webResource.type("application/json").delete(ClientResponse.class);

        this.shutdown = true;
        //TODO: dovrei gestire questa cosa con gli shutdown. Ma i thread in attesa sulle socket?
        for(EdgeNetworkWorkerThread t: this.edgeNetworkThreadPool)
            t.stop();
        if(this.isCoordinator() && this.coordinatorThread != null)
            this.coordinatorThread.stop();
        if(this.coordinatorUpdatesThread != null)
            this.coordinatorUpdatesThread.stop();
        this.edgeNetworkSocket.close();
        this.gRPCServer.shutdownNow();

    }

    //HELLO SEQUENCE MANAGEMENT
    private final Object helloSequenceLock = new Object();

    public Object getHelloSequenceLock() {
        return helloSequenceLock;
    }

    private void helloSequence(){
        //Mando pachetti HELLO a tutti gli altri nodi
        for(EdgeNodeRepresentation node: this.nodes){
            String request = new Gson().toJson(new HelloMessage(this.getRepresentation()));
            DatagramPacket packet = new DatagramPacket(request.getBytes(), request.length(), new InetSocketAddress(node.getIpAddr(), node.getNodesPort()));
            this.edgeNetworkSocket.write(packet);
        }
        synchronized (this.helloSequenceLock){
            try {this.helloSequenceLock.wait(15000);} catch(InterruptedException e){e.printStackTrace();}
        }
        if(this.getCoordinator() != null){
            //Qualcuno mi ha detto di essere il coordinatore
            System.out.println("DEBUG: HelloSequence got coordinator info: "+this.coordinator);
        }
        else{
            synchronized (getElectionStatusLock()) {
                if (this.getElectionStatus() != ElectionStatus.FINISHED) {
                    //Sono stato coinvolto in un'elezione
                    System.out.println("DEBUG: HelloSequence into election");
                    return;
                }
                //Non ho ricevuto niente da nessuno, faccio partire una mia elezione, alla peggio contatto nodi già in un'elezione e mi manderanno un ACK
                System.out.println("DEBUG: HelloSequence is starting election after 15 seconds");
                setElectionStatus(ElectionStatus.STARTED);
                bullyElection();
            }
        }
    }


    /*
     * Crea la socket UDP per la comunicazione sulla EdgeNetwork e
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

        this.edgeNetworkThreadPool = new EdgeNetworkWorkerThread[THREAD_POOL_SIZE];

        for(int i=0; i<THREAD_POOL_SIZE; i++) {
            this.edgeNetworkThreadPool[i] = new EdgeNetworkWorkerThread(this, this.edgeNetworkSocket);
            this.edgeNetworkThreadPool[i].start();
        }
    }

    private Thread coordinatorUpdatesThread;
    private boolean awaitingCoordinatorACK;
    private final Object coordinatorACKLock = new Object();

    public Object getCoordinatorACKLock() {
        return coordinatorACKLock;
    }

    public boolean isAwaitingCoordinatorACK() {
        synchronized (coordinatorACKLock) {
            return awaitingCoordinatorACK;
        }
    }

    public void setAwaitingCoordinatorACK(boolean awaitingCoordinatorACK) {
        synchronized (coordinatorACKLock) {
            this.awaitingCoordinatorACK = awaitingCoordinatorACK;
            if(awaitingCoordinatorACK == false)
                coordinatorACKLock.notify();
        }
    }

    private SharedBuffer<Measurement> sensorsMeasurementBuffer;

    public SharedBuffer<Measurement> getSensorsMeasurementBuffer() {
        return sensorsMeasurementBuffer;
    }

    private Server gRPCServer;

    /*
     * Crea il buffer per accumulare le statistiche
     * Crea il server grpc
     * Lancia il thread per mandare i dati al coordinatore
     */
    private void startSensorsManagement(){

        //Crea il buffer per accumulare le misurazioni dei sensori
        this.sensorsMeasurementBuffer = new SharedBuffer<Measurement>();

        //Crea il server grpc che riceve gli stream dai sensori
        gRPCServer = ServerBuilder.forPort(this.sensorsPort).addService(new SensorsGRPCInterfaceImpl(this)).build();
        try{gRPCServer.start();} catch (IOException e){e.printStackTrace();}

        //Lancia il thread che manda le medie al coordinatore.
        this.coordinatorUpdatesThread = new CoordinatorUpdatesThread(this, this.edgeNetworkSocket);
        this.coordinatorUpdatesThread.start();
    }

    public enum ElectionStatus{
        STARTED, TAKEN_CARE, WON, FINISHED
    }

    private ElectionStatus electionStatus = ElectionStatus.FINISHED;
    private final Object electionStatusLock = new Object();

    public ElectionStatus getElectionStatus() {
        return electionStatus;
    }

    public void setElectionStatus(ElectionStatus electionStatus) {
        this.electionStatus = electionStatus;
    }

    public Object getElectionStatusLock() {
        return electionStatusLock;
    }

    private Object electionLock = new Object();

    public Object getElectionLock() {
        return electionLock;
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
        String electionMsg = new Gson().toJson(new ElectionMesssage(ElectionMesssage.ElectionMessageType.ELECTION, this.getRepresentation()));

        //Manda pacchetti HELLO_ELECTION agli ID maggiori (e li salva in higherId)
        for(EdgeNodeRepresentation e: this.nodes){
            if(e.getNodeId() > this.getNodeId()){
                System.out.println("DEBUG: mando pacchetto al nodo: "+e.getNodeId());
                higherId.add(e);
                this.edgeNetworkSocket.write(new DatagramPacket(electionMsg.getBytes(), electionMsg.length(), new InetSocketAddress(e.getIpAddr(), e.getNodesPort())));
            }
        }

        //Se nessuno ha id maggiore ho vinto automaticamente
        if(higherId.size() == 0){
            System.out.println("DEBUG: nessuno ha id maggiore del mio, sono io il coordinatore, mando i pacchetti VICTORY");
            synchronized (this.electionStatusLock){
                this.electionStatus = ElectionStatus.WON;
            }
            setCoordinator(this.getRepresentation());

            //Lancia il thread per il lavoro da coordinatore
            this.startCoordinatorWork();

            //Manda in giro pacchetti VICTORY
            String victoryMsg = new Gson().toJson(new ElectionMesssage(ElectionMesssage.ElectionMessageType.VICTORY, this.getRepresentation()));
            for (EdgeNodeRepresentation e: this.nodes){
                this.edgeNetworkSocket.write(new DatagramPacket(victoryMsg.getBytes(), victoryMsg.length(), new InetSocketAddress(e.getIpAddr(), e.getNodesPort())));
            }

            synchronized (this.electionStatusLock){
                this.electionStatus = ElectionStatus.FINISHED;
            }

            //Se ho vinto un'elezione iniziata da me devo segnalarlo al CoordinatorUpdatesThread
            setAwaitingCoordinatorACK(false);
            return;
        }
        else{
            //Aspetto 2 secondi
            System.out.println("DEBUG: attendo risposte");
            try {
                synchronized (this.electionLock){
                    this.electionLock.wait(2000);
                }} catch (InterruptedException e) { e.printStackTrace();}

            synchronized (this.electionStatusLock){
                //Se l'elezione è finita ho ricevuto un VICTORY packet e qualcuno sarà il coordinatore
                if(this.electionStatus == ElectionStatus.FINISHED) {
                    System.out.println("DEBUG: l'elezione è finita: devo aver ricevuto dei pacchetti VICTORY. Il nuovo coordinatore è: "+this.coordinator);
                    return;
                }

                //Se non ho ricevuto nemmeno un ACK per due secondi posso assumere che siano tutti morti
                if(this.electionStatus == ElectionStatus.STARTED){
                    System.out.println("DEBUG: non ho ricevuto notizie da nessuno, assumo che tutti quelli che ho contattato siano morti e ricomincio");
                    failed = true;
                }
            }
            if(failed) {
                //rimuovo tutti quelli che ho contattato con ID più alto del mio dalla lista (assumo siano morti) e ricomincio
                for (EdgeNodeRepresentation e : higherId) {
                    this.nodes.remove(e);
                }
                bullyElection();
                return;
            }

            //Se invece ho ricevuto un ACK da qualcuno con id superiore al mio aspetto altri 5 secondi
            System.out.println("Ho ricevuto ALIVE_ACK da qualcuno di superiore aspetto altri 5 secondi");
            try {
                synchronized (this.electionLock){
                    this.electionLock.wait(5000);
                }} catch (InterruptedException e) { e.printStackTrace();}

            //Se dopo i 5 secondi l'elezione non è finita vuol dire che qualcuno è morto dopo aver mandato un ACK, ricomincio
            synchronized (this.electionStatusLock){
                if(this.electionStatus == ElectionStatus.FINISHED){
                    System.out.println("DEBUG: dopo i 5 ho ricevuto dei pacchetti VICTORY");
                    return;
                }
                this.electionStatus = ElectionStatus.STARTED;
            }
            System.out.println("DEBUG: qualcuno deve essere morto dopo aver mandato un ALIVE_ACK, ricomincio");
            bullyElection();
        }
    }

    /*
     * Inizializza il buffer per ricevere le statistiche e
     * Lancia il thread che si occupa di raccogliere le statistiche e mandarle al server
     */
    public void startCoordinatorWork(){
        this.coordinatorBuffer = new SharedBuffer<CoordinatorMessage>();
        this.coordinatorThread = new CoordinatorThread(this, this.coordinatorBuffer, this.edgeNetworkSocket);
        this.coordinatorThread.start();
    }

    public SharedBuffer<CoordinatorMessage> getCoordinatorBuffer() {
        return coordinatorBuffer;
    }

    public EdgeNode(Position position, int nodeId, String ipAddr, int sensorsPort, int nodesPort) {
        this.position = position;
        this.nodeId = nodeId;
        this.ipAddr = ipAddr;
        this.sensorsPort = sensorsPort;
        this.nodesPort = nodesPort;
        startSensorsManagement();
    }

    public EdgeNode(EdgeNodeRepresentation repr) {
        this(repr.getPosition(), repr.getNodeId(), repr.getIpAddr(), repr.getSensorsPort(), repr.getNodesPort());
        shutdown = false;
    }

    public void setPosition(Position position) {
        this.position = position;
    }

    public SharedNodeList getNodes() {
        return nodes;
    }

    public EdgeNodeRepresentation getRepresentation(){
        if(this.representation == null)
            this.representation =  new EdgeNodeRepresentation(this.position, this.getNodeId(), this.getIpAddr(), this.getSensorsPort(), this.getNodesPort());
        return this.representation;
    }

    public EdgeNodeRepresentation getCoordinator() {
        synchronized (this.coordinatorLock) {
            return coordinator;
        }
    }

    public void setCoordinator(EdgeNodeRepresentation coordinator) {
        synchronized (this.coordinatorLock){
            this.coordinator = coordinator;
        }
    }

    public boolean isCoordinator(){
        synchronized (this.coordinatorLock) {
            if (coordinator == null)
                return false;
            else
                return this.getNodeId() == this.coordinator.getNodeId();
        }
    }

    public Object getCoordinatorLock() {
        return coordinatorLock;
    }

    public SharedDatagramSocket getEdgeNetworkSocket() {
        return edgeNetworkSocket;
    }

    public boolean isShutdown() {
        return shutdown;
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
