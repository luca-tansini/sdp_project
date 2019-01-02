package ServerCloud.Model;

public class Model{

    private static Model instance;

    private Grid grid;

    private Model(){}

    public synchronized static Model getInstance(){
        if(instance == null)
            instance = new Model();
        return instance;
    }

    public Grid getGrid(){
        return grid;
    }

}
