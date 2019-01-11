package EdgeNode.EdgeNetworkMessage;

import Sensor.Measurement;
import ServerCloud.Model.EdgeNodeRepresentation;

public class CoordinatorMessage extends EdgeNetworkMessage {

    public enum CoordinatorMessageType{
        STATS_UPDATE, ACK
    }

    private CoordinatorMessageType coordinatorMessageType;
    private EdgeNodeRepresentation sender;
    private Measurement measurement;

    public CoordinatorMessage(){}

    public CoordinatorMessage(CoordinatorMessageType coordinatorMessageType, EdgeNodeRepresentation sender, Measurement measurement) {
        this.setType(MessageType.COORDINATOR);
        this.coordinatorMessageType = coordinatorMessageType;
        this.sender = sender;
        this.measurement = measurement;
    }

    public Measurement getMeasurement() {
        return measurement;
    }

    public void setMeasurement(Measurement measurement) {
        this.measurement = measurement;
    }

    public CoordinatorMessageType getCoordinatorMessageType() {
        return coordinatorMessageType;
    }

    public void setCoordinatorMessageType(CoordinatorMessageType coordinatorMessageType) {
        this.coordinatorMessageType = coordinatorMessageType;
    }

    public EdgeNodeRepresentation getSender() {
        return sender;
    }

    public void setSender(EdgeNodeRepresentation sender) {
        this.sender = sender;
    }
}
