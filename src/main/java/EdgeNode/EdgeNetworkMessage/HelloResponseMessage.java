package EdgeNode.EdgeNetworkMessage;

import ServerCloud.Model.EdgeNodeRepresentation;

public class HelloResponseMessage extends EdgeNetworkMessage {

    private EdgeNodeRepresentation coordinator;
    private EdgeNodeRepresentation parent;

    public HelloResponseMessage() {
        super(MessageType.HELLO_RESPONSE);
    }

    public HelloResponseMessage(EdgeNodeRepresentation coordinator, long timestamp, EdgeNodeRepresentation parent){
        super(MessageType.HELLO_RESPONSE);
        this.coordinator = coordinator;
        this.parent = parent;
    }

    public void setCoordinator(EdgeNodeRepresentation coordinator) {
        this.coordinator = coordinator;
    }

    public EdgeNodeRepresentation getCoordinator() {
        return coordinator;
    }

    public EdgeNodeRepresentation getParent() {
        return parent;
    }

    public void setParent(EdgeNodeRepresentation parent) {
        this.parent = parent;
    }
}
