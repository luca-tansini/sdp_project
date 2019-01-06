package EdgeNode;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

public class SharedDatagramSocket {

    private DatagramSocket socket;
    private Object readLock;
    private Object writeLock;

    public SharedDatagramSocket(DatagramSocket socket){
        this.socket = socket;
        readLock = new Object();
        writeLock = new Object();
    }

    public void read(DatagramPacket packet) {
        synchronized (readLock){
            try {
                this.socket.receive(packet);
            } catch (IOException e){
                System.out.println("socket error in receive:");
                e.printStackTrace();
            }
        }
    }

    public void write(DatagramPacket packet) {
        synchronized (writeLock){
            try {
                this.socket.send(packet);
            } catch (IOException e){
                System.out.println("socket error sending packet: "+new String(packet.getData(), 0, packet.getLength()));
                e.printStackTrace();
            }
        }
    }

    public void close(){
        this.socket.close();
    }
}
