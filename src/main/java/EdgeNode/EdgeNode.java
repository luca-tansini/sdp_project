package EdgeNode;

import EdgeNode.EdgeNetworkMessage.CoordinatorMessage;
import EdgeNode.EdgeNetworkMessage.ElectionMesssage;
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
import java.util.ArrayList;
import java.util.Random;

public class EdgeNode{

    private static final int THREAD_POOL_SIZE = 5;

    private int xPos;
    private int yPos;
    private int nodeId;
    private String ipAddr;
    private int sensorsPort;
    private int nodesPort;

    //TODO: sincronizzare nodes
    private SharedNodeList nodes;
    private EdgeNodeRepresentation representation;

    private volatile EdgeNodeRepresentation coordinator;
    private CoordinatorThread coordinatorThread;
    private SharedBuffer<CoordinatorMessage> coordinatorBuffer;

    private Thread sensorThread;
    private boolean awaitingCoordinatorACK;
    //TODO: E' davvero necessario?
    private final Object coordinatorACKLock = new Object();

    private SharedDatagramSocket edgeNetworkSocket;
    private EdgeNetworkWorkerThread edgeNetworkThreadPool[];

    private volatile boolean shutdown;
    private ElectionStatus electionStatus = ElectionStatus.FINISHED;
    private final Object electionStatusLock = new Object();

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
                node.nodes = new SharedNodeList(nodes.getNodes());
                //Toglie se stesso dalla lista di nodi
                node.nodes.remove(node.getRepresentation());
                node.startWork();
                return;
            }
        }

        System.out.println("Couldn't join edge network, terminating");

    }

    private void startWork(){

        startEdgeNetworkCommunication();

        if(nodes.size() == 0){
            this.coordinator = this.getRepresentation();
            System.out.println("self proclaimed coordinator");
            startCoordinatorWork();
        }
        else{
            //Chiedo chi è il coordinatore a tutti gli altri nodi
            int i = 0;
            while(this.coordinator == null && i < this.nodes.size()) {
                EdgeNodeRepresentation node = this.nodes.get(i);
                String request = new Gson().toJson(new WhoisCoordRequestMessage(this.getRepresentation()));
                DatagramPacket packet = new DatagramPacket(request.getBytes(), request.length(), new InetSocketAddress(node.getIpAddr(), node.getNodesPort()));
                this.edgeNetworkSocket.write(packet);
                i++;
            }

            if(this.coordinator == null) {
                //Timeout di 1 secondo per aspettare risposte poi elezione
                try { Thread.sleep(1000); } catch (InterruptedException e) { e.printStackTrace();}
                if (this.coordinator == null) {
                    System.out.println("DEBUG: il coordinatore non ha risposto ai miei pacchetti WHOIS_COORD");
                    //Controlla che non sia già stata iniziata un'elezione da qualcuno
                    boolean alreadyStarted = false;
                    synchronized (this.electionStatusLock){
                        if(this.electionStatus == ElectionStatus.FINISHED)
                            this.electionStatus = ElectionStatus.STARTED;
                        else
                            alreadyStarted = true;
                    }
                    if(!alreadyStarted){
                        System.out.println("DEBUG: Starting election from main");
                        bullyElection();
                    }
                }
                else{
                    System.out.println("Got coordinator info: "+this.coordinator);
                }
            }
        }

        //TODO: is it safe to put this here?
        startSensorsManagement();

        //Loop di attesa
        System.out.println("press a key to stop");
        try {System.in.read();} catch (IOException e) {e.printStackTrace();}
        System.out.println("stopping...");
        this.shutdown = true;
        for(EdgeNetworkWorkerThread t: this.edgeNetworkThreadPool)
            t.stop();
        if(this.isCoordinator() && this.coordinatorThread != null)
            this.coordinatorThread.stop();
        if(this.sensorThread != null)
            this.sensorThread.stop();
        this.edgeNetworkSocket.close();

        //Manda al server la notifica di stop
        Client client = Client.create();
        WebResource webResource = client.resource("http://localhost:4242/edgenetwork/nodes/"+this.nodeId);
        webResource.type("application/json").delete(ClientResponse.class);
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

        this.edgeNetworkThreadPool = new EdgeNetworkWorkerThread[THREAD_POOL_SIZE];

        for(int i=0; i<THREAD_POOL_SIZE; i++) {
            this.edgeNetworkThreadPool[i] = new EdgeNetworkWorkerThread(this, this.edgeNetworkSocket);
            this.edgeNetworkThreadPool[i].start();
        }
    }

    public boolean isAwaitingCoordinatorACK() {
        synchronized (coordinatorACKLock) {
            return awaitingCoordinatorACK;
        }
    }

    public void setAwaitingCoordinatorACK(boolean awaitingCoordinatorACK) {
        synchronized (coordinatorACKLock) {
            this.awaitingCoordinatorACK = awaitingCoordinatorACK;
        }
    }

    /*
     * Crea la socket per la comunicazione con i sensori
     * Crea il pool di thread per gestirli
     * Lancia il thread per mandare i dati al coordinatore
     */
    private void startSensorsManagement(){

        //TODO: Crea la socket per la comunicazione con i sensori

        //TODO: Crea il pool di thread per gestirli

        //TODO: Lancia il thread che raccoglie i dati e li manda al coordinatore. SUPER DUMMY, se li manda anche da solo
        this.sensorThread = new Thread(new Runnable() {

            @Override
            public void run() {

                Gson gson = new Gson();

                while (true){
                    try {
                        //Se il coordinatore non è definito (può succedere solo all'inizio in effetti) non fa nulla TODO: migliorare situazione
                        if(getCoordinator() != null) {
                            System.out.println("Sensors thread is sending STATS_UPDATE: random message from " + nodeId);
                            CoordinatorMessage coordinatorMessage = new CoordinatorMessage(CoordinatorMessage.CoordinatorMessageType.STATS_UPDATE, getRepresentation(), "random message from " + nodeId);
                            String json = gson.toJson(coordinatorMessage, CoordinatorMessage.class);
                            edgeNetworkSocket.write(new DatagramPacket(json.getBytes(), json.length(), new InetSocketAddress(coordinator.getIpAddr(), coordinator.getNodesPort())));
                            setAwaitingCoordinatorACK(true);
                        }
                        Thread.sleep(5000);
                        if(isAwaitingCoordinatorACK()){
                            System.out.println("DEBUG: Il coordinatore è morto!");
                            //Rimuove il vecchio coordinatore dalla lista dei nodi attivi
                            setCoordinator(null);
                            getNodes().remove(getCoordinator());
                            setAwaitingCoordinatorACK(false);
                            synchronized (getElectionStatusLock()){
                                if(getElectionStatus() != EdgeNode.ElectionStatus.FINISHED)
                                    continue;
                                setElectionStatus(EdgeNode.ElectionStatus.STARTED);
                            }
                            System.out.println("DEBUG: Sensor thread: faccio partire elezione");
                            bullyElection();
                        }
                    } catch (Exception e){
                        e.printStackTrace();
                        try {Thread.sleep(1000);} catch (InterruptedException e1) {e1.printStackTrace();}
                    }
                }
            }
        });
        this.sensorThread.start();

    }


    public enum ElectionStatus{
        STARTED, TAKEN_CARE, WON, FINISHED
    }

    public ElectionStatus getElectionStatus() {
        return electionStatus;
    }

    public void setElectionStatus(ElectionStatus electionStatus) {
        this.electionStatus = electionStatus;
    }

    public Object getElectionStatusLock() {
        return electionStatusLock;
    }

    /*
     * Bully election algorithm (based on ID)
     * Sono sicuro che se esco da questo metodo ho un coordinatore
     * Questo medoto viene eseguito da al più un solo thread alla volta, è garantito da electionStatus che è atomica
     */
    public void bullyElection(){

        boolean failed = false;
        ArrayList<EdgeNodeRepresentation> higherId = new ArrayList<>();

        //Prepara un messaggio di ELECTION
        String electionMsg = new Gson().toJson(new ElectionMesssage(ElectionMesssage.ElectionMessageType.ELECTION, this.getRepresentation()));

        //Manda pacchetti ELECTION agli ID maggiori (e li salva in higherId)
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
            // Se il sensorThread stava aspettando una risposta e
            // siamo entrati qui dentro non per merito suo,
            // lo facciamo passare oltre
            setAwaitingCoordinatorACK(false);
            return;
        }
        else{
            //Aspetto 2 secondi
            System.out.println("DEBUG: attendo risposte");
            try { Thread.sleep(2000); } catch (InterruptedException e) { e.printStackTrace();}

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
            try { Thread.sleep(5000); } catch (InterruptedException e) { e.printStackTrace();}

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

    public EdgeNode(int xPos, int yPos, int nodeId, String ipAddr, int sensorsPort, int nodesPort) {
        this.xPos = xPos;
        this.yPos = yPos;
        this.nodeId = nodeId;
        this.ipAddr = ipAddr;
        this.sensorsPort = sensorsPort;
        this.nodesPort = nodesPort;
    }

    public EdgeNode(EdgeNodeRepresentation repr) {
        this(repr.getxPos(), repr.getyPos(), repr.getNodeId(), repr.getIpAddr(), repr.getSensorsPort(), repr.getNodesPort());
        shutdown = false;
    }

    public SharedNodeList getNodes() {
        return nodes;
    }

    public EdgeNodeRepresentation getCoordinator() {
        return coordinator;
    }

    public EdgeNodeRepresentation getRepresentation(){
        if(this.representation == null)
            this.representation =  new EdgeNodeRepresentation(this.getxPos(), this.getyPos(), this.getNodeId(), this.getIpAddr(), this.getSensorsPort(), this.getNodesPort());
        return this.representation;
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

    public int getxPos() {
        return xPos;
    }

    public int getyPos() {
        return yPos;
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
