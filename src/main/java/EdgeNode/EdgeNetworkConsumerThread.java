package EdgeNode;

import EdgeNode.EdgeNetworkMessage.EdgeNetworkMessage;
import EdgeNode.EdgeNetworkMessage.WhoisCoordRequestMessage;
import EdgeNode.EdgeNetworkMessage.WhoisCoordResponseMessage;
import ServerCloud.Model.EdgeNodeRepresentation;
import com.google.gson.Gson;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetSocketAddress;

public class EdgeNetworkConsumerThread extends Thread {

    EdgeNode parent;
    SharedDatagramSocket socket;

    public EdgeNetworkConsumerThread(EdgeNode parent, SharedDatagramSocket socket) {
        this.parent = parent;
        this.socket = socket;
    }

    public void run(){

        Gson gson = new Gson();
        byte buf[] = new byte[1024];

        while(!parent.isShutdown()){

            DatagramPacket packet = new DatagramPacket(buf, buf.length);
            try {
                socket.read(packet);
            } catch (IOException e){
                System.out.println("EdgeNetworkConsumerThread for EdgeNode"+parent.getNodeId()+" got IOException while reading:");
                continue;
            }
            String json = new String(packet.getData(),0,packet.getLength());
            EdgeNetworkMessage msg = gson.fromJson(json, EdgeNetworkMessage.class);

            switch (msg.getType()){
                case WHOIS_COORD_RESPONSE:
                    WhoisCoordResponseMessage coordResponseMessage = gson.fromJson(json, WhoisCoordResponseMessage.class);
                    parent.setCoordinator(coordResponseMessage.getCoordinator());
                    break;

                case WHOIS_COORD_REQUEST:
                    WhoisCoordRequestMessage coordRequestMessage = gson.fromJson(json, WhoisCoordRequestMessage.class);
                    EdgeNodeRepresentation requestingNode = coordRequestMessage.getRequestingNode();
                    if(parent.isCoordinator()) {
                        String jsonResponse = gson.toJson(new WhoisCoordResponseMessage(parent.getRepresentation()));
                        try {
                            socket.write(new DatagramPacket(jsonResponse.getBytes(), jsonResponse.length(), new InetSocketAddress(requestingNode.getIpAddr(), requestingNode.getNodesPort())));
                        } catch (IOException e){
                            System.out.println("EdgeNetworkConsumerThread for EdgeNode"+parent.getNodeId()+" got IOException while responding to WHOIS_COORD_REQUEST:");
                            e.printStackTrace();
                        }
                    }
                    break;

                default:
                    System.out.println("EdgeNetworkConsumerThread for EdgeNode"+parent.getNodeId()+ " got unknown request");
                    break;
            }
        }
    }
}
