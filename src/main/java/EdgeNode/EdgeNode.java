package EdgeNode;

import EdgeNode.EdgeNetworkMessage.*;
import EdgeNode.GRPC.SensorsGRPCInterfaceImpl;
import Sensor.Measurement;
import ServerCloud.Model.*;
import com.google.gson.Gson;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import io.grpc.ServerBuilder;

import javax.ws.rs.core.Response;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.time.Instant;
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

        EdgeNode node = new EdgeNode(null, nodeId, ipAddr, sensorsPort, nodesPort);

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
                    System.out.println(Grid.ID_TAKEN);
                    break;
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
    }


    private void startWork(){

        startEdgeNetworkCommunication();

        //Se è l'unico nodo si proclama coordinatore
        if(stateModel.nodes.size() == 0){
            System.out.println("self proclaimed coordinator");
            stateModel.setLastElectionTimestamp(Instant.now().toEpochMilli());
            startCoordinatorWork();
        }
        //Altrimenti fa partire la helloSequence
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
            //Qualcuno mi ha risposto con un pacchetto HELLO_RESPONSE
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
     * Mostra il pannello e attende il comando di arresto
     */
    private void showPanel(){

        Scanner stdin = new Scanner(System.in);

        while (true) {
            System.out.println("\n\n\n\n\n\n\n\n\n\n\n\n");
            System.out.print("PANNELLO DI CONTROLLO EDGENODE id:" + getNodeId());
            if (isCoordinator()) {
                System.out.print(" (coordinatore)");
            }
            synchronized (stateModel.statsLock) {
                System.out.println("\n\n" + stateModel.stats + "\n\n\n\n\n\n");
            }
            System.out.println("premi invio per aggiornare o x per uscire: ");
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
        stopSensorsCommunication();
        stopEdgeNetworkCommunication();
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

        stateModel.edgeNetworkOnline = true;
        stateModel.edgeNetworkThreadPool = new EdgeNetworkWorkerThread[stateModel.THREAD_POOL_SIZE];

        for(int i=0; i<stateModel.THREAD_POOL_SIZE; i++) {
            stateModel.edgeNetworkThreadPool[i] = new EdgeNetworkWorkerThread();
            stateModel.edgeNetworkThreadPool[i].start();
        }

    }

    private void stopEdgeNetworkCommunication(){
        stateModel.edgeNetworkOnline = false;

        String jsonQuit = gson.toJson(new QuitMessage());
        for(int i=0; i<stateModel.THREAD_POOL_SIZE; i++)
            stateModel.edgeNetworkSocket.write(new DatagramPacket(jsonQuit.getBytes(), jsonQuit.length(), new InetSocketAddress(this.ipAddr,this.nodesPort)));

        if(this.isCoordinator() && stateModel.coordinatorThread != null)
            stateModel.coordinatorBuffer.put(new CoordinatorMessage(CoordinatorMessage.CoordinatorMessageType.QUIT, null, null));

        try {
            for (EdgeNetworkWorkerThread t : stateModel.edgeNetworkThreadPool)
                t.join();
            if(stateModel.coordinatorThread != null)
                stateModel.coordinatorThread.join();
        }
        catch (InterruptedException e) {e.printStackTrace();}

        stateModel.edgeNetworkSocket.close();
    }


    /*
     * Crea il buffer per accumulare le statistiche
     * Crea il server grpc che riceve gli stream dai sensori
     * Lancia il thread per mandare i dati al coordinatore
     */
    protected void startSensorsCommunication(){

        stateModel.sensorCommunicationOnline = true;
        stateModel.sensorsMeasurementBuffer = new SharedBuffer<Measurement>();

        stateModel.gRPCServer = ServerBuilder.forPort(this.sensorsPort).addService(new SensorsGRPCInterfaceImpl()).build();
        try{stateModel.gRPCServer.start();} catch (IOException e){e.printStackTrace();}

        stateModel.coordinatorUpdatesThread = new CoordinatorUpdatesThread();
        stateModel.coordinatorUpdatesThread.start();

    }

    protected void stopSensorsCommunication(){

        stateModel.sensorCommunicationOnline = false;
        stateModel.sensorsMeasurementBuffer.put(new Measurement("quit","quit",0,0));
        stateModel.gRPCServer.shutdownNow();
        try {
            stateModel.coordinatorUpdatesThread.join();
        }
        catch (InterruptedException e) {e.printStackTrace();}
    }

    /*
     * Crea il buffer e fa partire il thread che si occupa di raccogliere le statistiche
     * e mandarle al parent (o al server se è il coordinatore)
    */
    protected void startInternalNodeWork(){
        stateModel.coordinatorBuffer = new SharedBuffer<CoordinatorMessage>();
        stateModel.coordinatorThread = new CoordinatorThread();
        stateModel.coordinatorThread.start();
    }

    protected void stopInternalNodeWork(){}

    /*
     * Fa partire il lavoro da nodo interno
     * Costruisce l'albero dei nodi edge
     * Comunica a nodi il loro ruolo nell'albero
     * Comunica al server la lista delle foglie
     */
    protected void startCoordinatorWork(){

        stateModel.setCoordinator(this.getRepresentation());
        startInternalNodeWork();

        ArrayList<EdgeNodeRepresentation> leaves = null;
        ArrayList<NetworkTreeNode> nodes = null;
        //Costruisce l'albero dei nodi edge
        synchronized (stateModel.networkTreeLock) {
            stateModel.networkTree = buildNetworkTree();
            leaves = stateModel.networkTree.getLeaves();
            nodes = stateModel.networkTree.toList();
        }

        //Comunica a nodi il loro ruolo nell'albero e il padre
        for(NetworkTreeNode ntn: nodes){
            if(ntn.isLeaf()){
                String json = gson.toJson(new TreeMessage(TreeMessage.TreeNodeType.LEAF, ntn.getParent().getEdgeNode()));
                stateModel.edgeNetworkSocket.write(new DatagramPacket(json.getBytes(), json.length(), new InetSocketAddress(ntn.getEdgeNode().getIpAddr(), ntn.getEdgeNode().getNodesPort())));
            }
            else {
                String json = gson.toJson(new TreeMessage(TreeMessage.TreeNodeType.INTERNAL, ntn.getParent().getEdgeNode()));
                stateModel.edgeNetworkSocket.write(new DatagramPacket(json.getBytes(), json.length(), new InetSocketAddress(ntn.getEdgeNode().getIpAddr(), ntn.getEdgeNode().getNodesPort())));
            }
        }

        //Comunica al server la lista delle foglie
        Client client = Client.create();
        WebResource webResource = client.resource("http://localhost:4242/edgenetwork/leaves");
        webResource.type("application/json").post(ClientResponse.class, gson.toJson(leaves));
    }

    private NetworkTree buildNetworkTree(){

        NetworkTree networkTree = new NetworkTree(this.getRepresentation());

        for(EdgeNodeRepresentation node: stateModel.nodes) {
            networkTree.addNode(node);
        }

        return networkTree;
    }


    /*
     * Aggiunge il nodo passato all'albero e gli risponde con un pacchetto HELLO_RESPONSE in cui comunica coordinatore e parent
     * Inoltre se il parent è appena stato promosso a nodo interno glielo notifica
     */
    protected void addNetworkTreeNode(EdgeNodeRepresentation node){
        String helloResponseJson;
        String parentPromotionJson = null;
        EdgeNodeRepresentation parentEdgeNode;

        synchronized (stateModel.networkTreeLock){
            NetworkTreeNode parent = stateModel.networkTree.addNode(node);
            parentEdgeNode = parent.getEdgeNode();
            helloResponseJson = gson.toJson(new HelloResponseMessage(this.getRepresentation(), stateModel.getLastElectionTimestamp(), parentEdgeNode));
            //Se il parent ha un figlio solo vuol dire che l'ho appena promosso
            if(parent.getChildren().size() == 1) {
                EdgeNodeRepresentation grandparentEdge = null;
                NetworkTreeNode grandparent = parent.getParent();
                if(grandparent != null) grandparentEdge = grandparent.getEdgeNode();
                parentPromotionJson = gson.toJson(new TreeMessage(TreeMessage.TreeNodeType.INTERNAL, grandparentEdge));
            }
        }

        //Comunica al server la nuova foglia e nel caso di promozione toglie il parent dalle foglie
        Client client = Client.create();
        WebResource webResource = client.resource("http://localhost:4242/edgenetwork/leaves/"+node.getNodeId());
        ClientResponse response = webResource.type("application/json").post(ClientResponse.class);
        if(response.getStatus() == 404)
            System.out.println("DEBUG: EdgeNode - got NOT_FOUND while adding node"+node.getNodeId()+"as leaf");

        if(parentPromotionJson != null){
            webResource = client.resource("http://localhost:4242/edgenetwork/leaves/"+parentEdgeNode.getNodeId());
            response = webResource.type("application/json").delete(ClientResponse.class);
            if(response.getStatus() == 404)
                System.out.println("DEBUG: EdgeNode - got NOT_FOUND while removing node"+parentEdgeNode.getNodeId()+" from leaves");
        }

        //Comunica al nodo (e nel caso anche al parent) il nuovo ruolo nell'albero
        stateModel.edgeNetworkSocket.write(new DatagramPacket(helloResponseJson.getBytes(), helloResponseJson.length(), new InetSocketAddress(node.getIpAddr(), node.getNodesPort())));
        if(parentPromotionJson != null)
            stateModel.edgeNetworkSocket.write(new DatagramPacket(parentPromotionJson.getBytes(),parentPromotionJson.length(),new InetSocketAddress(parentEdgeNode.getIpAddr(), parentEdgeNode.getNodesPort())));

    }


    /*
     * Bully election algorithm (based on ID)
     * Sono sicuro che se esco da questo metodo ho un coordinatore
     * Questo medoto viene eseguito da al più un solo thread alla volta, è garantito da electionStatus che è atomica
     */
    protected void bullyElection(){

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
            ElectionMesssage victory = new ElectionMesssage(ElectionMesssage.ElectionMessageType.VICTORY, this.getRepresentation());
            String victoryMsg = gson.toJson(victory);
            for (EdgeNodeRepresentation e: stateModel.nodes){
                stateModel.edgeNetworkSocket.write(new DatagramPacket(victoryMsg.getBytes(), victoryMsg.length(), new InetSocketAddress(e.getIpAddr(), e.getNodesPort())));
            }

            stateModel.setLastElectionTimestamp(victory.getTimestamp());

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
