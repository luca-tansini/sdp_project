package ServerCloud.Model;

import javax.xml.bind.annotation.XmlRootElement;
import java.lang.Math;

@XmlRootElement
public class EdgeNodeRepresentation {

    private Position position;
    private int nodeId;
    private String ipAddr;
    private int sensorsPort;
    private int nodesPort;

    public EdgeNodeRepresentation(Position position, int nodeId, String ipAddr, int sensorsPort, int nodesPort) {
        this.position = position;
        this.nodeId = nodeId;
        this.ipAddr = ipAddr;
        this.sensorsPort = sensorsPort;
        this.nodesPort = nodesPort;
    }

    public int getDistance(EdgeNodeRepresentation other){
        return position.getDistance(other.getPosition());
    }

    public int getDistance(Position pos){
        return position.getDistance(pos);
    }

    @Override
    public String toString(){
        return "node["+ nodeId + ", "+position+", " + ipAddr +"]";
    }


    //Methods needed by JAXB
    public EdgeNodeRepresentation(){}

    public Position getPosition() {
        return position;
    }

    public void setPosition(Position position) {
        this.position = position;
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
