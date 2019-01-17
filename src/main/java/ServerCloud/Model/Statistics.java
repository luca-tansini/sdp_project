package ServerCloud.Model;

import Sensor.Measurement;
import java.util.HashMap;

public class Statistics {

    private Measurement global;
    private HashMap<String, Measurement> local;

    public Statistics(){
        this.local = new HashMap<>();
    }

    public Measurement getGlobal() {
        return global;
    }

    public void setGlobal(Measurement global) {
        this.global = global;
    }

    public HashMap<String, Measurement> getLocal() {
        return local;
    }

    public void setLocal(HashMap<String, Measurement> local) {
        this.local = local;
    }

    @Override
    public String toString(){
        String out = "Statistics:\n";
        out += "    Global: ";
        if (global != null)
            out +=global+"\n";
        else
            out +="\n";
        out += "    Local:\n";
        for(String id: local.keySet()){
            out += "        "+id+": "+local.get(id)+"\n";
        }
        return out;
    }

}
