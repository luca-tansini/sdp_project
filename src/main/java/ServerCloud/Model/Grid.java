package ServerCloud.Model;

public class Grid {

    public static final String INVALID_POS = "positions must be between 0 and grid limits";
    public static final String ID_TAKEN = "ID already taken";
    public static final String NODE_TOO_CLOSE = "node is too close to another node (distance < 20)";
    public static final String NOT_FOUND = "node not found";

    private int XSize;
    private int YSize;
    private NodeList nodes;
    private NodeList leaves;

    public Grid(int XSize, int YSize){
        this.XSize = XSize;
        this.YSize = YSize;
        this.nodes = new NodeList();
    }

    public NodeList getLeaves(){
        return leaves;
    }

    public NodeList getNodes() {
        return nodes;
    }

    public void addNode(EdgeNodeRepresentation node){

        Position pos = node.getPosition();

        if(pos.getX() < 0 || pos.getX() >= this.XSize || pos.getY() <0 || pos.getY() >= this.YSize)
            throw new IllegalArgumentException(INVALID_POS);

        //Controlla insieme che l'id non sia già occupato e che non ci sia un altro nodo vicino
        for(EdgeNodeRepresentation e: this.getNodes()){
            if(e.getNodeId() == node.getNodeId())
                throw new IllegalArgumentException(ID_TAKEN);
            if(e.getPosition().getDistance(pos) < 20)
                throw new IllegalArgumentException(NODE_TOO_CLOSE);
        }
        this.nodes.add(node);
    }

    public void removeNode(int nodeId){
        if(!nodes.contains(nodeId))
            throw new IllegalArgumentException(NOT_FOUND);
        nodes.removeById(nodeId);
        leaves.removeById(nodeId);
    }

    public EdgeNodeRepresentation getSensorTargetNode(Position sensorPos){
        EdgeNodeRepresentation nearest = null;
        int nearestDist = this.XSize + this.YSize;
        for(EdgeNodeRepresentation n: leaves) {
            if(n.getDistance(sensorPos) < nearestDist){
                nearest = n;
                nearestDist = n.getDistance(sensorPos);
            }
        }
        return nearest;
    }

}
