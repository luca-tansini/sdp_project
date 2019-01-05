package EdgeNode;

import EdgeNode.EdgeNetworkMessage.*;
import ServerCloud.Model.EdgeNodeRepresentation;
import com.google.gson.Gson;

import java.io.IOException;
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
            try {
                socket.read(packet);
            } catch (IOException e){
                System.out.println("EdgeNetworkWorkerThread for EdgeNode"+parent.getNodeId()+" got IOException while reading:");
                continue;
            }
            String json = new String(packet.getData(),0,packet.getLength());

            EdgeNetworkMessage msg = gson.fromJson(json, EdgeNetworkMessage.class);

            switch (msg.getType()){

                case WHOIS_COORD_RESPONSE:
                    handleCoordResponse(json);
                    break;

                case WHOIS_COORD_REQUEST:
                    handleCoordRequest(json);
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

    public void handleCoordResponse(String msg){
        WhoisCoordResponseMessage coordResponseMessage = gson.fromJson(msg, WhoisCoordResponseMessage.class);
        EdgeNodeRepresentation newCoord = coordResponseMessage.getCoordinator();
        parent.setCoordinator(newCoord);
        if(!parent.getNodes().contains(newCoord))
            parent.getNodes().add(newCoord);
    }

    public void handleCoordRequest(String msg){
        WhoisCoordRequestMessage coordRequestMessage = gson.fromJson(msg, WhoisCoordRequestMessage.class);
        EdgeNodeRepresentation requestingNode = coordRequestMessage.getRequestingNode();
        if(parent.isCoordinator()) {
            String jsonResponse = gson.toJson(new WhoisCoordResponseMessage(parent.getRepresentation()));
            try {
                socket.write(new DatagramPacket(jsonResponse.getBytes(), jsonResponse.length(), new InetSocketAddress(requestingNode.getIpAddr(), requestingNode.getNodesPort())));
            } catch (IOException e){
                System.out.println("EdgeNetworkWorkerThread for EdgeNode"+parent.getNodeId()+" got IOException while responding to WHOIS_COORD_REQUEST:");
                e.printStackTrace();
            }
        }
        if(!parent.getNodes().contains(requestingNode))
            parent.getNodes().add(requestingNode);
    }

    public void handleElectionMsg(String msg){
        ElectionMesssage electionMesssage = gson.fromJson(msg, ElectionMesssage.class);


        EdgeNodeRepresentation sender = electionMesssage.getSender();
        if(!parent.getNodes().contains(sender))
            parent.getNodes().add(sender);

        switch (electionMesssage.getElectionMessageType()){

            //Quando ricevo un pacchetto election il sender ha ID minore per forza, gli rispondo con un ALIVE_ACK e inizio un processo di elezione
            case ELECTION:
                String response = gson.toJson(new ElectionMesssage(ElectionMesssage.ElectionMessageType.ALIVE_ACK, parent.getRepresentation()));
                try {
                    socket.write(new DatagramPacket(response.getBytes(), response.length(), new InetSocketAddress(sender.getIpAddr(), sender.getNodesPort())));
                } catch (IOException e){
                    System.out.println("EdgeNetworkWorkerThread for EdgeNode"+parent.getNodeId()+" got IOException while responding to ELECTION_MESSAGE:");
                    e.printStackTrace();
                }
                synchronized (parent.getElectionStatusLock()){
                    if(parent.getElectionStatus() != EdgeNode.ElectionStatus.FINISHED)
                        break;
                    parent.setElectionStatus(EdgeNode.ElectionStatus.STARTED);
                }
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        parent.bullyElection();
                    }
                }).start();
                break;

            //Quando ricevo un pacchetto ACK, mi metto in stato TAKEN_CARE, se non lo ero già. Non posso riceverne da nodi sconosciuti
            case ALIVE_ACK:
                synchronized (parent.getElectionStatusLock()){
                    if(parent.getElectionStatus() != EdgeNode.ElectionStatus.STARTED)
                        break;
                    parent.setElectionStatus(EdgeNode.ElectionStatus.TAKEN_CARE);
                }
                break;

            //Quando ricevo un pacchetto VICTORY posso assumere che sia finita un'elezione e mi segno il nuovo coordinatore.
            case VICTORY:
                synchronized (parent.getElectionStatusLock()){
                    parent.setElectionStatus(EdgeNode.ElectionStatus.FINISHED);
                }
                parent.setCoordinator(sender);
                parent.setAwaitingACK(false);
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
                parent.setAwaitingACK(false);
                break;
        }

    }

}
