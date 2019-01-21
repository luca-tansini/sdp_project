package ServerCloud.Model;

import Sensor.Measurement;

import java.time.Instant;
import java.util.HashMap;

public class Statistics {

    public static final int EXPIRY_OFFSET = 15000;

    private Measurement global;                         //Dentro a global c'è la statistica che arriva dal coordinatore
    private HashMap<String, Measurement> local;         //Dentro a local ci sono tutte le statistiche locali dei nodi inferiori

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
        long timestamp = Instant.now().toEpochMilli();
        String out = "Statistics:\n";
        out += "    Global: ";
        if (global != null)
            out +=global+"\n";
        else
            out +="\n";
        out += "    Local:\n";
        for(String id: local.keySet()){
            if(local.get(id).getTimestamp() < (timestamp - EXPIRY_OFFSET))
            out += "        "+id+": "+ local.get(id)+"\n";
        }
        return out;
    }

}
