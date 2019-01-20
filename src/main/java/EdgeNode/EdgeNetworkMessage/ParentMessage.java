package EdgeNode.EdgeNetworkMessage;

import Sensor.Measurement;
import ServerCloud.Model.EdgeNodeRepresentation;

import java.util.HashMap;

public class ParentMessage extends EdgeNetworkMessage {

    public enum ParentMessageType {
        STATS_UPDATE, ACK, QUIT
    }

    private ParentMessageType parentMessageType;
    private EdgeNodeRepresentation sender;
    private Measurement measurement;
    private HashMap<String,Measurement> local;

    public ParentMessage(){}

    public ParentMessage(ParentMessageType parentMessageType, EdgeNodeRepresentation sender, Measurement measurement) {
        super.setType(MessageType.PARENT);
        this.parentMessageType = parentMessageType;
        this.sender = sender;
        this.measurement = measurement;
    }

    public ParentMessage(ParentMessageType parentMessageType, EdgeNodeRepresentation sender, Measurement measurement, HashMap<String,Measurement> local) {
        super.setType(MessageType.PARENT);
        this.parentMessageType = parentMessageType;
        this.sender = sender;
        this.measurement = measurement;
        this.local = local;
    }

    public ParentMessageType getParentMessageType() {
        return parentMessageType;
    }

    public void setParentMessageType(ParentMessageType parentMessageType) {
        this.parentMessageType = parentMessageType;
    }

    public EdgeNodeRepresentation getSender() {
        return sender;
    }

    public void setSender(EdgeNodeRepresentation sender) {
        this.sender = sender;
    }

    public Measurement getMeasurement() {
        return measurement;
    }

    public void setMeasurement(Measurement measurement) {
        this.measurement = measurement;
    }

    public HashMap<String, Measurement> getLocal() {
        return local;
    }

    public void setLocal(HashMap<String, Measurement> local) {
        this.local = local;
    }
}
