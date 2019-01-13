package EdgeNode;

import ServerCloud.Model.EdgeNodeRepresentation;

import java.util.ArrayList;
import java.util.Iterator;

public class SharedNodeList implements Iterable<EdgeNodeRepresentation>{

    private ArrayList<EdgeNodeRepresentation> nodes;
    private ArrayList<EdgeNodeRepresentation> safetyList;

    public SharedNodeList(ArrayList<EdgeNodeRepresentation> nodes){
        this.nodes = nodes;
        this.safetyList = new ArrayList<>();
    }

    public synchronized void remove(EdgeNodeRepresentation node){
        if(this.safetyList.contains(node)){
            this.safetyList.remove(node);
        }
        else
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

    public synchronized void addSafety(EdgeNodeRepresentation node){
        this.safetyList.add(node);
    }

    public synchronized boolean contains(EdgeNodeRepresentation node){
        return this.nodes.contains(node);
    }

    @Override
    public synchronized Iterator<EdgeNodeRepresentation> iterator(){
        return this.nodes.iterator();
    }

}
