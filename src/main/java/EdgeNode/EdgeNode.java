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

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
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
        this.stateModel.edgeNode = this;
        //Fa partire subito la comunicazione coi sensori
        startSensorsCommunication();
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
        node.stopSensorsCommunication();
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
        // Può capitare che il coordinatore mi risponda prima ancora di aver finito di inviare gli hello
        if(stateModel.getCoordinator() == null) {
            synchronized (stateModel.helloSequenceLock) {
                try {
                    stateModel.helloSequenceLock.wait(15000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        if(stateModel.getCoordinator() != null){
            //Qualcuno mi ha risposto con un pacchetto HELLO_RESPONSE
            System.out.println("DEBUG: EdgeNode - got coordinator info: "+stateModel.getCoordinator());
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
            else
                System.out.print(" (parent:"+stateModel.getNetworkTreeParent().getNodeId()+")");
            System.out.println("\n");

            //Stampa le statistiche immagazzinate
            System.out.println("Statistiche:");
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

        stopSensorsCommunication();
        stopInternalNodeWork();
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
        stateModel.edgeNetworkThreadPool = new EdgeNetworkWorkerThread[StateModel.THREAD_POOL_SIZE];

        for(int i=0; i<StateModel.THREAD_POOL_SIZE; i++) {
            stateModel.edgeNetworkThreadPool[i] = new EdgeNetworkWorkerThread();
            stateModel.edgeNetworkThreadPool[i].start();
        }

    }

    private void stopEdgeNetworkCommunication(){
        stateModel.edgeNetworkOnline = false;

        String jsonQuit = gson.toJson(new QuitMessage());
        for(int i=0; i<StateModel.THREAD_POOL_SIZE; i++)
            stateModel.edgeNetworkSocket.write(new DatagramPacket(jsonQuit.getBytes(), jsonQuit.length(), new InetSocketAddress(this.ipAddr,this.nodesPort)));

        try {
            for (EdgeNetworkWorkerThread t : stateModel.edgeNetworkThreadPool)
                t.join();
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

        //Controlla che non sia già attiva
        if(!stateModel.sensorCommunicationOnline) {

            System.out.println("DEBUG: EdgeNode - starting sensors communication");

            stateModel.sensorCommunicationOnline = true;
            stateModel.sensorsMeasurementBuffer = new SharedBuffer<Measurement>();

            stateModel.gRPCServer = ServerBuilder.forPort(this.sensorsPort).addService(new SensorsGRPCInterfaceImpl()).build();
            try {
                stateModel.gRPCServer.start();
            } catch (IOException e) {
                e.printStackTrace();
            }

            stateModel.parentUpdatesThread = new ParentUpdatesThread();
            stateModel.parentUpdatesThread.start();
        }
    }

    protected void stopSensorsCommunication(){

        //Controlla che ci sia effettivamente una comunicazione attiva
        if(stateModel.sensorCommunicationOnline) {

            System.out.println("DEBUG: EdgeNode - stopping sensors communication");

            stateModel.sensorCommunicationOnline = false;
            stateModel.sensorsMeasurementBuffer.put(new Measurement("quit", "quit", 0, 0));
            stateModel.gRPCServer.shutdownNow();
            try {
                stateModel.parentUpdatesThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    /*
     * Crea il buffer e fa partire il thread che si occupa di raccogliere le statistiche
     * e mandarle al edgeNode (o al server se è il coordinatore)
    */
    protected void startInternalNodeWork(){

        if(!stateModel.isInternalNode) {

            System.out.println("DEBUG: EdgeNode - starting internal node work");

            stateModel.isInternalNode = true;
            stateModel.partialMean = new HashMap<>();
            stateModel.internalNodeBuffer = new SharedBuffer<ParentMessage>();
            stateModel.internalNodeThread = new InternalNodeThread();
            stateModel.internalNodeThread.start();
        }
    }

    protected void stopInternalNodeWork(){

        if(stateModel.isInternalNode) {

            System.out.println("DEBUG: EdgeNode - stopping internal node work");

            stateModel.isInternalNode = false;
            stateModel.internalNodeBuffer.put(new ParentMessage(ParentMessage.ParentMessageType.QUIT, null, null));
            try {
                stateModel.internalNodeThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    /*
     * Fa partire il lavoro da nodo interno
     * Costruisce l'albero dei nodi edge
     * Comunica a nodi il loro ruolo nell'albero
     * Comunica al server la lista delle foglie
     */
    protected void startCoordinatorWork(){

        System.out.println("DEBUG: EdgeNode - starting coordinator work");

        stateModel.setCoordinator(this.getRepresentation());
        stopInternalNodeWork();
        stateModel.isInternalNode = true;
        stateModel.partialMean = new HashMap<>();
        stateModel.internalNodeBuffer = new SharedBuffer<ParentMessage>();
        stateModel.internalNodeThread = new CoordinatorThread();
        stateModel.internalNodeThread.start();

        ArrayList<NetworkTreeNode> nodes;
        //Costruisce l'albero dei nodi edge
        synchronized (stateModel.networkTreeLock) {
            stateModel.networkTree = buildNetworkTree();
            nodes = stateModel.networkTree.toList();

            //DEBUG
            System.out.println("DEBUG - Albero appena costruito");
            stateModel.networkTree.printTree();

        }

        //Comunica a nodi il loro ruolo nell'albero e il padre
        for(NetworkTreeNode ntn: nodes){
            if(ntn.isRoot()) continue;
            if(ntn.isLeaf()) {
                String json = gson.toJson(new TreeMessage(TreeMessage.TreeMessageType.LEAF, ntn.getParent().getEdgeNode()));
                stateModel.edgeNetworkSocket.write(new DatagramPacket(json.getBytes(), json.length(), new InetSocketAddress(ntn.getEdgeNode().getIpAddr(), ntn.getEdgeNode().getNodesPort())));
            }
            else {
                String json = gson.toJson(new TreeMessage(TreeMessage.TreeMessageType.INTERNAL, ntn.getParent().getEdgeNode()));
                stateModel.edgeNetworkSocket.write(new DatagramPacket(json.getBytes(), json.length(), new InetSocketAddress(ntn.getEdgeNode().getIpAddr(), ntn.getEdgeNode().getNodesPort())));
            }
        }
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
     * All'inizio rimuove il nodo dall'albero perchè potrebbe essere rimasto come foglia zombie
     */
    protected void addNetworkTreeNode(EdgeNodeRepresentation node){
        String helloResponseJson;
        String parentPromotionJson = null;
        EdgeNodeRepresentation parentEdgeNode;

        synchronized (stateModel.networkTreeLock){

            //Rimuove eventuali zombie
            NetworkTreeNode networkTreeNode = stateModel.networkTree.findNode(node);
            if(networkTreeNode != null)
                stateModel.networkTree.removeNode(networkTreeNode);

            NetworkTreeNode parent = stateModel.networkTree.addNode(node);
            parentEdgeNode = parent.getEdgeNode();
            helloResponseJson = gson.toJson(new HelloResponseMessage(this.getRepresentation(), stateModel.getLastElectionTimestamp(), parentEdgeNode));
            //Se il parent ha un figlio solo vuol dire che l'ho appena promosso
            if(!parent.isRoot() && parent.getChildren().size() == 1) {
                //Mando parent null tanto non sto cambiando il padre
                parentPromotionJson = gson.toJson(new TreeMessage(TreeMessage.TreeMessageType.INTERNAL, null));
            }
        }

        // Se ha promosso il parent gli comunica il nuovo ruolo di nodo interno
        if(parentPromotionJson != null){
            stateModel.edgeNetworkSocket.write(new DatagramPacket(parentPromotionJson.getBytes(),parentPromotionJson.length(),new InetSocketAddress(parentEdgeNode.getIpAddr(), parentEdgeNode.getNodesPort())));
        }

        // Comunica al nodo nuovo il ruolo di foglia nell'albero
        stateModel.edgeNetworkSocket.write(new DatagramPacket(helloResponseJson.getBytes(), helloResponseJson.length(), new InetSocketAddress(node.getIpAddr(), node.getNodesPort())));

        //DEBUG
        System.out.println("\nDEBUG - Albero dopo inserimento nodo nuovo "+node.getNodeId());
        stateModel.networkTree.printTree();
        System.out.println();

    }


    /*
     * Gestione del caso in cui muore un nodo interno (sono il coordinatore e ricevo un PARENT_DOWN)
     * Quello che deve fare è semplicemente togliere il parent del nodo che ha mandato il pacchetto
     * dalla lista dei figli del grandparent e trovare un nuovo parent per il nodo.
     * Se arriveranno altri pacchetti PARENT_DOWN dai fratelli di node
     * li riconoscerà perchè non riuscirò a trovarli nell'albero e gli darò un nuovo padre.
     * In questo modo faccio sparire dall'albero anche eventuali figli foglia zombie.
     */
    protected void networkTreeNodeParentDown(EdgeNodeRepresentation node){

        String parentUpdateJson;
        String parentPromotionJson = null;
        EdgeNodeRepresentation newParentEdgeNode = null;
        NetworkTreeNode grandparent = null;
        String grandparentDemotionJson = null;
        EdgeNodeRepresentation grandparentEdgenode = null;

        synchronized (stateModel.networkTreeLock){

            // Prende un riferimento al nodo dell'albero di node (per non perdere il sottoalbero)
            NetworkTreeNode networkTreeNode = stateModel.networkTree.findNode(node);

            // Toglie il deadParent dall'albero (questo può portare ad una demozione di un nodo da nodo interno a foglia)
            // e si rimuove dalla lista dei figli del padre
            if(networkTreeNode.getParent() != null) {
                grandparent = networkTreeNode.getParent().getParent();
                networkTreeNode.getParent().getChildren().remove(networkTreeNode);
                stateModel.networkTree.removeNode(networkTreeNode.getParent());
            }

            //DEBUG
            System.out.println("\nDEBUG - Albero dopo rimozione");
            stateModel.networkTree.printTree();

            // Aggiunge il NetworkTreeNode di node all'albero
            NetworkTreeNode newParent = stateModel.networkTree.addNode(networkTreeNode);

            //DEBUG
            System.out.println("\nDEBUG - Albero dopo aggiunta");
            stateModel.networkTree.printTree();
            System.out.println();

            // Messaggio di parentUpdate per node
            parentUpdateJson = gson.toJson(new TreeMessage(TreeMessage.TreeMessageType.PARENT_UPDATE, newParent.getEdgeNode()));

            // Se newParent e grandparent sono lo stesso nodo non devo fare nulla di più
            if(newParent != grandparent){
                // Se grandparent non ha più figli lo faccio diventare una foglia
                if(grandparent != null && grandparent.getChildren().size() == 0){
                    grandparentDemotionJson = gson.toJson(new TreeMessage(TreeMessage.TreeMessageType.LEAF, null));
                    grandparentEdgenode = grandparent.getEdgeNode();
                }
                // Se newParent ha un figlio solo vuol dire che l'ho appena promosso
                if(newParent.getChildren().size() == 1) {
                    parentPromotionJson = gson.toJson(new TreeMessage(TreeMessage.TreeMessageType.INTERNAL, null));
                    newParentEdgeNode = newParent.getEdgeNode();
                }
            }
        }

        // Se ho promosso il padre glielo notifico
        if(parentPromotionJson != null)
            stateModel.edgeNetworkSocket.write(new DatagramPacket(parentPromotionJson.getBytes(), parentPromotionJson.length(), new InetSocketAddress(newParentEdgeNode.getIpAddr(), newParentEdgeNode.getNodesPort())));

        // Notifico a node il nuovo padre
        stateModel.edgeNetworkSocket.write(new DatagramPacket(parentUpdateJson.getBytes(), parentUpdateJson.length(), new InetSocketAddress(node.getIpAddr(), node.getNodesPort())));

        // Se ho degradato il grandparent glielo notifico
        if(grandparentDemotionJson != null){
            stateModel.edgeNetworkSocket.write(new DatagramPacket(grandparentDemotionJson.getBytes(), grandparentDemotionJson.length(), new InetSocketAddress(grandparentEdgenode.getIpAddr(), grandparentEdgenode.getNodesPort())));
        }
    }


    /*
     * Bully election algorithm (based on ID)
     * Sono sicuro che se esco da questo metodo ho un coordinatore
     * Questo medoto viene eseguito da al più un solo thread alla volta, è garantito da electionStatus che è atomica
     */
    protected void bullyElection(){

        boolean failed = false;
        ArrayList<EdgeNodeRepresentation> higherId = new ArrayList<>();

        // Prepara un messaggio di HELLO_ELECTION
        String electionMsg = gson.toJson(new ElectionMesssage(ElectionMesssage.ElectionMessageType.ELECTION, this.getRepresentation()));

        // Manda pacchetti HELLO_ELECTION agli ID maggiori (e li salva in higherId)
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
