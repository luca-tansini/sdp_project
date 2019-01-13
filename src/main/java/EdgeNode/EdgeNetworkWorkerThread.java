package EdgeNode;

import EdgeNode.EdgeNetworkMessage.*;
import ServerCloud.Model.EdgeNodeRepresentation;
import com.google.gson.Gson;

import java.net.DatagramPacket;
import java.net.InetSocketAddress;

public class EdgeNetworkWorkerThread extends Thread {

    Gson gson;
    StateModel stateModel;

    public EdgeNetworkWorkerThread() {
        this.gson = new Gson();
        this.stateModel = StateModel.getInstance();
    }

    public void run(){

        byte buf[] = new byte[1024];

        while(!stateModel.shutdown){

            DatagramPacket packet = new DatagramPacket(buf, buf.length);
            stateModel.edgeNetworkSocket.read(packet);
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
                    System.out.println("EdgeNetworkWorkerThread for EdgeNode"+stateModel.parent.getNodeId()+ " got unknown request");
                    break;
            }
        }
    }


    public void handleHelloRequest(String msg){
        HelloMessage helloMessage = gson.fromJson(msg, HelloMessage.class);
        EdgeNodeRepresentation requestingNode = helloMessage.getRequestingNode();
        if(!stateModel.nodes.contains(requestingNode))
            stateModel.nodes.add(requestingNode);
        else{
            //Caso limite in cui qualcuno è morto e risorto prima che me ne rendessi conto
            stateModel.nodes.addSafety(requestingNode);
        }
        if(stateModel.parent.isCoordinator()) {
            String jsonResponse = gson.toJson(new HelloResponseMessage(stateModel.parent.getRepresentation()));
            stateModel.edgeNetworkSocket.write(new DatagramPacket(jsonResponse.getBytes(), jsonResponse.length(), new InetSocketAddress(requestingNode.getIpAddr(), requestingNode.getNodesPort())));
        }
    }


    public void handleHelloResponse(String msg){
        HelloResponseMessage coordResponseMessage = gson.fromJson(msg, HelloResponseMessage.class);
        EdgeNodeRepresentation coord = coordResponseMessage.getCoordinator();
        synchronized (stateModel.helloSequenceLock) {
            if (coord != null) {
                stateModel.setCoordinator(coord);
                stateModel.helloSequenceLock.notify();
            }
        }
    }


    public void handleElectionMsg(String msg){
        ElectionMesssage electionMesssage = gson.fromJson(msg, ElectionMesssage.class);

        EdgeNodeRepresentation sender = electionMesssage.getSender();

        switch (electionMesssage.getElectionMessageType()) {

            //Quando ricevo un pacchetto election rispondo con un ALIVE_ACK e faccio partire un'elezione, se non ce n'è già una in corso
            case ELECTION:
                String response = gson.toJson(new ElectionMesssage(ElectionMesssage.ElectionMessageType.ALIVE_ACK, stateModel.parent.getRepresentation()));
                stateModel.edgeNetworkSocket.write(new DatagramPacket(response.getBytes(), response.length(), new InetSocketAddress(sender.getIpAddr(), sender.getNodesPort())));
                synchronized (stateModel.electionStatusLock) {
                    if (stateModel.electionStatus != StateModel.ElectionStatus.FINISHED)
                        break;
                    stateModel.electionStatus = StateModel.ElectionStatus.STARTED;
                }
                System.out.println("DEBUG: ho ricevuto pacchetto election, faccio partire elezione da WorkerThread");
                //Gestione della helloSequence
                synchronized (stateModel.helloSequenceLock) {
                    stateModel.helloSequenceLock.notify();
                }
                stateModel.setAwaitingCoordinatorACK(false);
                stateModel.setCoordinator(null);
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        stateModel.parent.bullyElection();
                    }
                }).start();
                break;

            //Quando ricevo un pacchetto ACK, mi metto in stato TAKEN_CARE, se non lo ero già.
            case ALIVE_ACK:
                synchronized (stateModel.electionStatusLock) {
                    if (stateModel.electionStatus != StateModel.ElectionStatus.STARTED)
                        break;
                    stateModel.electionStatus = StateModel.ElectionStatus.TAKEN_CARE;
                }
                synchronized (stateModel.electionLock){
                    stateModel.electionLock.notify();
                }
                break;

            //Quando ricevo un pacchetto VICTORY posso assumere che sia finita un'elezione e mi segno il nuovo coordinatore.
            case VICTORY:
                System.out.println("DEBUG: EdgeNetworkWokerThread ho ricevuto pacchetto VICTORY");
                synchronized (stateModel.electionStatusLock){
                    stateModel.electionStatus = StateModel.ElectionStatus.FINISHED;
                }
                stateModel.setAwaitingCoordinatorACK(false);
                stateModel.setCoordinator(sender);
                synchronized (stateModel.electionLock){
                    stateModel.electionLock.notify();
                }
                //gestione della helloSequence
                synchronized (stateModel.helloSequenceLock) {
                    stateModel.helloSequenceLock.notify();
                }
                break;
        }
    }


    public void handleCoordinatorMsg(String msg){

        CoordinatorMessage coordinatorMessage = gson.fromJson(msg, CoordinatorMessage.class);

        switch (coordinatorMessage.getCoordinatorMessageType()){

            case STATS_UPDATE:
                stateModel.coordinatorBuffer.put(coordinatorMessage);
                break;

            case ACK:
                stateModel.setAwaitingCoordinatorACK(false);
                break;
        }
    }
}
