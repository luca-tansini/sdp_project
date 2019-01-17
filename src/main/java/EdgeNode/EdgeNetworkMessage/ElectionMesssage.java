package EdgeNode.EdgeNetworkMessage;

import ServerCloud.Model.EdgeNodeRepresentation;

import java.time.Instant;

public class ElectionMesssage extends EdgeNetworkMessage {

    public enum ElectionMessageType{
        ELECTION, ALIVE_ACK, VICTORY
    }

    private ElectionMessageType electionMessageType;
    private EdgeNodeRepresentation sender;
    private long timestamp;

    public ElectionMesssage() {
    }

    public ElectionMesssage(ElectionMessageType type, EdgeNodeRepresentation sender) {
        this.setType(MessageType.ELECTION);
        this.electionMessageType = type;
        this.sender = sender;
        this.timestamp = Instant.now().toEpochMilli();
    }

    public ElectionMessageType getElectionMessageType() {
        return electionMessageType;
    }

    public void setElectionMessageType(ElectionMessageType type) {
        this.electionMessageType = type;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public EdgeNodeRepresentation getSender() {
        return sender;
    }

    public void setSender(EdgeNodeRepresentation sender) {
        this.sender = sender;
    }
}
