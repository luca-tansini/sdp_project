package ServerCloud;

import EdgeNode.EdgeNode;
import ServerCloud.Model.EdgeNodeRepresentation;
import ServerCloud.Model.NodeList;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;

import javax.xml.soap.Node;
import java.util.Scanner;

public class AnalystApp {

    public static void main(String[] args){

        if(args.length != 2){
            System.out.println("usage: AnalystApp <serverAddr> <port>");
            return;
        }

        String serverAddr = args[0];
        String port = args[1];
        String baseURL = "http://"+serverAddr+":"+port+"/analyst/";

        Client client = Client.create();

        Scanner stdin = new Scanner(System.in);

        System.out.println("\nBENVENUTO in ANALYSTAPP\n");
        String help = "\nelenco comandi (usa il comando help per visualizzarlo di nuovo):\n" +
                "   - nodes:    restituisce lo stato attuale della città (posizione dei vari nodi edge della griglia)\n"+
                "   - global n: restituisce le ultime n statistiche (con timestamp) globali e locali della città\n" +
                "   - local n:  restituisce le ultime n statistiche (con timestamp) prodotte da uno specifico nodo edge\n"+
                "   - help:     mostra questo messaggio di help\n";
        System.out.println(help);

        while (true){

            System.out.print("\n>>> ");
            String in = stdin.nextLine();
            if(in.equals("")) continue;
            String[] cmd = in.split(" ");

            switch(cmd[0]){
                case "nodes":
                    ClientResponse response = client.resource(baseURL+"nodes").type("application/json").get(ClientResponse.class);
                    if(response.getStatus() == 200){
                        System.out.println("\nElenco dei nodi attivi:");
                        NodeList l = response.getEntity(NodeList.class);
                        for (EdgeNodeRepresentation e: l.getNodes())
                            System.out.println("    "+e);
                    }
                    else
                        System.out.println(response);
                    break;

                case "global":
                    if(cmd.length != 2){
                        System.out.println("\nil comando global richiede un parametro n");
                        break;
                    }
                    System.out.println("\nglobal n="+cmd[1]);
                    break;

                case "local":
                    if(cmd.length != 2){
                        System.out.println("\nil comando local richiede un parametro n");
                        break;
                    }
                    System.out.println("\nlocal n="+cmd[1]);
                    break;

                case "help":
                    System.out.println(help);
                    break;


                default:
                    System.out.println("\ncomando "+in+" non riconosciuto");
                    break;
            }
        }
    }
}
