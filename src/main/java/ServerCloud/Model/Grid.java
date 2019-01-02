package ServerCloud.Model;

import java.util.ArrayList;

public class Grid {

    private int XSize;
    private int YSize;
    private ArrayList<GridNode> nodes;

    public Grid(int XSize, int YSize){
        this.XSize = XSize;
        this.YSize = YSize;
        this.nodes = new ArrayList<GridNode>();
    }

    public void addNode(GridNode node){
        //TODO: Controlli sulla validità delle posizioni?
        GridNode nearest = getNearestNode(node);
        if(nearest != null && nearest.getDistance(node) < 20){
            //TODO: Solleva un'eccezione
        }
        nodes.add(node);
    }

    public void removeNode(int nodeId){
        for(GridNode n: this.nodes)
            if(n.getNodeId() == nodeId) {
                this.nodes.remove(n);
                break;
            }
    }

    public GridNode getNearestNode(int xPos, int yPos){
        GridNode nearest = null;
        int nearestDist = this.XSize + this.YSize;
        for(GridNode n: this.nodes) {
            if(n.getDistance(xPos, yPos) < nearestDist){
                nearest = n;
                nearestDist = n.getDistance(xPos, yPos);
            }
        }
        return nearest;
    }

    public GridNode getNearestNode(GridNode node){
        return getNearestNode(node.getxPos(), node.getyPos());
    }

}
