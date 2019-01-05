package EdgeNode;

import EdgeNode.EdgeNetworkMessage.CoordinatorMessage;
import com.google.gson.Gson;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetSocketAddress;

//TODO: gestione statistiche
public class CoordinatorThread extends Thread{

    private EdgeNode parent;
    private SharedDatagramSocket socket;
    private SharedBuffer<CoordinatorMessage> buffer;

    public CoordinatorThread(EdgeNode parent, SharedBuffer<CoordinatorMessage> buffer, SharedDatagramSocket socket) {
        this.parent = parent;
        this.buffer = buffer;
        this.socket = socket;
    }

    public void run(){

        System.out.println("Starting coordinator work!");

        Gson gson = new Gson();

        //TODO: fa partire thread di timeout che scrive al server

        //Legge gli update e risponde
        while (true){
            CoordinatorMessage msg = buffer.take();
            System.out.println("Coordinator msg: "+msg.getMsg());

            CoordinatorMessage response = new CoordinatorMessage(CoordinatorMessage.CoordinatorMessageType.ACK, parent.getRepresentation(), "ACK");
            String json = gson.toJson(response, CoordinatorMessage.class);
            try {
                socket.write(new DatagramPacket(json.getBytes(), json.length(), new InetSocketAddress(msg.getSender().getIpAddr(), msg.getSender().getNodesPort())));
            } catch (IOException e) {
                System.out.println("Coordinator thread got IOException while sending ACK to EdgeNode"+msg.getSender().getNodeId());
                e.printStackTrace();
            }
        }

    }

}
