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
