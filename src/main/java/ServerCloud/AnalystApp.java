package ServerCloud;

import EdgeNode.EdgeNode;
import Sensor.Measurement;
import ServerCloud.Model.EdgeNodeRepresentation;
import ServerCloud.Model.MeanStdevBean;
import ServerCloud.Model.NodeList;
import ServerCloud.Model.StatisticsHistory;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;

import javax.xml.soap.Node;
import java.util.ArrayList;
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
                "   - nodes:          restituisce lo stato attuale della città (posizione dei vari nodi edge della griglia)\n"+
                "   - global n:       restituisce le ultime n statistiche (con timestamp) globali e locali della città\n" +
                "   - globalmean n:   restituisce media e deviazione standard delle ultime n statistiche globali\n" +
                "   - local id n:     restituisce le ultime n statistiche (con timestamp) prodotte da uno specifico nodo edge\n"+
                "   - localmean id n: restituisce media e deviazione standard delle ultime n statistiche relative ad uno specifico nodo edge\n" +
                "   - help:           mostra questo messaggio di help\n";
        System.out.println(help);
        ClientResponse response;
        Gson gson = new Gson();
        MeanStdevBean msb;

        while (true){

            System.out.print("\n>>> ");
            String in = stdin.nextLine();
            if(in.equals("")) continue;
            String[] cmd = in.split(" ");

            switch(cmd[0]){
                case "nodes":
                    response = client.resource(baseURL+"nodes").type("application/json").get(ClientResponse.class);
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
                    response = client.resource(baseURL+"rawstatistics?n="+cmd[1]).type("application/json").get(ClientResponse.class);
                    if(response.getStatus() != 200) {
                        System.out.println("errore REST: "+response.getEntity(String.class));
                        break;
                    }
                    StatisticsHistory globalStats = gson.fromJson(response.getEntity(String.class), StatisticsHistory.class);
                    System.out.println("\nStatitiche Globali ["+cmd[1]+"]:\n"+globalStats);
                    break;

                case "globalmean":
                    if(cmd.length != 2){
                        System.out.println("\nil comando globalmean richiede un parametro n");
                        break;
                    }
                    response = client.resource(baseURL+"statistics?n="+cmd[1]).type("application/json").get(ClientResponse.class);
                    if(response.getStatus() != 200) {
                        System.out.println("errore REST: "+response.getEntity(String.class));
                        break;
                    }
                    msb = response.getEntity(MeanStdevBean.class);
                    System.out.println("\nStatistiche globali (ultime "+cmd[1]+" misurazioni)\n    media: "+msb.getMean()+" deviazione standard: "+msb.getStdev());
                    break;

                case "local":
                    if(cmd.length != 3){
                        System.out.println("\nil comando local richiede due parametri id n");
                        break;
                    }
                    response = client.resource(baseURL+"rawstatistics/"+cmd[1]+"?n="+cmd[2]).type("application/json").get(ClientResponse.class);
                    if(response.getStatus() != 200) {
                        if(response.getStatus() == 404)
                            System.out.println("il nodo richiesto non è stato trovato!");
                        else
                            System.out.println("errore REST: "+response.getEntity(String.class));
                        break;
                    }
                    ArrayList<Measurement> localStats = gson.fromJson(response.getEntity(String.class), new TypeToken<ArrayList<Measurement>>(){}.getType());
                    System.out.println("\nStatistiche locali nodo"+cmd[1]+" ["+cmd[2]+"]:\n");
                    for(Measurement m: localStats)
                        System.out.println("    "+m);
                    break;

                case "localmean":
                    if(cmd.length != 3){
                        System.out.println("\nil comando localmean richiede due parametri id n");
                        break;
                    }
                    response = client.resource(baseURL+"statistics/"+cmd[1]+"?n="+cmd[2]).type("application/json").get(ClientResponse.class);
                    if(response.getStatus() != 200) {
                        if(response.getStatus() == 404)
                            System.out.println("il nodo richiesto non è stato trovato!");
                        else
                            System.out.println("errore REST: "+response.getEntity(String.class));
                        break;
                    }
                    msb = response.getEntity(MeanStdevBean.class);
                    System.out.println("\nStatistiche locali nodo"+cmd[1]+" (ultime "+cmd[2]+" misurazioni)\n    media: "+msb.getMean()+" deviazione standard: "+msb.getStdev());
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
