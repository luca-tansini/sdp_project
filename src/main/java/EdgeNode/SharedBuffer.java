package EdgeNode;

import java.util.ArrayList;

public class SharedBuffer<T> {

    private ArrayList<T> buffer;

    public SharedBuffer(){
        this.buffer = new ArrayList<T>();
    }

    public synchronized void put(T message) {
        buffer.add(message);
        notify();
    }
    public synchronized T take() {
        T message = null;
        while(buffer.size() == 0) {
            try { wait(); }
            catch (InterruptedException e) { e.printStackTrace(); }
        }
        if(buffer.size() > 0) {
            message = buffer.get(0);
            buffer.remove(0);
        }
        return message;
    }
}
