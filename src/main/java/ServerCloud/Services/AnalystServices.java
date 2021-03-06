package ServerCloud.Services;

import Sensor.Measurement;
import ServerCloud.Model.MeanStdevBean;
import ServerCloud.Model.Model;
import ServerCloud.Model.StatisticsHistory;
import com.google.gson.Gson;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.util.ArrayList;

@Path("analyst")
public class AnalystServices {

    Gson gson = new Gson();

    @Path("nodes")
    @GET
    @Produces({"application/json"})
    public Response getNodes(){
        return Response.ok(gson.toJson(Model.getInstance().getGrid().getNodes())).build();
    }

    @Path("rawstatistics")
    @GET
    @Produces({"application/json"})
    public Response getRawStatistics(@QueryParam("n") int n){
        if(n < 0)
            return Response.status(Response.Status.BAD_REQUEST).entity("n must be positive").build();
        StatisticsHistory stats = Model.getInstance().getStats().getStatistics(n);
        return Response.ok(gson.toJson(stats)).build();
    }

    @Path("statistics")
    @GET
    @Produces({"application/json"})
    public Response getStatistics(@QueryParam("n") int n){
        if(n < 0)
            return Response.status(Response.Status.BAD_REQUEST).entity("n must be positive").build();
        ArrayList<Measurement> stats = Model.getInstance().getStats().getGlobalStatistics(n);

        double mean = mean(stats);
        return Response.ok(gson.toJson(new MeanStdevBean(mean, stdev(stats, mean)))).build();
    }

    @Path("rawstatistics/{id}")
    @GET
    @Produces({"application/json"})
    public Response getRawStatistics(@PathParam("id") String id, @QueryParam("n") int n) {
        if (n < 0)
            return Response.status(Response.Status.BAD_REQUEST).entity("n must be positive").build();
        try {
            return Response.ok(gson.toJson(Model.getInstance().getStats().getLocalStatistics(id, n))).build();
        } catch (IllegalArgumentException e){
            return Response.status(Response.Status.NOT_FOUND).build();
        }
    }

    @Path("statistics/{id}")
    @GET
    @Produces({"application/json"})
    public Response getStatistics(@PathParam("id") String id, @QueryParam("n") int n){
        if(n < 0)
            return Response.status(Response.Status.BAD_REQUEST).entity("n must be positive").build();
        try {
            ArrayList<Measurement> stats = Model.getInstance().getStats().getLocalStatistics(id, n);
            double mean = mean(stats);
            return Response.ok(gson.toJson(new MeanStdevBean(mean, stdev(stats, mean)))).build();
        } catch (IllegalArgumentException e){
            return Response.status(Response.Status.NOT_FOUND).build();
        }
    }

    private double mean(ArrayList<Measurement> values){
        if(values.size() == 0)
            return 0;
        double mean = 0;
        for(Measurement m: values)
            mean += m.getValue();
        mean /= values.size();
        return mean;
    }

    private double stdev(ArrayList<Measurement> values, double mean){
        if(values.size() <= 1)
            return 0;
        double stdev = 0;
        for(Measurement m: values)
            stdev += (m.getValue() - mean) * (m.getValue() - mean);
        stdev /= (values.size()-1);
        return Math.sqrt(stdev);
    }

}
