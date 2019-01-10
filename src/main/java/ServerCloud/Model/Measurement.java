package ServerCloud.Model;

import javax.xml.bind.annotation.XmlRootElement;
import java.time.Instant;

@XmlRootElement
public class Measurement {

    double value;
    long timestamp;

    public Measurement(){}

    public Measurement(double value, long timestamp) {
        this.value = value;
        this.timestamp = timestamp;
    }

    public double getValue() {
        return value;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setValue(double value) {
        this.value = value;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString(){
        return "value: "+value + " at: "+ Instant.ofEpochMilli(timestamp);
    }

}
