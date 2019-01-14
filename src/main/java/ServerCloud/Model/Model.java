package ServerCloud.Model;

public class Model{

    private static Model instance;

    private Grid grid;
    private StatisticsHistory stats;

    private Model(){
        this.grid = new Grid(100,100);
        this.stats = new StatisticsHistory();
    }

    public synchronized static Model getInstance(){
        if(instance == null)
            instance = new Model();
        return instance;
    }

    public Grid getGrid(){
        return grid;
    }

    public StatisticsHistory getStats() {
        return stats;
    }
}
