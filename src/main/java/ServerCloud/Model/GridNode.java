package ServerCloud.Model;

import java.lang.Math;

public class GridNode {

    private int xPos;
    private int yPos;
    private int nodeId;

    public GridNode(int xPos, int yPos, int nodeId) {
        this.xPos = xPos;
        this.yPos = yPos;
        this.nodeId = nodeId;
    }

    public int getxPos() {
        return xPos;
    }

    public int getyPos() {
        return yPos;
    }

    public int getNodeId() {
        return nodeId;
    }

    public int getDistance(int xPos, int yPos){
        return Math.abs(xPos - this.xPos) + Math.abs(yPos - this.yPos);
    }

    public  int getDistance(GridNode other){
        return getDistance(other.getxPos(), other.getyPos());
    }

}
