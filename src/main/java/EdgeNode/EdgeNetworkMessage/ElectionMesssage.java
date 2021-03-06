package EdgeNode.EdgeNetworkMessage;

import ServerCloud.Model.EdgeNodeRepresentation;

import java.time.Instant;

public class ElectionMesssage extends EdgeNetworkMessage {

    public enum ElectionMessageType{
        ELECTION, ALIVE_ACK, VICTORY
    }

    private ElectionMessageType electionMessageType;
    private EdgeNodeRepresentation sender;

    public ElectionMesssage() {
        super(MessageType.ELECTION);
    }

    public ElectionMesssage(ElectionMessageType type, EdgeNodeRepresentation sender) {
        super(MessageType.ELECTION);
        this.electionMessageType = type;
        this.sender = sender;
    }

    public ElectionMessageType getElectionMessageType() {
        return electionMessageType;
    }

    public void setElectionMessageType(ElectionMessageType type) {
        this.electionMessageType = type;
    }

    public EdgeNodeRepresentation getSender() {
        return sender;
    }

    public void setSender(EdgeNodeRepresentation sender) {
        this.sender = sender;
    }
}
