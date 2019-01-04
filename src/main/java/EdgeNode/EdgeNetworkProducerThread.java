package EdgeNode;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

public class EdgeNetworkProducerThread extends Thread {

    EdgeNode parent;
    SharedBuffer<String> buffer;
    DatagramSocket socket;

    public EdgeNetworkProducerThread(EdgeNode parent, SharedBuffer<String> buffer, DatagramSocket socket) {
        this.parent = parent;
        this.buffer = buffer;
        this.socket = socket;
    }

    public void run(){

        //Per le statistiche aggregate potrebbe non bastare?
        byte in[] = new byte[1024];
        final DatagramPacket datagramPacket = new DatagramPacket(in, in.length);

        while(!parent.isShutdown()){

            try{

                socket.receive(datagramPacket);
                String msg = new String(datagramPacket.getData());
                //DEBUG
                System.out.println(msg);
                //END DEBUG
                buffer.put(msg);

            } catch (IOException e){
                System.out.println("EdgeNetworkProducerThread for EdgeNode"+parent.getNodeId()+" got IOException:");
                e.printStackTrace();
            }

        }

    }

}
