package ServerCloud.Model;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class Position {

    private int x;
    private int y;

    public Position(){}

    public Position(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public int getDistance(Position other){
        return Math.abs(this.x - other.x) + Math.abs(this.y - other.y);
    }

    @Override
    public String toString(){
        return "("+x+","+y+")";
    }

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }
}
