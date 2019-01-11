package ServerCloud.Model;

import java.util.ArrayList;

public class Grid {

    public static final String INVALID_POS = "positions must be between 0 and grid limits";
    public static final String ID_TAKEN = "ID already taken";
    public static final String NODE_TOO_CLOSE = "node is too close to another node (distance < 20)";

    private int XSize;
    private int YSize;
    //Don't like this, JAXB sucks
    private NodeList nodes;

    public Grid(int XSize, int YSize){
        this.XSize = XSize;
        this.YSize = YSize;
        this.nodes = new NodeList();
    }

    public NodeList getNodeList(){
        return nodes;
    }

    public ArrayList<EdgeNodeRepresentation> getNodes() {
        return nodes.getNodes();
    }

    public synchronized void addNode(EdgeNodeRepresentation node){

        Position pos = node.getPosition();

        if(pos.getX() < 0 || pos.getX() >= this.XSize || pos.getY() <0 || pos.getY() >= this.YSize)
            throw new IllegalArgumentException(INVALID_POS);

        //Efficienza 0, whatever
        for(EdgeNodeRepresentation e: this.getNodes())
            if(e.getNodeId() == node.getNodeId())
                throw new IllegalArgumentException(ID_TAKEN);

        EdgeNodeRepresentation nearest = getNearestNode(node);
        if(nearest != null && nearest.getDistance(node) < 20){
            throw new IllegalArgumentException(NODE_TOO_CLOSE);
        }
        this.getNodes().add(node);
    }

    public synchronized void removeNode(int nodeId){
        for(EdgeNodeRepresentation n: this.getNodes())
            if(n.getNodeId() == nodeId) {
                this.getNodes().remove(n);
                return;
            }
        throw new IllegalArgumentException("node not found");
    }

    /*TODO: Nearest node non usa meccanismi di sincronizzazione, perchè
    * quando viene chiamata fuori dalla addNode viene chiamata dai sensori
    * e anche se dovesse dare un risultato sbagliato
    * (nodo che non è il più vicino o nodo rimosso)
    * il sensore si sistemerà automaticamente in pochi secondi*/
    public EdgeNodeRepresentation getNearestNode(Position pos){
        EdgeNodeRepresentation nearest = null;
        int nearestDist = this.XSize + this.YSize;
        for(EdgeNodeRepresentation n: this.getNodes()) {
            if(n.getDistance(pos) < nearestDist){
                nearest = n;
                nearestDist = n.getDistance(pos);
            }
        }
        return nearest;
    }

    public EdgeNodeRepresentation getNearestNode(EdgeNodeRepresentation node){
        return getNearestNode(node.getPosition());
    }

}
