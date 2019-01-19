package EdgeNode.EdgeNetworkMessage;

public class EdgeNetworkMessage {

    public enum MessageType{
        HELLO, HELLO_RESPONSE, ELECTION, PARENT, TREE, QUIT
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
