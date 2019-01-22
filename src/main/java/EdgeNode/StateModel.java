package EdgeNode;

import EdgeNode.EdgeNetworkMessage.ParentMessage;
import Sensor.Measurement;
import ServerCloud.Model.EdgeNodeRepresentation;
import ServerCloud.Model.Statistics;
import io.grpc.Server;

import java.util.HashMap;

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

    //Riferimento all'Edgenode
    public EdgeNode edgeNode;


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

    public final Object electionLock = new Object();

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


    //Gestione del comportamento da nodo interno
    public InternalNodeThread internalNodeThread;
    public SharedBuffer<ParentMessage> internalNodeBuffer;
    private boolean isInternalNode = false;
    public final Object internalNodeLock = new Object();

    public boolean isInternalNode() {
        synchronized (internalNodeLock) {
            return isInternalNode;
        }
    }

    public void setInternalNode(boolean internalNode) {
        synchronized (internalNodeLock) {
            isInternalNode = internalNode;
        }
    }

    //Gestione dei thread che mandano update al parent
    public ParentUpdatesThread parentUpdatesThread;

    private boolean awaitingParentACK;
    public final Object parentACKLock = new Object();

    public boolean isAwaitingParentACK() {
        synchronized (parentACKLock) {
            return awaitingParentACK;
        }
    }

    public void setAwaitingParentACK(boolean awaitingParentACK) {
        synchronized (parentACKLock) {
            this.awaitingParentACK = awaitingParentACK;
            if(awaitingParentACK == false)
                parentACKLock.notify();
        }
    }

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
    //Dentro a partialMean ci sono le statistiche da aggregare (la mia media e le medie che arrivano dai figli)
    public HashMap<String, Measurement> partialMean;

}
