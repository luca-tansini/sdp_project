package EdgeNode.EdgeNetworkMessage;
import ServerCloud.Model.EdgeNodeRepresentation;

public class WhoisCoordRequestMessage extends EdgeNetworkMessage {

    private EdgeNodeRepresentation requestingNode;

    public WhoisCoordRequestMessage() {
    }

    public WhoisCoordRequestMessage(EdgeNodeRepresentation requestingNode){
        this.setType(MessageType.WHOIS_COORD_REQUEST);
        this.requestingNode = requestingNode;
    }

    public void setRequestingNode(EdgeNodeRepresentation requestingNode) {
        this.requestingNode = requestingNode;
    }

    public EdgeNodeRepresentation getRequestingNode() {
        return requestingNode;
    }
}
