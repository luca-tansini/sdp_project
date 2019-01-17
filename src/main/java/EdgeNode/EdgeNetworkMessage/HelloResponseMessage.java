package EdgeNode.EdgeNetworkMessage;

import ServerCloud.Model.EdgeNodeRepresentation;

public class HelloResponseMessage extends EdgeNetworkMessage {

    private EdgeNodeRepresentation coordinator;
    private long timestamp;

    public HelloResponseMessage() {
    }

    public HelloResponseMessage(EdgeNodeRepresentation coordinator, long timestamp){
        this.setType(MessageType.HELLO_RESPONSE);
        this.coordinator = coordinator;
        this.timestamp = timestamp;
    }

    public void setCoordinator(EdgeNodeRepresentation coordinator) {
        this.coordinator = coordinator;
    }

    public EdgeNodeRepresentation getCoordinator() {
        return coordinator;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
