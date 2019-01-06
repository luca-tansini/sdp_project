package EdgeNode;

import ServerCloud.Model.EdgeNodeRepresentation;

import java.util.ArrayList;
import java.util.Iterator;

public class SharedNodeList implements Iterable<EdgeNodeRepresentation>{

    private ArrayList<EdgeNodeRepresentation> nodes;

    public SharedNodeList(ArrayList<EdgeNodeRepresentation> nodes){
        this.nodes = nodes;
    }

    public synchronized void remove(EdgeNodeRepresentation node){
        this.nodes.remove(node);
    }

    public synchronized int size(){
        return this.nodes.size();
    }

    public synchronized EdgeNodeRepresentation get(int i) {
        return this.nodes.get(i);
    }

    public synchronized void add(EdgeNodeRepresentation node){
        this.nodes.add(node);
    }

    public synchronized boolean contains(EdgeNodeRepresentation node){
        return this.nodes.contains(node);
    }

    @Override
    public Iterator<EdgeNodeRepresentation> iterator(){
        return this.nodes.iterator();
    }

}
