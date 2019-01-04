package EdgeNode.EdgeNetworkMessage;

public class EdgeNetworkMessage {

    public enum MessageType{
        WHOIS_COORD_REQUEST, WHOIS_COORD_RESPONSE
    }

    private MessageType type;

    public EdgeNetworkMessage(){}

    public void setType(MessageType type) {
        this.type = type;
    }

    public MessageType getType(){
        return this.type;
    }

}
