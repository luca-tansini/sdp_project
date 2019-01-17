package EdgeNode.EdgeNetworkMessage;

import ServerCloud.Model.EdgeNodeRepresentation;

public class HelloResponseMessage extends EdgeNetworkMessage {

    private EdgeNodeRepresentation coordinator;

    public HelloResponseMessage() {
    }

    public HelloResponseMessage(EdgeNodeRepresentation coordinator){
        this.setType(MessageType.HELLO_RESPONSE);
        this.coordinator = coordinator;
    }

    public void setCoordinator(EdgeNodeRepresentation coordinator) {
        this.coordinator = coordinator;
    }

    public EdgeNodeRepresentation getCoordinator() {
        return coordinator;
    }
}
