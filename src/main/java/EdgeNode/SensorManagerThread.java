package EdgeNode;

import EdgeNode.EdgeNetworkMessage.CoordinatorMessage;
import com.google.gson.Gson;

import java.net.DatagramPacket;
import java.net.InetSocketAddress;

public class SensorManagerThread extends Thread {

    EdgeNode parent;
    SharedDatagramSocket socket;

    public SensorManagerThread(EdgeNode parent, SharedDatagramSocket socket){
        this.parent = parent;
        this.socket = socket;
    }

    @Override
    public void run() {

        Gson gson = new Gson();

        while (true){
            try {
                //Se il coordinatore non è definito non fa nulla
                if(parent.getCoordinator() != null) {
                    System.out.println("SensorManagerThread is sending STATS_UPDATE: random message from " + parent.getNodeId());
                    CoordinatorMessage coordinatorMessage = new CoordinatorMessage(CoordinatorMessage.CoordinatorMessageType.STATS_UPDATE, parent.getRepresentation(), "random message from " + parent.getNodeId());
                    String json = gson.toJson(coordinatorMessage, CoordinatorMessage.class);
                    socket.write(new DatagramPacket(json.getBytes(), json.length(), new InetSocketAddress(parent.getCoordinator().getIpAddr(), parent.getCoordinator().getNodesPort())));
                    parent.setAwaitingCoordinatorACK(true);
                }
                Thread.sleep(5000);
                if(parent.isAwaitingCoordinatorACK() == true){
                    System.out.println("DEBUG: Il coordinatore è morto!");
                    parent.setCoordinator(null);
                    parent.setAwaitingCoordinatorACK(false);
                    synchronized (parent.getElectionStatusLock()){
                        if(parent.getElectionStatus() != EdgeNode.ElectionStatus.FINISHED)
                            continue;
                        parent.setElectionStatus(EdgeNode.ElectionStatus.STARTED);
                    }
                    System.out.println("DEBUG: SensorManagerThread: faccio partire elezione");
                    parent.bullyElection();
                }
            } catch (Exception e){
                e.printStackTrace();
            }
        }
    }

}
