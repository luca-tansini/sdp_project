package EdgeNode;

import ServerCloud.Model.EdgeNodeRepresentation;

import java.util.ArrayList;
import java.util.Iterator;

public class NetworkTree{

    private NetworkTreeNode root;

    public NetworkTree(EdgeNodeRepresentation node){
        this.root = new NetworkTreeNode(node, null);
    }

    public NetworkTreeNode getRoot() {
        return root;
    }

    public ArrayList<EdgeNodeRepresentation> getLeaves(){
        ArrayList<EdgeNodeRepresentation> leaves = new ArrayList();
        getLeaves(root, leaves);
        return leaves;
    }

    //Visita DFS per trovare tutte le foglie
    public void getLeaves(NetworkTreeNode ntn, ArrayList<EdgeNodeRepresentation> leaves){
        if(ntn.isLeaf())
            leaves.add(ntn.getEdgeNode());
        else
            for(NetworkTreeNode child: ntn.getChildren())
                getLeaves(child, leaves);
    }

    /*
     * Aggiunge un nodo all'albero con una visita BFS per mantenerlo meno profondo.
     * Lo aggiunge al primo nodo interno con un buco che trova o promuove una foglia.
     * Restituisce un riferimento al padre. Se il padre ha un figlio solo è appena stato promosso a nodo interno.
     */
    public NetworkTreeNode addNode(EdgeNodeRepresentation node){

        ArrayList<NetworkTreeNode> queue = new ArrayList<>();
        queue.add(root);

        while(!queue.isEmpty()){
            NetworkTreeNode ntn = queue.remove(0);
            //Trova un buco in un nodo interno
            if(ntn.getChildren().size() > 0 && ntn.getChildren().size() < NetworkTreeNode.MAX_CHILDREN) {
                ntn.addChildren(node);
                return ntn;
            }
            //Promuove una foglia
            else if(ntn.getChildren().size() == 0){
                ntn.addChildren(node);
                return ntn;
            }
            else
                queue.addAll(ntn.getChildren());
        }
        //Never get here
        return null;
    }


    //TODO: metodo estremamente duplicato
    /*
     * Aggiunge un nodo all'albero con una visita BFS per mantenerlo meno profondo.
     * Lo aggiunge al primo nodo interno con un buco che trova o promuove una foglia.
     * Restituisce un riferimento al padre. Se il padre ha un figlio solo è appena stato promosso a nodo interno.
     */
    public NetworkTreeNode addNode(NetworkTreeNode node){

        ArrayList<NetworkTreeNode> queue = new ArrayList<>();
        queue.add(root);

        while(!queue.isEmpty()){
            NetworkTreeNode ntn = queue.remove(0);
            //Trova un buco in un nodo interno
            if(ntn.getChildren().size() > 0 && ntn.getChildren().size() < NetworkTreeNode.MAX_CHILDREN) {
                ntn.addChildren(node);
                return ntn;
            }
            //Promuove una foglia
            else if(ntn.getChildren().size() == 0){
                ntn.addChildren(node);
                return ntn;
            }
            else
                queue.addAll(ntn.getChildren());
        }
        //Never get here
        return null;
    }

    /*
     * Rimuove un nodo morto dall'albero. Tutti gli altri figli si aggiusteranno da soli mandando i PARENT_DOWN al coordinatore.
     * Restituisce un riferimento al padre del nodo rimosso. Se il padre non ha più figli deve essere degradato a foglia.
     */
    public NetworkTreeNode removeNode(EdgeNodeRepresentation deadNode){
        NetworkTreeNode deadTreeNode = findNode(root, deadNode);
        //Se non ho trovato il nodo cercato è perchè l'ho già rimosso
        if(deadTreeNode == null)
            return null;
        //Se trovo il nodo cercato lo rimuovo dalla lista del padre e ritorno il padre
        else{
            deadTreeNode.getParent().getChildren().remove(deadTreeNode);
            return deadTreeNode.getParent();
        }
    }

    //Visita DFS per trovare un nodo specifico
    public NetworkTreeNode findNode(NetworkTreeNode ntn, EdgeNodeRepresentation node){
        if(ntn.getEdgeNode().equals(node))
            return ntn;
        for(NetworkTreeNode child: ntn.getChildren()){
            NetworkTreeNode found = findNode(child, node);
            if(found != null) return found;
        }
        return null;
    }

    //Visita DFS per ritornare lista di nodi
    public ArrayList<NetworkTreeNode> toList(){
        ArrayList<NetworkTreeNode> list = new ArrayList<>();
        toList(root,list);
        return list;
    }

    public void toList(NetworkTreeNode ntn, ArrayList<NetworkTreeNode> list){
        list.add(ntn);
        for(NetworkTreeNode child: ntn.getChildren())
            toList(child, list);
    }

}

class NetworkTreeNode{

    public static final int MAX_CHILDREN = 3;

    private EdgeNodeRepresentation edgeNode;
    private NetworkTreeNode parent;
    private ArrayList<NetworkTreeNode> children;

    public NetworkTreeNode(EdgeNodeRepresentation edgeNode, NetworkTreeNode parent){
        this.parent = parent;
        this.edgeNode = edgeNode;
        this.children = new ArrayList<>();
    }

    public void addChildren(EdgeNodeRepresentation edgeNode){
        this.children.add(new NetworkTreeNode(edgeNode, this));
    }

    public void addChildren(NetworkTreeNode ntn){
        this.children.add(ntn);
        ntn.setParent(this);
    }

    public boolean isRoot(){
        return parent == null;
    }

    public boolean isLeaf(){
        return children.size() == 0;
    }

    public EdgeNodeRepresentation getEdgeNode() {
        return edgeNode;
    }

    public void setEdgeNode(EdgeNodeRepresentation edgeNode) {
        this.edgeNode = edgeNode;
    }

    public NetworkTreeNode getParent() {
        return parent;
    }

    public void setParent(NetworkTreeNode parent) {
        this.parent = parent;
    }

    public ArrayList<NetworkTreeNode> getChildren() {
        return children;
    }

    public void setChildren(ArrayList<NetworkTreeNode> children) {
        this.children = children;
    }
}
