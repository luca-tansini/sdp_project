package ServerCloud.Model;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class MeanStdevBean {

    private double mean;
    private double stdev;

    public MeanStdevBean(double mean, double stdev) {
        this.mean = mean;
        this.stdev = stdev;
    }

    public MeanStdevBean(){}

    public double getMean() {
        return mean;
    }

    public void setMean(double mean) {
        this.mean = mean;
    }

    public double getStdev() {
        return stdev;
    }

    public void setStdev(double stdev) {
        this.stdev = stdev;
    }
}
