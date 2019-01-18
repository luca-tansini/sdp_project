package ServerCloud.Services;

import ServerCloud.Model.EdgeNodeRepresentation;
import ServerCloud.Model.Model;
import ServerCloud.Model.Statistics;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.util.ArrayList;

@Path("edgenetwork")
public class EdgeNetworkServices {

    Gson gson = new Gson();

    @Path("nodes")
    @GET
    @Produces({"application/json"})
    public Response getNodes(){
        String json = gson.toJson(Model.getInstance().getGrid().getNodes());
        return Response.ok(json).build();
    }

    @Path("nodes")
    @POST
    @Consumes({"application/json"})
    @Produces({"application/json"})
    public Response addNode(String nodeJson){
        EdgeNodeRepresentation node = gson.fromJson(nodeJson, EdgeNodeRepresentation.class);
        try{
            Model.getInstance().getGrid().addNode(node);
        } catch (IllegalArgumentException e){
            return  Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build();
        }
        System.out.println("ADD: "+node);
        return Response.ok(gson.toJson(Model.getInstance().getGrid().getNodes())).build();
    }

    @Path("nodes/{id}")
    @DELETE
    public Response removeNode(@PathParam("id") int nodeId){
        try{
            Model.getInstance().getGrid().removeNode(nodeId);
        } catch (IllegalArgumentException e){
            return Response.status(Response.Status.NOT_FOUND).entity(e.getMessage()).build();
        }
        System.out.println("REMOVE: "+nodeId);
        return Response.ok().build();
    }

    @Path("leaves")
    @POST
    @Consumes({"application/json"})
    public Response setLeaves(String leavesJson){
        ArrayList<EdgeNodeRepresentation> newLeaves = gson.fromJson(leavesJson, new TypeToken<ArrayList<EdgeNodeRepresentation>>(){}.getType());
        Model.getInstance().getGrid().getLeaves().setNodes(newLeaves);
        return Response.ok().build();
    }

    @Path("leaves/{id}")
    @POST
    public Response addLeaf(@PathParam("id") int nodeId){
        EdgeNodeRepresentation node = Model.getInstance().getGrid().getNodes().getById(nodeId);
        if(node != null) {
            System.out.println("ADD LEAF: "+nodeId);
            Model.getInstance().getGrid().getLeaves().add(node);
            return Response.ok().build();
        }
        else return Response.status(Response.Status.NOT_FOUND).build();
    }

    @Path("leaves/{id}")
    @DELETE
    public Response removeLeaf(@PathParam("id") int nodeId){
        EdgeNodeRepresentation node = Model.getInstance().getGrid().getLeaves().getById(nodeId);
        if(node != null) {
            Model.getInstance().getGrid().getLeaves().removeById(nodeId);
            System.out.println("REMOVE LEAF: "+nodeId);
            return Response.ok().build();
        }
        return Response.status(Response.Status.NOT_FOUND).build();
    }

    @Path("statistics")
    @POST
    @Consumes({"application/json"})
    public Response updateStatistics(String statsJson){
        Statistics stats = gson.fromJson(statsJson, Statistics.class);
        Model.getInstance().getStats().update(stats);
        return Response.ok().build();
    }
}
