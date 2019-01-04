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

    public void read(DatagramPacket packet) throws IOException {
        synchronized (readLock){
            this.socket.receive(packet);
        }
    }

    public void write(DatagramPacket packet) throws IOException{
        synchronized (writeLock){
            this.socket.send(packet);
        }
    }

    public void close(){
        this.socket.close();
    }
}
