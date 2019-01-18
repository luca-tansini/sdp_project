package EdgeNode;

import EdgeNode.EdgeNetworkMessage.CoordinatorMessage;
import Sensor.Measurement;
import ServerCloud.Model.EdgeNodeRepresentation;
import ServerCloud.Model.Statistics;
import io.grpc.Server;

public class StateModel {

    private static StateModel instance = null;

    public static synchronized StateModel getInstance(){
        if(instance == null)
            instance = new StateModel();
        return instance;
    }

    private StateModel(){
        electionStatus = ElectionStatus.FINISHED;
        stats = new Statistics();
    }

    //Riferimento al nodo padre e flag di shutdown
    EdgeNode parent;
    public EdgeNode getParent() {
        return parent;
    }

    public volatile boolean shutdown;


    //Lista dei nodi edge noti
    public SafetyNodeList nodes;


    //Lock per la sequenza iniziale
    public final Object helloSequenceLock = new Object();


    //Gestione dello stato delle elezioni
    public enum ElectionStatus{
        STARTED, TAKEN_CARE, WON, FINISHED
    }
    public ElectionStatus electionStatus;
    public final Object electionStatusLock = new Object();

    public Object electionLock = new Object();

    private long lastElectionTimestamp = 0;
    private final Object lastElectionTimestampLock = new Object();

    public long getLastElectionTimestamp() {
        synchronized (lastElectionTimestampLock) {
            return lastElectionTimestamp;
        }
    }

    public void setLastElectionTimestamp(long lastElectionTimestamp) {
        synchronized (lastElectionTimestampLock) {
            this.lastElectionTimestamp = lastElectionTimestamp;
        }
    }

    //Gestione della rete dei nodi Edge
    public SharedDatagramSocket edgeNetworkSocket;
    public EdgeNetworkWorkerThread[] edgeNetworkThreadPool;
    public static final int THREAD_POOL_SIZE = 5;
    public volatile boolean edgeNetworkOnline;


    //Gestione dell'albero dei nodi
    public NetworkTree networkTree;
    public final Object networkTreeLock = new Object();

    //TODO: sincronizzare come coordinatore?
    private EdgeNodeRepresentation networkTreeParent;
    public final Object networkTreeParentLock = new Object();

    public EdgeNodeRepresentation getNetworkTreeParent() {
        synchronized (networkTreeParentLock) {
            return networkTreeParent;
        }
    }

    public void setNetworkTreeParent(EdgeNodeRepresentation networkTreeParent) {
        synchronized (networkTreeParentLock) {
            this.networkTreeParent = networkTreeParent;
        }
    }

    //Gestione del coordinatore
    //TODO: cambiare sincronizzazione?
    private EdgeNodeRepresentation coordinator;
    public final Object coordinatorLock = new Object();

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

    public CoordinatorThread coordinatorThread;

    public SharedBuffer<CoordinatorMessage> coordinatorBuffer;


    //Gestione del thread che manda update al coordinatore
    //TODO: cambiare nome e comportamento
    public Thread coordinatorUpdatesThread;

    private boolean awaitingCoordinatorACK;
    public final Object coordinatorACKLock = new Object();

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


    //Gestione della comunicazione coi sensori
    public volatile boolean sensorCommunicationOnline;
    public Server gRPCServer;

    public SharedBuffer<Measurement> sensorsMeasurementBuffer;


    //Gestione delle statistiche
    public Statistics stats;
    public final Object statsLock = new Object();

}
