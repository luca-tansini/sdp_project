package EdgeNode;

import ServerCloud.Model.EdgeNodeRepresentation;
import ServerCloud.Model.NodeList;

import java.util.ArrayList;

public class SafetyNodeList extends NodeList {

    private ArrayList<EdgeNodeRepresentation> safetyList;

    public SafetyNodeList(){
        super();
        this.safetyList = new ArrayList<>();
    }

    public SafetyNodeList(NodeList l){
        super.setNodes(l.getNodes());
        this.safetyList = new ArrayList<>();
    }

    @Override
    public synchronized void remove(EdgeNodeRepresentation node){
        if(this.safetyList.contains(node)){
            this.safetyList.remove(node);
        }
        else
            super.remove(node);
    }

    public synchronized void addSafety(EdgeNodeRepresentation node){
        this.safetyList.add(node);
    }

}
