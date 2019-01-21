package EdgeNode.EdgeNetworkMessage;

import ServerCloud.Model.EdgeNodeRepresentation;

public class TreeMessage extends EdgeNetworkMessage{

    public enum TreeMessageType {
        LEAF, INTERNAL, PARENT_DOWN, PARENT_UPDATE
    }

    private TreeMessageType treeMessageType;
    private EdgeNodeRepresentation parent;
    private EdgeNodeRepresentation sender;

    public TreeMessage(TreeMessageType treeMessageType, EdgeNodeRepresentation parent){
        super(MessageType.TREE);
        this.treeMessageType = treeMessageType;
        this.parent = parent;
    }

    public TreeMessage(TreeMessageType treeMessageType, EdgeNodeRepresentation parent, EdgeNodeRepresentation sender){
        super(MessageType.TREE);
        this.treeMessageType = treeMessageType;
        this.parent = parent;
        this.sender = sender;
    }

    public EdgeNodeRepresentation getParent() {
        return parent;
    }

    public void setParent(EdgeNodeRepresentation parent) {
        this.parent = parent;
    }

    public TreeMessageType getTreeMessageType() {
        return treeMessageType;
    }

    public void setTreeMessageType(TreeMessageType treeMessageType) {
        this.treeMessageType = treeMessageType;
    }

    public EdgeNodeRepresentation getSender() {
        return sender;
    }

    public void setSender(EdgeNodeRepresentation sender) {
        this.sender = sender;
    }
}
