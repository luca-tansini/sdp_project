package ServerCloud.Model;

import Sensor.Measurement;

import java.util.ArrayList;
import java.util.HashMap;

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

    //TODO: le statistiche più nuove andrebbero attaccate davanti, non in fondo
    public void update(Statistics s){

        this.global.add(s.getGlobal());

        for(String i: s.getLocal().keySet()){
            if(this.local.containsKey(i))
                this.local.get(i).add(s.getLocal().get(i));
            else {
                ArrayList<Measurement> l = new ArrayList<>();
                l.add(s.getLocal().get(i));
                this.local.put(i,l);
            }
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
        String out = "StatisticsHistory:\n";
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
