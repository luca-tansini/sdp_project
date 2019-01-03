package ServerCloud.Model;

import sun.security.x509.IPAddressName;

import javax.xml.bind.annotation.XmlRootElement;
import java.lang.Math;

@XmlRootElement
public class EdgeNode {

    private int xPos;
    private int yPos;
    private int nodeId;
    private String ipAddr;
    private int sensorsPort;
    private int nodesPort;


    public EdgeNode(){}

    public EdgeNode(int xPos, int yPos, int nodeId) {
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

    public  int getDistance(EdgeNode other){
        return getDistance(other.getxPos(), other.getyPos());
    }

    public String getIpAddr() {
        return ipAddr;
    }

    public int getSensorsPort() {
        return sensorsPort;
    }

    public int getNodesPort() {
        return nodesPort;
    }
}
