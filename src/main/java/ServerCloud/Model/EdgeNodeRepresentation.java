package ServerCloud.Model;

import javax.xml.bind.annotation.XmlRootElement;
import java.lang.Math;

@XmlRootElement
public class EdgeNodeRepresentation {

    private int xPos;
    private int yPos;
    private int nodeId;
    private String ipAddr;
    private int sensorsPort;
    private int nodesPort;

    public EdgeNodeRepresentation(int xPos, int yPos, int nodeId, String ipAddr, int sensorsPort, int nodesPort) {
        this.xPos = xPos;
        this.yPos = yPos;
        this.nodeId = nodeId;
        this.ipAddr = ipAddr;
        this.sensorsPort = sensorsPort;
        this.nodesPort = nodesPort;
    }

    public int getDistance(int xPos, int yPos){
        return Math.abs(xPos - this.xPos) + Math.abs(yPos - this.yPos);
    }

    public  int getDistance(EdgeNodeRepresentation other){
        return getDistance(other.getxPos(), other.getyPos());
    }

    @Override
    public String toString(){
        return "node["+ nodeId + ", pos(" + xPos + "," + yPos + "), " + ipAddr +"]";
    }


    //Methods needed by JAXB
    public EdgeNodeRepresentation(){}

    public int getxPos() {
        return xPos;
    }

    public void setxPos(int xPos) {
        this.xPos = xPos;
    }

    public int getyPos() {
        return yPos;
    }

    public void setyPos(int yPos) {
        this.yPos = yPos;
    }

    public int getNodeId() {
        return nodeId;
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

    @Override
    public boolean equals(Object other){
        if(other instanceof EdgeNodeRepresentation)
            return this.nodeId == ((EdgeNodeRepresentation) other).nodeId;
        else return false;
    }

    public void setNodeId(int nodeId) {
        this.nodeId = nodeId;
    }

    public void setIpAddr(String ipAddr) {
        this.ipAddr = ipAddr;
    }

    public void setSensorsPort(int sensorsPort) {
        this.sensorsPort = sensorsPort;
    }

    public void setNodesPort(int nodesPort) {
        this.nodesPort = nodesPort;
    }

}
