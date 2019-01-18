package ServerCloud.Model;

import EdgeNode.EdgeNode;

import java.util.ArrayList;
import java.util.Iterator;

public class NodeList implements Iterable<EdgeNodeRepresentation>{

    private ArrayList<EdgeNodeRepresentation> nodes;

    public NodeList(){
        this.nodes = new ArrayList<>();
    }

    public synchronized ArrayList<EdgeNodeRepresentation> getNodes(){
        return (ArrayList<EdgeNodeRepresentation>) nodes.clone();
    }

    public synchronized void setNodes(ArrayList<EdgeNodeRepresentation> nodes) {
        this.nodes = nodes;
    }

    public synchronized void add(EdgeNodeRepresentation node){
        this.nodes.add(node);
    }

    public synchronized int size(){
        return this.nodes.size();
    }

    public synchronized boolean contains(EdgeNodeRepresentation node){
        return this.nodes.contains(node);
    }

    public synchronized boolean contains(int id){
        for(EdgeNodeRepresentation n: nodes)
            if(n.getNodeId() == id)
                return true;
        return false;
    }

    public synchronized EdgeNodeRepresentation getById(int id){
        for(EdgeNodeRepresentation node: nodes)
            if(node.getNodeId() == id) return node;
        return null;
    }

    public synchronized EdgeNodeRepresentation get(int i) {
        return this.nodes.get(i);
    }

    public synchronized void remove(EdgeNodeRepresentation node){
        this.nodes.remove(node);
    }

    public synchronized void removeById(int id){
        for(EdgeNodeRepresentation n: nodes)
            if(n.getNodeId() == id) {
                nodes.remove(n);
                return;
            }
    }

    @Override
    public synchronized Iterator<EdgeNodeRepresentation> iterator(){
        ArrayList<EdgeNodeRepresentation> tmp = (ArrayList<EdgeNodeRepresentation>) this.nodes.clone();
        return tmp.iterator();
    }

}
