package Sensor;

import javax.xml.bind.annotation.XmlRootElement;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.TextStyle;
import java.util.Locale;

@XmlRootElement
public class Measurement implements Comparable<Measurement> {

    private String id;
    private String type;
    private double value;
    private long timestamp;

    public Measurement(String id, String type, double value, long timestamp) {
        this.value = value;
        this.timestamp = timestamp;
        this.id=id;
        this.type=type;
    }

    public Measurement(){}

    public double getValue() {
        return value;
    }

    public void setValue(double value) {
        this.value = value;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getId() {
        return id;
    }

    public void setId(String type) {
        this.id = type;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @Override
    public int compareTo(Measurement m) {
        Long thisTimestamp = timestamp;
        Long otherTimestamp = m.getTimestamp();
        return thisTimestamp.compareTo(otherTimestamp);
    }

    @Override
    public String toString(){
        String out = String.format("%.3f ",value);
        LocalDateTime ldt = LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.systemDefault());
        out += ldt.getDayOfMonth() +"-"+ ldt.getMonth().getDisplayName(TextStyle.SHORT, Locale.ITALIAN)+ "-" + ldt.getYear() + " ";
        String s = ldt.toString();
        out += s.substring(s.length()-12);
        return out;
    }
}
