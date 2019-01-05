package EdgeNode.EdgeNetworkMessage;

import ServerCloud.Model.EdgeNodeRepresentation;

public class CoordinatorMessage extends EdgeNetworkMessage {

    public enum CoordinatorMessageType{
        STATS_UPDATE, ACK
    }

    private CoordinatorMessageType coordinatorMessageType;
    private EdgeNodeRepresentation sender;
    //TODO: to be removed
    private String msg;

    public CoordinatorMessage(){}

    public CoordinatorMessage(CoordinatorMessageType coordinatorMessageType, EdgeNodeRepresentation sender, String msg) {
        this.setType(MessageType.COORDINATOR);
        this.coordinatorMessageType = coordinatorMessageType;
        this.sender = sender;
        this.msg = msg;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
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
