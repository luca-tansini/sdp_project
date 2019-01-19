package EdgeNode;

import EdgeNode.EdgeNetworkMessage.*;
import ServerCloud.Model.EdgeNodeRepresentation;
import com.google.gson.Gson;

import java.net.DatagramPacket;
import java.net.InetSocketAddress;

public class EdgeNetworkWorkerThread extends Thread {

    private Gson gson;
    private StateModel stateModel;

    public EdgeNetworkWorkerThread() {
        this.gson = new Gson();
        this.stateModel = StateModel.getInstance();
    }

    public void run(){

        byte[] buf = new byte[1024];

        while(stateModel.edgeNetworkOnline){

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

                case PARENT:
                    handleParentMsg(json);
                    break;

                case TREE:
                    handleTreeMsg(json);
                    break;

                case QUIT:
                    break;
            }
        }
    }


    private void handleHelloRequest(String msg){
        HelloMessage helloMessage = gson.fromJson(msg, HelloMessage.class);
        EdgeNodeRepresentation requestingNode = helloMessage.getRequestingNode();
        if(!stateModel.nodes.contains(requestingNode))
            stateModel.nodes.add(requestingNode);
        else{
            //Caso limite in cui qualcuno è morto e risorto prima che me ne rendessi conto
            stateModel.nodes.addSafety(requestingNode);
        }
        if(stateModel.edgeNode.isCoordinator()) {
            //Se il padre è il coordinatore/radice devo aggiungere il nodo nell'albero e rispondere con le info su coordinatore e ruolo nell'albero
            stateModel.edgeNode.addNetworkTreeNode(requestingNode);
        }
    }

    //Quando mi arriva una HELLO_RESPONSE sono per forza una foglia
    private void handleHelloResponse(String msg){
        HelloResponseMessage coordResponseMessage = gson.fromJson(msg, HelloResponseMessage.class);
        EdgeNodeRepresentation coord = coordResponseMessage.getCoordinator();
        //Si segna chi sono il edgeNode e il coordinatore e fa partire la comunicazione con i sensori
        stateModel.setNetworkTreeParent(coordResponseMessage.getParent());
        stateModel.setCoordinator(coord);
        stateModel.edgeNode.startSensorsCommunication();
        synchronized (stateModel.helloSequenceLock) {
            stateModel.helloSequenceLock.notify();
        }
    }


    private void handleElectionMsg(String msg){
        ElectionMesssage electionMesssage = gson.fromJson(msg, ElectionMesssage.class);

        EdgeNodeRepresentation sender = electionMesssage.getSender();

        switch (electionMesssage.getElectionMessageType()) {

            //Quando ricevo un pacchetto election rispondo con un ALIVE_ACK e faccio partire un'elezione, se non ce n'è già una in corso
            case ELECTION:
                String response = gson.toJson(new ElectionMesssage(ElectionMesssage.ElectionMessageType.ALIVE_ACK, stateModel.edgeNode.getRepresentation()));
                stateModel.edgeNetworkSocket.write(new DatagramPacket(response.getBytes(), response.length(), new InetSocketAddress(sender.getIpAddr(), sender.getNodesPort())));
                //Controlla che il pacchetto election non sia vecchio
                if(electionMesssage.getTimestamp() <= stateModel.getLastElectionTimestamp()){
                    break;
                }
                synchronized (stateModel.electionStatusLock) {
                    if (stateModel.electionStatus != StateModel.ElectionStatus.FINISHED)
                        break;
                    stateModel.electionStatus = StateModel.ElectionStatus.STARTED;
                }
                //Gestione della helloSequence
                synchronized (stateModel.helloSequenceLock) {
                    stateModel.helloSequenceLock.notify();
                }
                stateModel.setAwaitingParentACK(false);
                stateModel.setNetworkTreeParent(null);
                stateModel.setCoordinator(null);
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        stateModel.edgeNode.bullyElection();
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
                //Controlla che il pacchetto victory non sia vecchio
                if(electionMesssage.getTimestamp() <= stateModel.getLastElectionTimestamp()){
                    break;
                }
                synchronized (stateModel.electionStatusLock){
                    stateModel.electionStatus = StateModel.ElectionStatus.FINISHED;
                }
                stateModel.setAwaitingParentACK(false);
                //Setto il parent a null perchè appena il nuovo coordinatore ha ricostruito l'albero mi manderà un pacchetto TREE
                stateModel.setNetworkTreeParent(null);
                stateModel.setCoordinator(sender);
                stateModel.setLastElectionTimestamp(electionMesssage.getTimestamp());
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


    public void handleParentMsg(String msg){

        ParentMessage parentMessage = gson.fromJson(msg, ParentMessage.class);

        switch (parentMessage.getParentMessageType()){

            case STATS_UPDATE:
                if (stateModel.edgeNode.isCoordinator())
                    stateModel.internalNodeBuffer.put(parentMessage);
                break;

            case ACK:
                stateModel.setAwaitingParentACK(false);
                synchronized (stateModel.statsLock) {
                    //TODO: non bellissimo che nei pacchetti ACK il dato si chiami in modo sbagliato
                    stateModel.stats.setGlobal(parentMessage.getLocalmean());
                }
                break;
        }
    }

    private void handleTreeMsg(String msg) {

        TreeMessage treeMessage = gson.fromJson(msg, TreeMessage.class);

        switch (treeMessage.getTreeMessageType()){

            case LEAF:
                System.out.println("DEBUG: EdgeNetworkWorkerThread - LEAF");
                stateModel.setNetworkTreeParent(treeMessage.getParent());
                if(!stateModel.edgeNode.isCoordinator())
                    stateModel.edgeNode.stopInternalNodeWork();
                stateModel.setAwaitingParentACK(false);
                stateModel.edgeNode.startSensorsCommunication();
                break;

            case INTERNAL:
                System.out.println("DEBUG: EdgeNetworkWorkerThread - INTERNAL NODE");
                //Il parent non cambia
                stateModel.edgeNode.stopSensorsCommunication();
                stateModel.edgeNode.startInternalNodeWork();
                break;

            case PARENT_DOWN:
                System.out.println("DEBUG: EdgeNetworkWorkerThread - got PARENT_DOWN from: "+treeMessage.getSender().getNodeId());
                stateModel.edgeNode.networkTreeNodeParentDown(treeMessage.getSender(),treeMessage.getParent());
                break;

        }
    }
}
