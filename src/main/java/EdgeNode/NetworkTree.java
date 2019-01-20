package EdgeNode;

import ServerCloud.Model.EdgeNodeRepresentation;
import sun.nio.ch.Net;

import java.util.ArrayList;
import java.util.Iterator;

public class NetworkTree{

    private NetworkTreeNode root;
    private ArrayList<NetworkTreeNode> orphans;

    public NetworkTree(EdgeNodeRepresentation node){
        this.root = new NetworkTreeNode(node, null);
        this.orphans = new ArrayList<>();
    }

    public NetworkTreeNode getRoot() {
        return root;
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
            //Trova un buco in un nodo interno o promuove una foglia
            if(ntn.getChildren().size() < NetworkTreeNode.MAX_CHILDREN) {
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
        // Se non ho trovato il nodo cercato è perchè l'ho già rimosso
        if(deadTreeNode == null)
            return null;
        // Se trovo il nodo cercato lo rimuovo dalla lista del padre
        // Aggiungo gli eventuali figli agli orfani (togliendogli il riferimento al padre)
        // E infine ritorno il padre
        deadTreeNode.getParent().getChildren().remove(deadTreeNode);
        for(NetworkTreeNode newOrphan: deadTreeNode.getChildren()){
            newOrphan.setParent(null);
            orphans.add(newOrphan);
        }
        return deadTreeNode.getParent();
    }

    public NetworkTreeNode findNode(EdgeNodeRepresentation node){
        // Quando cerca un nodo per prima cosa lo cerca negli orfani e se lo trova lo rimuove
        for(int i=0; i<orphans.size(); i++) {
            // Cerca negli orfani con DFS (gli orfani sono alberi a tutti gli effetti)
            NetworkTreeNode found = findNode(orphans.get(i),node);
            if(found != null){
                // Se il padre è nullo  ho un orfano di primo livello
                // quindi deve toglierlo dagli orfani (altrimenti ci pensa il chiamante)
                if(found.getParent() == null)
                    orphans.remove(i);
                return found;
            }
        }
        // Se non lo trova negli orfani visita l'albero
        return findNode(root, node);
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

    public void printTree(){
        System.out.println("Tree:");
        printTree(root, 1);
        System.out.println("\nOrphans:");
        for (NetworkTreeNode ntn: orphans)
            printTree(ntn, 1);
    }

    public void printTree(NetworkTreeNode ntn, int lvl){
        for(int i=0; i<lvl; i++)
            System.out.print("\t");
        System.out.println(ntn);
        for(NetworkTreeNode child: ntn.getChildren())
            printTree(child, lvl+1);
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
        System.out.println("DEBUG - Trying to add NTN"+ntn.edgeNode.getNodeId()+" as children to NTN"+this.edgeNode.getNodeId());
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

    @Override
    public String toString(){
        String out =  "NTN-[EdgeNode: "+edgeNode.getNodeId()+" parent: ";
        out += parent == null ? null : parent.edgeNode.getNodeId();
        out += " children:(";
        for(NetworkTreeNode child: children)
            out += child.edgeNode.getNodeId() + ",";
        out+=")]";
        return out;
    }

}
