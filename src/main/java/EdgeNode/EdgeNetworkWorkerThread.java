package EdgeNode;

import EdgeNode.EdgeNetworkMessage.*;
import ServerCloud.Model.EdgeNodeRepresentation;
import com.google.gson.Gson;

import java.net.DatagramPacket;
import java.net.InetSocketAddress;

public class EdgeNetworkWorkerThread extends Thread {

    EdgeNode parent;
    SharedDatagramSocket socket;
    Gson gson;

    public EdgeNetworkWorkerThread(EdgeNode parent, SharedDatagramSocket socket) {
        this.parent = parent;
        this.socket = socket;
        gson = new Gson();
    }

    public void run(){

        byte buf[] = new byte[1024];

        while(!parent.isShutdown()){

            DatagramPacket packet = new DatagramPacket(buf, buf.length);
            socket.read(packet);
            String json = new String(packet.getData(),0,packet.getLength());

            EdgeNetworkMessage msg = gson.fromJson(json, EdgeNetworkMessage.class);

            switch (msg.getType()){

                case HELLO:
                    handleHelloRequest(json);
                    break;

                case HELLO_RESPONSE:
                    handleHelloResponse(json);
                    break;

                case ELECTION:
                    handleElectionMsg(json);
                    break;

                case COORDINATOR:
                    handleCoordinatorMsg(json);
                    break;

                default:
                    System.out.println("EdgeNetworkWorkerThread for EdgeNode"+parent.getNodeId()+ " got unknown request");
                    break;
            }
        }
    }

    public void handleHelloRequest(String msg){
        HelloMessage helloMessage = gson.fromJson(msg, HelloMessage.class);
        EdgeNodeRepresentation requestingNode = helloMessage.getRequestingNode();
        if(!parent.getNodes().contains(requestingNode))
            parent.getNodes().add(requestingNode);
        else{
            //Caso limite in cui qualcuno è morto e risorto prima che me ne rendessi conto
            parent.getNodes().addSafety(requestingNode);
        }
        if(parent.isCoordinator()) {
            String jsonResponse = gson.toJson(new HelloResponseMessage(parent.getRepresentation()));
            socket.write(new DatagramPacket(jsonResponse.getBytes(), jsonResponse.length(), new InetSocketAddress(requestingNode.getIpAddr(), requestingNode.getNodesPort())));
        }
    }

    public void handleHelloResponse(String msg){
        HelloResponseMessage coordResponseMessage = gson.fromJson(msg, HelloResponseMessage.class);
        EdgeNodeRepresentation coord = coordResponseMessage.getCoordinator();
        Object lock = parent.getHelloSequenceLock();
        synchronized (lock) {
            if (coord != null) {
                parent.setCoordinator(coord);
                lock.notify();
            }
        }
    }

    public void handleElectionMsg(String msg){
        ElectionMesssage electionMesssage = gson.fromJson(msg, ElectionMesssage.class);

        EdgeNodeRepresentation sender = electionMesssage.getSender();

        switch (electionMesssage.getElectionMessageType()) {

            case ELECTION:
                String response = gson.toJson(new ElectionMesssage(ElectionMesssage.ElectionMessageType.ALIVE_ACK, parent.getRepresentation()));
                socket.write(new DatagramPacket(response.getBytes(), response.length(), new InetSocketAddress(sender.getIpAddr(), sender.getNodesPort())));
                synchronized (parent.getElectionStatusLock()) {
                    if (parent.getElectionStatus() != EdgeNode.ElectionStatus.FINISHED)
                        break;
                    parent.setElectionStatus(EdgeNode.ElectionStatus.STARTED);
                }
                System.out.println("DEBUG: ho ricevuto pacchetto election, faccio partire elezione da WorkerThread");
                //gestione della helloSequence
                synchronized (parent.getHelloSequenceLock()) {
                    parent.getHelloSequenceLock().notify();
                }
                parent.setAwaitingCoordinatorACK(false);
                parent.setCoordinator(null);
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        parent.bullyElection();
                    }
                }).start();
                break;

            //Quando ricevo un pacchetto ACK, mi metto in stato TAKEN_CARE, se non lo ero già. Non posso riceverne da nodi sconosciuti
            case ALIVE_ACK:
                synchronized (parent.getElectionStatusLock()) {
                    if (parent.getElectionStatus() != EdgeNode.ElectionStatus.STARTED)
                        break;
                    parent.setElectionStatus(EdgeNode.ElectionStatus.TAKEN_CARE);
                }
                synchronized (parent.getElectionLock()){
                    parent.getElectionLock().notify();
                }
                break;

            //Quando ricevo un pacchetto VICTORY posso assumere che sia finita un'elezione e mi segno il nuovo coordinatore.
            case VICTORY:
                System.out.println("DEBUG: EdgeNetworkWokerThread ho ricevuto pacchetto VICTORY");
                synchronized (parent.getElectionStatusLock()){
                    parent.setElectionStatus(EdgeNode.ElectionStatus.FINISHED);
                }
                parent.setAwaitingCoordinatorACK(false);
                parent.setCoordinator(sender);
                synchronized (parent.getElectionLock()) {
                    parent.getElectionLock().notify();
                }
                //gestione della helloSequence
                synchronized (parent.getHelloSequenceLock()){
                    parent.getHelloSequenceLock().notify();
                }
                break;
        }
    }

    public void handleCoordinatorMsg(String msg){

        CoordinatorMessage coordinatorMessage = gson.fromJson(msg, CoordinatorMessage.class);

        switch (coordinatorMessage.getCoordinatorMessageType()){

            case STATS_UPDATE:
                parent.getCoordinatorBuffer().put(coordinatorMessage);
                break;

            case ACK:
                parent.setAwaitingCoordinatorACK(false);
                break;
        }

    }

}
