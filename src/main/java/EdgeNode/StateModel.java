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

    //Riferimento all'Edgenode e flag di shutdown
    public EdgeNode edgeNode;

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
    public boolean isInternalNode = false;


    //Gestione del thread che manda update al parent
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


    //Gestione della comunicazione coi sensori
    public volatile boolean sensorCommunicationOnline;
    public Server gRPCServer;

    public SharedBuffer<Measurement> sensorsMeasurementBuffer;


    //Gestione delle statistiche
    public Statistics stats;                                    //dentro a stats.local ci sono tutte le misurazioni dei figli dei figli
    public HashMap<String,Measurement> childLocalMeans;         //dentro a childLocalMeans ci sono le misurazioni dei figli diretti
    public Measurement localMean;                               //dentro a localMean c'è la media locale
    public final Object statsLock = new Object();

}
