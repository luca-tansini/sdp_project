package EdgeNode.EdgeNetworkMessage;

import ServerCloud.Model.EdgeNodeRepresentation;

public class WhoisCoordResponseMessage extends EdgeNetworkMessage {

    private EdgeNodeRepresentation coordinator;

    public WhoisCoordResponseMessage() {
    }

    public WhoisCoordResponseMessage(EdgeNodeRepresentation coordinator){
        this.setType(MessageType.WHOIS_COORD_RESPONSE);
        this.coordinator = coordinator;
    }

    public void setCoordinator(EdgeNodeRepresentation coordinator) {
        this.coordinator = coordinator;
    }

    public EdgeNodeRepresentation getCoordinator() {
        return coordinator;
    }
}
