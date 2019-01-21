package EdgeNode.EdgeNetworkMessage;
import ServerCloud.Model.EdgeNodeRepresentation;

public class HelloMessage extends EdgeNetworkMessage {

    private EdgeNodeRepresentation requestingNode;

    public HelloMessage() {
        super(MessageType.HELLO);
    }

    public HelloMessage(EdgeNodeRepresentation requestingNode){
        super(MessageType.HELLO);
        this.requestingNode = requestingNode;
    }

    public void setRequestingNode(EdgeNodeRepresentation requestingNode) {
        this.requestingNode = requestingNode;
    }

    public EdgeNodeRepresentation getRequestingNode() {
        return requestingNode;
    }
}
