package ServerCloud.Model;

import Sensor.Measurement;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.HashMap;

@XmlRootElement
public class StatisticsHistory {

    private ArrayList<Measurement> global;
    private HashMap<String, ArrayList<Measurement>> local;

    public StatisticsHistory(){
        this.global = new ArrayList<>();
        this.local = new HashMap<>();
    }

    public StatisticsHistory(ArrayList<Measurement> global, HashMap<String, ArrayList<Measurement>> local) {
        this.global = global;
        this.local = local;
    }

    public void update(Statistics s){

        this.global.add(0,s.getGlobal());

        for(String i: s.getLocal().keySet()){
            if(this.local.containsKey(i))
                this.local.get(i).add(0,s.getLocal().get(i));
            else {
                ArrayList<Measurement> l = new ArrayList<>();
                l.add(s.getLocal().get(i));
                this.local.put(i,l);
            }
        }
    }

    public StatisticsHistory getStatistics(int n){
        ArrayList<Measurement> l;
        l = this.global;
        ArrayList<Measurement> global = new ArrayList<>();
        global.addAll(l.subList(0, n<=l.size() ? n : l.size()));

        HashMap<String,ArrayList<Measurement>> local = new HashMap<>();
        for(String id: this.local.keySet()) {
            l = this.local.get(id);
            ArrayList<Measurement> k = new ArrayList<>();
            k.addAll(l.subList(0, n<=l.size() ? n : l.size()));
            local.put(id, k);
        }
        return new StatisticsHistory(global, local);
    }

    public ArrayList<Measurement> getGlobalStatistics(int n){
        ArrayList<Measurement> l = this.global;
        ArrayList<Measurement> k = new ArrayList<>();
        k.addAll(l.subList(0, n<=l.size() ? n : l.size()));
        return k;
    }

    public ArrayList<Measurement> getLocalStatistics(String nodeId, int n) {
        ArrayList<Measurement> l = this.local.get(nodeId);
        if (l != null) {
            ArrayList<Measurement> k = new ArrayList<>();
            k.addAll(l.subList(0, n <= l.size() ? n : l.size()));
            return k;
        } else
            throw new IllegalArgumentException();
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
        String out = "\n    Global:\n";
        for(Measurement m: global) {
            out += "        "+m+"\n";
        }
        out += "\n    Local:\n";
        for(String id: local.keySet()){
            out += "        "+id+":\n";
            for(Measurement m: local.get(id))
                out += "            "+m+"\n";
        }
        return out;
    }

}
