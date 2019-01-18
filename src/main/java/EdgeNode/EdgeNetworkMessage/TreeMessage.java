package EdgeNode.EdgeNetworkMessage;

import ServerCloud.Model.EdgeNodeRepresentation;

public class TreeMessage extends EdgeNetworkMessage{

    public enum TreeNodeType{
        LEAF,INTERNAL
    }

    private TreeNodeType treeNodeType;
    private EdgeNodeRepresentation parent;

    public TreeMessage(TreeNodeType nodeType, EdgeNodeRepresentation parent){
        this.setType(MessageType.TREE);
        this.treeNodeType = nodeType;
        this.parent = parent;
    }

    public EdgeNodeRepresentation getParent() {
        return parent;
    }

    public void setParent(EdgeNodeRepresentation parent) {
        this.parent = parent;
    }

    public TreeNodeType getTreeNodeType() {
        return treeNodeType;
    }

    public void setTreeNodeType(TreeNodeType treeNodeType) {
        this.treeNodeType = treeNodeType;
    }
}
