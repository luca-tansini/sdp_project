package EdgeNode;

import EdgeNode.EdgeNetworkMessage.CoordinatorMessage;
import ServerCloud.Model.Measurement;
import com.google.gson.Gson;

import java.net.DatagramPacket;
import java.net.InetSocketAddress;

//TODO: gestione statistiche
public class CoordinatorThread extends Thread{

    private EdgeNode parent;
    private SharedDatagramSocket socket;
    private SharedBuffer<CoordinatorMessage> buffer;
    private Measurement global;

    public CoordinatorThread(EdgeNode parent, SharedBuffer<CoordinatorMessage> buffer, SharedDatagramSocket socket) {
        this.parent = parent;
        this.buffer = buffer;
        this.socket = socket;
        //MOCK
        this.global = new Measurement(42,42);
    }

    public void run(){

        System.out.println("Starting coordinator work!");

        Gson gson = new Gson();

        //TODO: fa partire thread di timeout che scrive al server

        //Legge gli update e risponde
        while (true){
            CoordinatorMessage msg = buffer.take();
            System.out.println("Coordinator msg: "+msg.getMeasurement());
            CoordinatorMessage response = new CoordinatorMessage(CoordinatorMessage.CoordinatorMessageType.ACK, parent.getRepresentation(), global);
            String json = gson.toJson(response, CoordinatorMessage.class);
            socket.write(new DatagramPacket(json.getBytes(), json.length(), new InetSocketAddress(msg.getSender().getIpAddr(), msg.getSender().getNodesPort())));
        }
    }
}
