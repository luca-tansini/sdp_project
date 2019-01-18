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

                case TREE:
                    handleTreeMsg(json);
                    break;

                case QUIT:
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
            //Se il padre è il coordinatore/radice devo aggiungere il nodo nell'albero e rispondere con le info su coordinatore e ruolo nell'albero
            stateModel.parent.addNetworkTreeNode(requestingNode);
        }
    }

    //Quando mi arriva una HELLO_RESPONSE sono per forza una foglia
    public void handleHelloResponse(String msg){
        HelloResponseMessage coordResponseMessage = gson.fromJson(msg, HelloResponseMessage.class);
        EdgeNodeRepresentation coord = coordResponseMessage.getCoordinator();
        //Si segna chi sono il parent e il coordinatore e fa partire la comunicazione con i sensori
        stateModel.setNetworkTreeParent(coordResponseMessage.getParent());
        stateModel.setCoordinator(coord);
        stateModel.parent.startSensorsCommunication();
        synchronized (stateModel.helloSequenceLock) {
            stateModel.helloSequenceLock.notify();
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
                //Controlla che il pacchetto victory non sia vecchio
                if(electionMesssage.getTimestamp() <= stateModel.getLastElectionTimestamp()){
                    break;
                }
                synchronized (stateModel.electionStatusLock){
                    stateModel.electionStatus = StateModel.ElectionStatus.FINISHED;
                }
                stateModel.setAwaitingCoordinatorACK(false);
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


    public void handleCoordinatorMsg(String msg){

        CoordinatorMessage coordinatorMessage = gson.fromJson(msg, CoordinatorMessage.class);

        switch (coordinatorMessage.getCoordinatorMessageType()){

            case STATS_UPDATE:
                if (stateModel.parent.isCoordinator())
                    stateModel.coordinatorBuffer.put(coordinatorMessage);
                break;

            case ACK:
                stateModel.setAwaitingCoordinatorACK(false);
                synchronized (stateModel.statsLock) {
                    stateModel.stats.setGlobal(coordinatorMessage.getMeasurement());
                }
                break;
        }
    }

    private void handleTreeMsg(String msg) {

        TreeMessage treeMessage = gson.fromJson(msg, TreeMessage.class);

        switch (treeMessage.getTreeNodeType()){

            /*
             * TODO: è possibile ricevere una demozione? Cioè passare da nodo interno a foglia?
             * Dipende se voglio implementare il riconoscimento dell'abbandono dei nodi figli
             */
            case LEAF:
                //Si segna chi è il parent e fa partire la comunicazione con i sensori
                stateModel.setNetworkTreeParent(treeMessage.getParent());
                stateModel.parent.startSensorsCommunication();
                break;

            case INTERNAL:
                stateModel.parent.stopSensorsCommunication();
                stateModel.parent.startInternalNodeWork();
                break;

        }

    }

}
