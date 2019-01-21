package EdgeNode.EdgeNetworkMessage;

import java.time.Instant;

public class EdgeNetworkMessage {

    public enum MessageType{
        HELLO, HELLO_RESPONSE, ELECTION, PARENT, TREE, QUIT
    }

    private MessageType type;
    private long timestamp;

    public EdgeNetworkMessage(MessageType type){
        this.timestamp = Instant.now().toEpochMilli();
        this.type = type;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public void setType(MessageType type) {
        this.type = type;
    }

    public MessageType getType(){
        return this.type;
    }

}
