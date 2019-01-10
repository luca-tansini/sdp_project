package EdgeNode.EdgeNetworkMessage;
import ServerCloud.Model.EdgeNodeRepresentation;

public class HelloMessage extends EdgeNetworkMessage {

    private EdgeNodeRepresentation requestingNode;

    public HelloMessage() {
    }

    public HelloMessage(EdgeNodeRepresentation requestingNode){
        this.setType(MessageType.HELLO);
        this.requestingNode = requestingNode;
    }

    public void setRequestingNode(EdgeNodeRepresentation requestingNode) {
        this.requestingNode = requestingNode;
    }

    public EdgeNodeRepresentation getRequestingNode() {
        return requestingNode;
    }
}
