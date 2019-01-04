package ServerCloud.Model;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.HashMap;

@XmlRootElement
public class Statistics {

    private ArrayList<Measurement> global;
    private HashMap<Integer, ArrayList<Measurement>> local;

    public Statistics(){
        this.global = new ArrayList<>();
        this.local = new HashMap<>();
    }

    public Statistics(ArrayList<Measurement> global, HashMap<Integer, ArrayList<Measurement>> local) {
        this.global = global;
        this.local = local;
    }

    public void update(Statistics s){
        //TODO: è corretto assumere che i timestamp arrivino sempre in ordine? Dovrebbero arrivarmi sempre da una sola sorgente no?

        this.global.addAll(s.global);

        for(Integer i: s.local.keySet()){
            if(this.local.containsKey(i))
                this.local.get(i).addAll(s.local.get(i));
            else
                this.local.put(i, s.getLocal().get(i));
        }
    }

    public void setGlobal(ArrayList<Measurement> global) {
        this.global = global;
    }

    public void setLocal(HashMap<Integer, ArrayList<Measurement>> local) {
        this.local = local;
    }

    public ArrayList<Measurement> getGlobal() {
        return global;
    }

    public HashMap<Integer, ArrayList<Measurement>> getLocal() {
        return local;
    }
}
