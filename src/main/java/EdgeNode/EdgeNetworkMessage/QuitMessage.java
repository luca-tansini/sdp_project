package EdgeNode.EdgeNetworkMessage;

public class QuitMessage extends EdgeNetworkMessage {

    public QuitMessage(){
        this.setType(MessageType.QUIT);
    }
}
