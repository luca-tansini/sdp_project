package ServerCloud.Model;

import javax.xml.bind.annotation.XmlRootElement;

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
}
