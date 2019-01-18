package EdgeNode.EdgeNetworkMessage;

import ServerCloud.Model.EdgeNodeRepresentation;

public class HelloResponseMessage extends EdgeNetworkMessage {

    private EdgeNodeRepresentation coordinator;
    private long timestamp;
    private EdgeNodeRepresentation parent;

    public HelloResponseMessage() {
    }

    public HelloResponseMessage(EdgeNodeRepresentation coordinator, long timestamp, EdgeNodeRepresentation parent){
        this.setType(MessageType.HELLO_RESPONSE);
        this.coordinator = coordinator;
        this.timestamp = timestamp;
        this.parent = parent;
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

    public EdgeNodeRepresentation getParent() {
        return parent;
    }

    public void setParent(EdgeNodeRepresentation parent) {
        this.parent = parent;
    }
}
