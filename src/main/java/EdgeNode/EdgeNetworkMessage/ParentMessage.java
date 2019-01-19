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
    private Measurement localmean;
    private HashMap<String,Measurement> local;

    public ParentMessage(){}

    public ParentMessage(ParentMessageType parentMessageType, EdgeNodeRepresentation sender, Measurement localmean) {
        this.setType(MessageType.PARENT);
        this.parentMessageType = parentMessageType;
        this.sender = sender;
        this.localmean = localmean;
    }

    public ParentMessage(ParentMessageType parentMessageType, EdgeNodeRepresentation sender, Measurement localmean, HashMap<String,Measurement> local) {
        this.setType(MessageType.PARENT);
        this.parentMessageType = parentMessageType;
        this.sender = sender;
        this.localmean = localmean;
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

    public Measurement getLocalmean() {
        return localmean;
    }

    public void setLocalmean(Measurement localmean) {
        this.localmean = localmean;
    }

    public HashMap<String, Measurement> getLocal() {
        return local;
    }

    public void setLocal(HashMap<String, Measurement> local) {
        this.local = local;
    }
}
