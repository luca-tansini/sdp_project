package ServerCloud.Model;

import Sensor.Measurement;
import io.grpc.stub.StreamObserver;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.HashMap;

@XmlRootElement
public class Statistics {

    private ArrayList<Measurement> global;
    private HashMap<String, ArrayList<Measurement>> local;

    public Statistics(){
        this.global = new ArrayList<>();
        this.local = new HashMap<>();
    }

    public Statistics(ArrayList<Measurement> global, HashMap<String, ArrayList<Measurement>> local) {
        this.global = global;
        this.local = local;
    }

    public void update(Statistics s){
        //TODO: è corretto assumere che i timestamp arrivino sempre in ordine? Dovrebbero arrivarmi sempre da una sola sorgente no?

        this.global.addAll(s.global);

        for(String i: s.local.keySet()){
            if(this.local.containsKey(i))
                this.local.get(i).addAll(s.local.get(i));
            else
                this.local.put(i, s.getLocal().get(i));
        }
    }

    public void setGlobal(ArrayList<Measurement> global) {
        this.global = global;
    }

    public void setLocal(HashMap<String, ArrayList<Measurement>> local) {
        this.local = local;
    }

    public ArrayList<Measurement> getGlobal() {
        return global;
    }

    public HashMap<String, ArrayList<Measurement>> getLocal() {
        return local;
    }

    @Override
    public String toString(){
        String out = "Statistics:\n";
        out += "    Global:\n       [";
        for(int i=0; i<global.size(); i++) {
            out += global.get(i);
            if(i < global.size()-1)
                out += ", ";
        }
        out += "]\n";
        out += "    Local:\n";
        for(String id: local.keySet()){
            out += "        "+id+": [";
            ArrayList l = local.get(id);
            for(int i=0; i<l.size(); i++) {
                out += l.get(i);
                if(i < l.size()-1)
                    out += ", ";
            }
            out+="]\n";
        }
        return out;
    }

}
