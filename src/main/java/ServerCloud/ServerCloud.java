package ServerCloud;

import com.sun.jersey.api.container.httpserver.HttpServerFactory;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;

public class ServerCloud {

    public static void main(String[] args) throws IOException {

        if(args.length != 2){
            System.out.println("usage: ServerCloud <host> <port>");
            return;
        }

        /*//DEBUG
        ArrayList<Measurement> fakeGlobal = new ArrayList<>();
        fakeGlobal.add(new Measurement(106.7, 1000456));
        fakeGlobal.add(new Measurement(99.8, 1000479));

        HashMap<Integer,ArrayList<Measurement>> fakeLocal = new HashMap<>();

        ArrayList<Measurement> fakeLocalList = new ArrayList<>();
        fakeLocalList.add(new Measurement(47.5, 1000456));
        fakeLocal.put(42,fakeLocalList);

        fakeLocalList = new ArrayList<>();
        fakeLocalList.add(new Measurement(107.5, 1000446));
        fakeLocalList.add(new Measurement(47.5, 1000466));
        fakeLocal.put(43,fakeLocalList);

        StateModel.getInstance().getStats().update(new Statistics(fakeGlobal,fakeLocal));
        //END DEBUG*/

        String HOST = args[0];
        int PORT = Integer.parseInt(args[1]);

        HttpServer server = HttpServerFactory.create("http://"+HOST+":"+PORT+"/");
        server.start();

        System.out.println("Server running!");
        System.out.println("Server started on: http://"+HOST+":"+PORT);

        System.out.println("Hit return to stop...");
        System.in.read();
        System.out.println("Stopping server");
        server.stop(0);
        System.out.println("Server stopped");
    }
}
