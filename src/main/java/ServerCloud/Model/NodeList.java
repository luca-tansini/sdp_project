package ServerCloud.Model;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;

@XmlRootElement
public class NodeList {

    private ArrayList<EdgeNodeRepresentation> nodes;

    public NodeList(){
        this.nodes = new ArrayList<>();
    }

    public ArrayList<EdgeNodeRepresentation> getNodes() {
        return nodes;
    }

    public void setNodes(ArrayList<EdgeNodeRepresentation> nodes) {
        this.nodes = nodes;
    }
}
