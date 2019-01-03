package ServerCloud.Model;

import java.util.ArrayList;

public class Grid {

    private int XSize;
    private int YSize;
    private ArrayList<EdgeNode> nodes;

    public Grid(int XSize, int YSize){
        this.XSize = XSize;
        this.YSize = YSize;
        this.nodes = new ArrayList<EdgeNode>();
    }

    public ArrayList<EdgeNode> getNodes() {
        return nodes;
    }

    public synchronized void addNode(EdgeNode node){

        if(node.getxPos() < 0 || node.getxPos() >= this.XSize || node.getyPos() <0 || node.getyPos() >= this.YSize)
            throw new IllegalArgumentException("positions must be between 0 and grid limits");

        //Efficienza 0, whatever
        for(EdgeNode e: this.nodes)
            if(e.getNodeId() == node.getNodeId())
                throw new IllegalArgumentException("ID already taken");

        EdgeNode nearest = getNearestNode(node);
        if(nearest != null && nearest.getDistance(node) < 20){
            throw new IllegalArgumentException("node is too close to another node (distance < 20)");
        }
        nodes.add(node);
    }

    public synchronized void removeNode(int nodeId){
        for(EdgeNode n: this.nodes)
            if(n.getNodeId() == nodeId) {
                this.nodes.remove(n);
                return;
            }
        throw new IllegalArgumentException("node not found");
    }

    /*Nearest node non usa meccanismi di sincronizzazione, perchè
    * quando viene chiamata fuori dalla addNode viene chiamata dai sensori
    * e anche se dovesse dare un risultato sbagliato
    * (nodo che non è il più vicino o nodo rimosso)
    * il sensore si sistemerà automaticamente in pochi secondi*/
    public EdgeNode getNearestNode(int xPos, int yPos){
        EdgeNode nearest = null;
        int nearestDist = this.XSize + this.YSize;
        for(EdgeNode n: this.nodes) {
            if(n.getDistance(xPos, yPos) < nearestDist){
                nearest = n;
                nearestDist = n.getDistance(xPos, yPos);
            }
        }
        return nearest;
    }

    public EdgeNode getNearestNode(EdgeNode node){
        return getNearestNode(node.getxPos(), node.getyPos());
    }

}
